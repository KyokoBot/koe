package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import moe.kyokobot.koe.internal.util.RTPHeaderWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Supplier;

import static moe.kyokobot.koe.TestUtils.*;
import static moe.kyokobot.koe.internal.crypto.ReferenceCrypto.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks every encryption mode against {@link ReferenceCrypto}, so that the packets stay decodable by Discord
 * regardless of how the modes themselves are implemented.
 */
class EncryptionModeOracleTest {
    private static final int MAX_OPUS_FRAME = 1276;
    private static final int SSRC = 0xDEADBEEF;
    private static final int[] PAYLOAD_SIZES = {MAX_OPUS_FRAME, 160, 3, 1};

    // Uneven sizes so state left over from a bigger frame would leak into the following smaller ones.
    private static final int[] VECTOR_FRAME_SIZES = {200, 3, 1, 57, 200, 16, 2, 120};
    private static final String VECTORS_RESOURCE = "encryption-mode-vectors.txt";

    // Counters start right below 2^32 to cover wraparound.
    private static final Map<String, Supplier<EncryptionMode>> DETERMINISTIC_MODES = Map.of(
            "aead_aes256_gcm_rtpsize", () -> new AEADAES256GCMRTPSizeEncryptionMode(0xFFFFFFFD),
            "aead_aes256_gcm", () -> new AEADAES256GCMEncryptionMode(0xFFFFFFFD),
            "aead_xchacha20_poly1305_rtpsize", () -> new AEADXChaCha20Poly1305RTPSizeEncryptionMode(0xFFFFFFFD),
            "xsalsa20_poly1305", XSalsa20Poly1305EncryptionMode::new,
            "xsalsa20_poly1305_lite", () -> new XSalsa20Poly1305LiteEncryptionMode(0xFFFFFFFD),
            "xsalsa20_poly1305_lite_rtpsize", () -> new XSalsa20Poly1305LiteRTPSizeEncryptionMode(0xFFFFFFFD),
            "xsalsa20_poly1305_suffix", () -> new XSalsa20Poly1305SuffixEncryptionMode(new Random(0x6b6f65)::nextBytes),
            "plain", PlainEncryptionMode::new
    );

    private final byte[] key = new byte[32];
    private final Random random = new Random(0x6b6f65);

    {
        random.nextBytes(key);
    }

    static Set<String> modes() {
        return new TreeSet<>(RECEIVERS.keySet());
    }

    static Set<String> encryptedModes() {
        var modes = modes();
        modes.remove("plain");
        return modes;
    }

    @Test
    void everyRegisteredModeIsCovered() {
        assertEquals(RECEIVERS.keySet(), DefaultEncryptionModes.encryptionModes.keySet());
        assertEquals(RECEIVERS.keySet(), DETERMINISTIC_MODES.keySet());
    }

    @ParameterizedTest
    @MethodSource("modes")
    void subsequentFramesMatchKnownVectors(String name) throws Exception {
        var expected = loadVectors().get(name);
        assertNotNull(expected, "no vectors for " + name);
        assertEquals(VECTOR_FRAME_SIZES.length, expected.size());

        var mode = DETERMINISTIC_MODES.get(name).get();
        var receiver = RECEIVERS.get(name);
        for (int i = 0; i < VECTOR_FRAME_SIZES.length; i++) {
            // Guards the vectors themselves, they must be decodable by the reference implementation.
            assertArrayEquals(vectorPayload(i), receiver.open(hex(expected.get(i)), vectorKey()), "reference rejects vector " + i);

            assertEquals(expected.get(i), toHex(sealVectorFrame(mode, i)), "frame " + i);
        }
    }

    @ParameterizedTest
    @MethodSource("modes")
    void reportsItsName(String name) {
        assertEquals(name, EncryptionMode.get(name).getName());
    }

    @ParameterizedTest
    @MethodSource("modes")
    void packetsDecryptWithReferenceImplementation(String name) throws Exception {
        var mode = EncryptionMode.get(name);
        var receiver = RECEIVERS.get(name);

        // Large frames first, so leftovers in reused internal buffers would show up in the smaller ones.
        int seq = 0;
        for (int size : PAYLOAD_SIZES) {
            var payload = randomBytes(size);
            var packet = seal(mode, seq, payload);

            assertArrayEquals(header(seq), Arrays.copyOf(packet, HEADER_LENGTH), "RTP header must stay intact");
            int expectedLength = HEADER_LENGTH + size + (name.equals("plain") ? 0 : TAG_LENGTH) + receiver.nonceSuffix;
            assertEquals(expectedLength, packet.length, "packet length for " + size + " byte payload");
            assertArrayEquals(payload, receiver.open(packet, key), "decrypted " + size + " byte payload");
            seq++;
        }
    }

