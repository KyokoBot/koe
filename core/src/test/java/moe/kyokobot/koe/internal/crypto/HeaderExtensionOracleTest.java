package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.internal.KoeClientImpl;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static moe.kyokobot.koe.internal.crypto.ReferenceCrypto.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks packets with an RTP header extension against {@link ReferenceCrypto}, the rtpsize modes have to leave the
 * extension preamble unencrypted.
 */
class HeaderExtensionOracleTest {
    // One-byte header extension (RFC 8285) with one 32-bit word: element id 1, one byte of data, padding.
    private static final byte[] EXTENSION = {(byte) 0xBE, (byte) 0xDE, 0x00, 0x01, 0x10, (byte) 0xAA, 0x00, 0x00};

    private final byte[] key = new byte[32];
    private final Random random = new Random(0x6b6f65);
    private KoeOptions options;
    private MediaConnectionImpl connection;
    private DiscordUDPConnection udp;

    @BeforeEach
    void setUp() {
        random.nextBytes(key);
        options = KoeOptions.builder().setDAVEEnabled(false).create();
        connection = new MediaConnectionImpl(new KoeClientImpl(1L, options), 2L);
        udp = new DiscordUDPConnection(connection, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9), 1);
        connection.setConnectionHandler(udp);
    }

    @AfterEach
    void tearDown() {
        connection.close();
        options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }

    static Set<String> modes() {
        return new TreeSet<>(RECEIVERS.keySet());
    }

    @ParameterizedTest
    @MethodSource("modes")
    void extensionIsSentInTheHeader(String name) throws Exception {
        startSession(name);
        var payload = randomBytes(160);

        var packet = createPacket(concat(EXTENSION, payload), true);

        assertNotNull(packet);
        assertEquals(0x10, packet[0] & 0x10, "extension bit");
        int unencrypted = name.endsWith("_rtpsize") ? HEADER_LENGTH + 4 : HEADER_LENGTH;
        var decrypted = RECEIVERS.get(name).open(packet, key);
        assertArrayEquals(concat(EXTENSION, payload), concat(Arrays.copyOfRange(packet, HEADER_LENGTH, unencrypted), decrypted));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aead_aes256_gcm_rtpsize", "aead_xchacha20_poly1305_rtpsize"})
    void tamperedExtensionPreambleIsRejected(String name) {
        startSession(name);
        var packet = createPacket(concat(EXTENSION, randomBytes(160)), true);
        packet[HEADER_LENGTH + 1] ^= 0x01; // extension profile

        assertThrows(GeneralSecurityException.class, () -> RECEIVERS.get(name).open(packet, key));
    }

    @Test
    void extensionLongerThanTheFrameIsDropped() {
        startSession("aead_aes256_gcm_rtpsize");

        assertNull(createPacket(new byte[]{(byte) 0xBE, (byte) 0xDE, 0x00, 0x02, 0x10, (byte) 0xAA, 0x00, 0x00}, true));
        assertNull(createPacket(new byte[]{(byte) 0xBE, (byte) 0xDE}, true));
    }

    private void startSession(String mode) {
        var keyArray = new JsonArray();
        for (byte b : key) keyArray.add(b & 0xff);
        udp.handleSessionDescription(new JsonObject().add("mode", mode).add("secret_key", keyArray));
    }

    private byte[] createPacket(byte[] data, boolean extension) {
        var frame = Unpooled.directBuffer(data.length).writeBytes(data);
        try {
            var packet = udp.createPacket(CodecType.VIDEO, (byte) 101, 0, frame, data.length, extension);
            if (packet == null) {
                return null;
            }
            try {
                return ByteBufUtil.getBytes(packet);
            } finally {
                packet.release();
            }
        } finally {
            frame.release();
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        var out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private byte[] randomBytes(int size) {
        var bytes = new byte[size];
        random.nextBytes(bytes);
        return bytes;
    }
}
