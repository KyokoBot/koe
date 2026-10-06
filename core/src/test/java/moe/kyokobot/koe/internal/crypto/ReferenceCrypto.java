package moe.kyokobot.koe.internal.crypto;

import org.bouncycastle.crypto.engines.XSalsa20Engine;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;

/**
 * Receiving side of every encryption mode, built only on the JDK and Bouncy Castle so it shares no code with Koe.
 * Simplified and unoptimized on purpose, written for correctness and readability, use only as a test oracle.
 */
final class ReferenceCrypto {
    static final int HEADER_LENGTH = 12;
    static final int TAG_LENGTH = 16;

    static final Map<String, Receiver> RECEIVERS = Map.of(
            "aead_aes256_gcm_rtpsize", new Receiver(4, (packet, key) -> openAesGcm(packet, key, rtpSizeHeaderLength(packet))),
            "aead_aes256_gcm", new Receiver(4, (packet, key) -> openAesGcm(packet, key, HEADER_LENGTH)),
            "aead_xchacha20_poly1305_rtpsize", new Receiver(4, ReferenceCrypto::openXChaChaRtpSize),
            "xsalsa20_poly1305", new Receiver(0, ReferenceCrypto::openXSalsa20),
            "xsalsa20_poly1305_lite", new Receiver(4, (packet, key) -> openXSalsa20Lite(packet, key, HEADER_LENGTH)),
            "xsalsa20_poly1305_lite_rtpsize", new Receiver(4, (packet, key) -> openXSalsa20Lite(packet, key, rtpSizeHeaderLength(packet))),
            "xsalsa20_poly1305_suffix", new Receiver(24, ReferenceCrypto::openXSalsa20Suffix),
            "plain", new Receiver(0, (packet, key) -> Arrays.copyOfRange(packet, HEADER_LENGTH, packet.length))
    );

    private ReferenceCrypto() {
        //
    }

    // The rtpsize modes also leave CSRCs and the header extension preamble unencrypted, the others only the fixed
    // 12 byte header. The extension body is always encrypted.
    private static int rtpSizeHeaderLength(byte[] packet) {
        int csrcCount = packet[0] & 0x0f;
        boolean extension = (packet[0] & 0x10) != 0;
        return HEADER_LENGTH + csrcCount * 4 + (extension ? 4 : 0);
    }