    @ParameterizedTest
    @MethodSource("encryptedModes")
    void payloadIsNotSentInPlaintext(String name) {
        var payload = new byte[MAX_OPUS_FRAME];
        Arrays.fill(payload, (byte) 0x55);
        var packet = seal(EncryptionMode.get(name), 0, payload);

        var run = new byte[32];
        Arrays.fill(run, (byte) 0x55);
        assertEquals(-1, indexOf(packet, run));
    }

    @ParameterizedTest
    @MethodSource("encryptedModes")
    void noncesAreNeverReused(String name) {
        var mode = EncryptionMode.get(name);
        var receiver = RECEIVERS.get(name);
        var seen = new HashSet<String>();
        var payload = randomBytes(160);

        for (int i = 0; i < 2048; i++) {
            // xsalsa20_poly1305 derives the nonce from the RTP header, so the sequence/timestamp must differ.
            var packet = seal(mode, i, payload);
            var nonce = name.equals("xsalsa20_poly1305")
                    ? Arrays.copyOf(packet, HEADER_LENGTH)
                    : Arrays.copyOfRange(packet, packet.length - receiver.nonceSuffix, packet.length);
            assertTrue(seen.add(toHex(nonce)), "nonce reused at packet " + i);
        }
    }

    // The _lite and _suffix modes don't bind the RTP header to the payload.
    @ParameterizedTest
    @ValueSource(strings = {"aead_aes256_gcm_rtpsize", "aead_aes256_gcm", "aead_xchacha20_poly1305_rtpsize", "xsalsa20_poly1305"})
    void tamperedHeaderIsRejected(String name) {
        var packet = seal(EncryptionMode.get(name), 7, randomBytes(160));
        packet[2] ^= 0x01; // sequence number

        assertThrows(GeneralSecurityException.class, () -> RECEIVERS.get(name).open(packet, key));
    }

    @ParameterizedTest
    @MethodSource("encryptedModes")
    void tamperedPayloadIsRejected(String name) {
        var packet = seal(EncryptionMode.get(name), 7, randomBytes(160));
        packet[HEADER_LENGTH + 20] ^= 0x01;

        assertThrows(GeneralSecurityException.class, () -> RECEIVERS.get(name).open(packet, key));
    }

    @ParameterizedTest
    @MethodSource("encryptedModes")
    void wrongKeyIsRejected(String name) {
        var packet = seal(EncryptionMode.get(name), 7, randomBytes(160));
        var otherKey = key.clone();
        otherKey[0] ^= 0x01;

        assertThrows(GeneralSecurityException.class, () -> RECEIVERS.get(name).open(packet, otherKey));
    }

    @ParameterizedTest
    @MethodSource("encryptedModes")
    void validateKeyAcceptsOnly32ByteKeys(String name) {
        var mode = EncryptionMode.get(name);

        assertDoesNotThrow(() -> mode.validateKey(new byte[32]));
        for (int length : new int[]{0, 16, 24, 31, 33, 64}) {
            assertThrows(IllegalArgumentException.class, () -> mode.validateKey(new byte[length]), length + " byte key");
        }
        assertThrows(IllegalArgumentException.class, () -> mode.validateKey(null));
    }

    @Test
    void plainAcceptsAnyKey() {
        assertDoesNotThrow(() -> EncryptionMode.get("plain").validateKey(new byte[0]));
    }

    // DiscordUDPConnection#createPacket drops the packet both when box returns false and when it throws.
    @ParameterizedTest
    @MethodSource("encryptedModes")
    void invalidKeyFailsWithoutWritingPayload(String name) {
        var payload = randomBytes(160);
        ByteBuf plain = Unpooled.directBuffer(payload.length).writeBytes(payload);
        ByteBuf output = Unpooled.directBuffer();
        try {
            RTPHeaderWriter.writeV2(output, (byte) 120, (char) 1, 960, SSRC, false);
            boolean boxed;
            try {
                boxed = EncryptionMode.get(name).box(plain, payload.length, output, new byte[31]);
            } catch (RuntimeException e) {
                boxed = false;
            }

            assertFalse(boxed);
            assertEquals(HEADER_LENGTH, output.readableBytes(), "failed box wrote to the packet");
        } finally {
            plain.release();
            output.release();
        }
    }

    // AES-GCM goes through JCE, which allocates internally, and plain is only used for testing.
    @ParameterizedTest
    @ValueSource(strings = {"aead_xchacha20_poly1305_rtpsize", "xsalsa20_poly1305", "xsalsa20_poly1305_lite", "xsalsa20_poly1305_lite_rtpsize", "xsalsa20_poly1305_suffix"})
    void doesNotAllocatePerPacket(String name) {
        var threads = ManagementFactory.getThreadMXBean();
        assumeTrue(threads instanceof com.sun.management.ThreadMXBean
                && ((com.sun.management.ThreadMXBean) threads).isThreadAllocatedMemorySupported());
        var allocations = (com.sun.management.ThreadMXBean) threads;

        var mode = EncryptionMode.get(name);
        var payload = randomBytes(MAX_OPUS_FRAME);
        // Heap buffers, how Netty accesses direct ones depends on Unsafe availability and JIT state, and may allocate.
        ByteBuf plain = Unpooled.buffer(MAX_OPUS_FRAME);
        ByteBuf output = Unpooled.buffer(2048);
        try {
            for (int i = 0; i < 10_000; i++) {
                boxInto(mode, plain, output, payload, i);
            }

            long threadId = Thread.currentThread().getId();
            long before = allocations.getThreadAllocatedBytes(threadId);
            for (int i = 0; i < 1000; i++) {
                boxInto(mode, plain, output, payload, i);
            }
            long allocated = allocations.getThreadAllocatedBytes(threadId) - before;

            assertTrue(allocated < 1000, "allocated " + allocated + " bytes for 1000 packets");
        } finally {
            plain.release();
            output.release();
        }
    }

    private byte[] seal(EncryptionMode mode, int seq, byte[] payload) {
        return seal(mode, (char) seq, seq * 960, payload, key);
    }

    private static byte[] sealVectorFrame(EncryptionMode mode, int index) {
        return seal(mode, (char) (0xFFFE + index), 0x01020304 + index * 960, vectorPayload(index), vectorKey());
    }

    private static byte[] seal(EncryptionMode mode, char seq, int timestamp, byte[] payload, byte[] key) {
        ByteBuf plain = Unpooled.directBuffer(payload.length).writeBytes(payload);
        ByteBuf output = Unpooled.directBuffer();
        try {
            RTPHeaderWriter.writeV2(output, (byte) 120, seq, timestamp, SSRC, false);
            assertTrue(mode.box(plain, payload.length, output, key), "box failed");
            return ByteBufUtil.getBytes(output);
        } finally {
            plain.release();
            output.release();
        }
    }

    private void boxInto(EncryptionMode mode, ByteBuf plain, ByteBuf output, byte[] payload, int seq) {
        plain.clear().writeBytes(payload);
        output.clear();
        RTPHeaderWriter.writeV2(output, (byte) 120, (char) seq, seq * 960, SSRC, false);
        assertTrue(mode.box(plain, payload.length, output, key), "box failed");
    }

    private static byte[] header(int seq) {
        ByteBuf buf = Unpooled.buffer(HEADER_LENGTH);
        try {
            RTPHeaderWriter.writeV2(buf, (byte) 120, (char) seq, seq * 960, SSRC, false);
            return ByteBufUtil.getBytes(buf);
        } finally {
            buf.release();
        }
    }

    private static byte[] vectorKey() {
        var key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (0xA0 + i);
        return key;
    }

    private static byte[] vectorPayload(int index) {
        var payload = new byte[VECTOR_FRAME_SIZES[index]];
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) (index * 0x25 + i * 0x0b + 1);
        return payload;
    }

    private static Map<String, List<String>> loadVectors() throws Exception {
        var vectors = new HashMap<String, List<String>>();
        try (var in = EncryptionModeOracleTest.class.getResourceAsStream(VECTORS_RESOURCE)) {
            assertNotNull(in, VECTORS_RESOURCE + " not found");
            var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                var parts = line.split("\\s+");
                vectors.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
            }
        }
        return vectors;
    }

    private byte[] randomBytes(int size) {
        var bytes = new byte[size];
        random.nextBytes(bytes);
        return bytes;
    }
}