    // aead_aes256_gcm: AAD is the unencrypted header, the nonce is a 32-bit counter appended to the packet and
    // zero-padded to 12 bytes.
    private static byte[] openAesGcm(byte[] packet, byte[] key, int headerLength) throws GeneralSecurityException {
        var nonce = Arrays.copyOf(Arrays.copyOfRange(packet, packet.length - 4, packet.length), 12);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH * 8, nonce));
        cipher.updateAAD(packet, 0, headerLength);
        return cipher.doFinal(packet, headerLength, packet.length - headerLength - 4);
    }

    // aead_xchacha20_poly1305_rtpsize: same layout as above, nonce zero-padded to 24 bytes.
    private static byte[] openXChaChaRtpSize(byte[] packet, byte[] key) throws GeneralSecurityException {
        var nonce = Arrays.copyOf(Arrays.copyOfRange(packet, packet.length - 4, packet.length), 24);
        var subKey = hChaCha20(key, Arrays.copyOf(nonce, 16));
        var ietfNonce = new byte[12];
        System.arraycopy(nonce, 16, ietfNonce, 4, 8);

        var cipher = Cipher.getInstance("ChaCha20-Poly1305");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(subKey, "ChaCha20"), new IvParameterSpec(ietfNonce));
        int headerLength = rtpSizeHeaderLength(packet);
        cipher.updateAAD(packet, 0, headerLength);
        return cipher.doFinal(packet, headerLength, packet.length - headerLength - 4);
    }

    // xsalsa20_poly1305: the nonce is the RTP header zero-padded to 24 bytes.
    private static byte[] openXSalsa20(byte[] packet, byte[] key) throws GeneralSecurityException {
        var nonce = Arrays.copyOf(Arrays.copyOf(packet, HEADER_LENGTH), 24);
        return secretBoxOpen(Arrays.copyOfRange(packet, HEADER_LENGTH, packet.length), nonce, key);
    }

    // xsalsa20_poly1305_lite: a 32-bit counter is appended to the packet, zero-padded to 24 bytes.
    private static byte[] openXSalsa20Lite(byte[] packet, byte[] key, int headerLength) throws GeneralSecurityException {
        var nonce = Arrays.copyOf(Arrays.copyOfRange(packet, packet.length - 4, packet.length), 24);
        return secretBoxOpen(Arrays.copyOfRange(packet, headerLength, packet.length - 4), nonce, key);
    }

    // xsalsa20_poly1305_suffix: the full 24-byte nonce is appended to the packet.
    private static byte[] openXSalsa20Suffix(byte[] packet, byte[] key) throws GeneralSecurityException {
        var nonce = Arrays.copyOfRange(packet, packet.length - 24, packet.length);
        return secretBoxOpen(Arrays.copyOfRange(packet, HEADER_LENGTH, packet.length - 24), nonce, key);
    }

    // NaCl crypto_secretbox_open: tag || ciphertext, first 32 keystream bytes are the Poly1305 key.
    private static byte[] secretBoxOpen(byte[] box, byte[] nonce, byte[] key) throws GeneralSecurityException {
        if (box.length < TAG_LENGTH) {
            throw new AEADBadTagException("box too short");
        }

        var xsalsa = new XSalsa20Engine();
        xsalsa.init(false, new ParametersWithIV(new KeyParameter(key), nonce));
        var polyKey = new byte[32];
        xsalsa.processBytes(new byte[32], 0, 32, polyKey, 0);

        var mac = new Poly1305();
        mac.init(new KeyParameter(polyKey));
        mac.update(box, TAG_LENGTH, box.length - TAG_LENGTH);
        var tag = new byte[TAG_LENGTH];
        mac.doFinal(tag, 0);
        if (!MessageDigest.isEqual(tag, Arrays.copyOf(box, TAG_LENGTH))) {
            throw new AEADBadTagException("Poly1305 tag mismatch");
        }

        var plain = new byte[box.length - TAG_LENGTH];
        xsalsa.processBytes(box, TAG_LENGTH, plain.length, plain, 0);
        return plain;
    }

    /**
     * HChaCha20 subkey derivation for XChaCha20, as in draft-irtf-cfrg-xchacha-03 section 2.2.
     * The JDK only ships the IETF ChaCha20-Poly1305 with 12-byte nonces, which XChaCha20 builds upon.
     * Simplified, unoptimized implementation for correctness.
     */
    static byte[] hChaCha20(byte[] key, byte[] nonce) {
        var k = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
        var n = ByteBuffer.wrap(nonce).order(ByteOrder.LITTLE_ENDIAN);
        int[] s = new int[16];
        s[0] = 0x61707865;
        s[1] = 0x3320646e;
        s[2] = 0x79622d32;
        s[3] = 0x6b206574;
        for (int i = 0; i < 8; i++) s[4 + i] = k.getInt(i * 4);
        for (int i = 0; i < 4; i++) s[12 + i] = n.getInt(i * 4);

        for (int i = 0; i < 10; i++) {
            quarterRound(s, 0, 4, 8, 12);
            quarterRound(s, 1, 5, 9, 13);
            quarterRound(s, 2, 6, 10, 14);
            quarterRound(s, 3, 7, 11, 15);
            quarterRound(s, 0, 5, 10, 15);
            quarterRound(s, 1, 6, 11, 12);
            quarterRound(s, 2, 7, 8, 13);
            quarterRound(s, 3, 4, 9, 14);
        }

        var out = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 4; i++) out.putInt(s[i]);
        for (int i = 12; i < 16; i++) out.putInt(s[i]);
        return out.array();
    }

    private static void quarterRound(int[] s, int a, int b, int c, int d) {
        s[a] += s[b]; s[d] = Integer.rotateLeft(s[d] ^ s[a], 16);
        s[c] += s[d]; s[b] = Integer.rotateLeft(s[b] ^ s[c], 12);
        s[a] += s[b]; s[d] = Integer.rotateLeft(s[d] ^ s[a], 8);
        s[c] += s[d]; s[b] = Integer.rotateLeft(s[b] ^ s[c], 7);
    }

    interface Opener {
        byte[] open(byte[] packet, byte[] key) throws GeneralSecurityException;
    }

    static final class Receiver {
        /** Number of nonce bytes appended after the encrypted payload. */
        final int nonceSuffix;
        final Opener opener;

        Receiver(int nonceSuffix, Opener opener) {
            this.nonceSuffix = nonceSuffix;
            this.opener = opener;
        }

        byte[] open(byte[] packet, byte[] key) throws GeneralSecurityException {
            return opener.open(packet, key);
        }
    }
}
