package moe.kyokobot.koe.internal.crypto;

/**
 * XChaCha20-Poly1305 AEAD encryption (RFC 8439), which doesn't allocate after construction. 
 * Not thread safe.
 */
@SuppressWarnings("Duplicates")
final class XChaCha20Poly1305 {
    private static final byte[] ZERO_PADDING = new byte[16];

    private final byte[] o = new byte[64];
    private final byte[] lengths = new byte[16];
    private final Poly1305 poly1305 = new Poly1305();

    // HChaCha20 subkey
    private int k0;
    private int k1;
    private int k2;
    private int k3;
    private int k4;
    private int k5;
    private int k6;
    private int k7;

    /**
     * Encrypts {@code payload} in place and writes the authentication tag to {@code tag}.
     *
     * @param key                  32 byte key
     * @param nonce                24 byte nonce
     * @param associatedData       data authenticated along with the payload
     * @param associatedDataLength number of bytes of {@code associatedData} to authenticate
     * @param payload              data to encrypt in place
     * @param payloadLength        number of bytes of {@code payload} to encrypt
     * @param tag                  16 byte output for the authentication tag
     */
    void seal(byte[] key, byte[] nonce, byte[] associatedData, int associatedDataLength,
              byte[] payload, int payloadLength, byte[] tag) {
        hChaCha20(key, nonce);

        // IETF ChaCha20 with the derived subkey and a 12 byte nonce of 4 zero bytes followed by the last 8 nonce bytes.
        int n0 = nonce[16] & 0xff | (nonce[17] & 0xff) << 8 | (nonce[18] & 0xff) << 16 | (nonce[19] & 0xff) << 24;
        int n1 = nonce[20] & 0xff | (nonce[21] & 0xff) << 8 | (nonce[22] & 0xff) << 16 | (nonce[23] & 0xff) << 24;

        // The first 32 keystream bytes are the Poly1305 key, the payload is encrypted starting from the next block.
        chaCha20Block(0, n0, n1);
        poly1305.init(o);

        int counter = 1;
        for (int offset = 0; offset < payloadLength; offset += 64) {
            chaCha20Block(counter++, n0, n1);
            int end = Math.min(64, payloadLength - offset);
            for (int i = 0; i < end; i++) {
                payload[offset + i] ^= o[i];
            }
        }

        poly1305.update(associatedData, 0, associatedDataLength);
        padTo16(associatedDataLength);
        poly1305.update(payload, 0, payloadLength);
        padTo16(payloadLength);
        writeLongLE(lengths, 0, associatedDataLength);
        writeLongLE(lengths, 8, payloadLength);
        poly1305.update(lengths, 0, 16);
        poly1305.finish(tag, 0);
    }

    private void padTo16(int length) {
        int remainder = length % 16;
        if (remainder != 0) {
            poly1305.update(ZERO_PADDING, 0, 16 - remainder);
        }
    }

    private static void writeLongLE(byte[] b, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            b[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    private void hChaCha20(byte[] key, byte[] nonce) {
        int j0 = 0x61707865;
        int j1 = 0x3320646e;
        int j2 = 0x79622d32;
        int j3 = 0x6b206574;
        int j4 = key[0] & 0xff | (key[1] & 0xff) << 8 | (key[2] & 0xff) << 16 | (key[3] & 0xff) << 24;
        int j5 = key[4] & 0xff | (key[5] & 0xff) << 8 | (key[6] & 0xff) << 16 | (key[7] & 0xff) << 24;
        int j6 = key[8] & 0xff | (key[9] & 0xff) << 8 | (key[10] & 0xff) << 16 | (key[11] & 0xff) << 24;
        int j7 = key[12] & 0xff | (key[13] & 0xff) << 8 | (key[14] & 0xff) << 16 | (key[15] & 0xff) << 24;
        int j8 = key[16] & 0xff | (key[17] & 0xff) << 8 | (key[18] & 0xff) << 16 | (key[19] & 0xff) << 24;
        int j9 = key[20] & 0xff | (key[21] & 0xff) << 8 | (key[22] & 0xff) << 16 | (key[23] & 0xff) << 24;
        int j10 = key[24] & 0xff | (key[25] & 0xff) << 8 | (key[26] & 0xff) << 16 | (key[27] & 0xff) << 24;
        int j11 = key[28] & 0xff | (key[29] & 0xff) << 8 | (key[30] & 0xff) << 16 | (key[31] & 0xff) << 24;
        int j12 = nonce[0] & 0xff | (nonce[1] & 0xff) << 8 | (nonce[2] & 0xff) << 16 | (nonce[3] & 0xff) << 24;
        int j13 = nonce[4] & 0xff | (nonce[5] & 0xff) << 8 | (nonce[6] & 0xff) << 16 | (nonce[7] & 0xff) << 24;
        int j14 = nonce[8] & 0xff | (nonce[9] & 0xff) << 8 | (nonce[10] & 0xff) << 16 | (nonce[11] & 0xff) << 24;
        int j15 = nonce[12] & 0xff | (nonce[13] & 0xff) << 8 | (nonce[14] & 0xff) << 16 | (nonce[15] & 0xff) << 24;

        int x0 = j0;
        int x1 = j1;
        int x2 = j2;
        int x3 = j3;
        int x4 = j4;
        int x5 = j5;
        int x6 = j6;
        int x7 = j7;
        int x8 = j8;
        int x9 = j9;
        int x10 = j10;
        int x11 = j11;
        int x12 = j12;
        int x13 = j13;
        int x14 = j14;
        int x15 = j15;

        for (int i = 0; i < 20; i += 2) {
            x0 += x4;
            x12 ^= x0;
            x12 = x12 << 16 | x12 >>> 16;
            x8 += x12;
            x4 ^= x8;
            x4 = x4 << 12 | x4 >>> 20;
            x0 += x4;
            x12 ^= x0;
            x12 = x12 << 8 | x12 >>> 24;
            x8 += x12;
            x4 ^= x8;
            x4 = x4 << 7 | x4 >>> 25;

            x1 += x5;
            x13 ^= x1;
            x13 = x13 << 16 | x13 >>> 16;
            x9 += x13;
            x5 ^= x9;
            x5 = x5 << 12 | x5 >>> 20;
            x1 += x5;
            x13 ^= x1;
            x13 = x13 << 8 | x13 >>> 24;
            x9 += x13;
            x5 ^= x9;
            x5 = x5 << 7 | x5 >>> 25;

            x2 += x6;
            x14 ^= x2;
            x14 = x14 << 16 | x14 >>> 16;
            x10 += x14;
            x6 ^= x10;
            x6 = x6 << 12 | x6 >>> 20;
            x2 += x6;
            x14 ^= x2;
            x14 = x14 << 8 | x14 >>> 24;
            x10 += x14;
            x6 ^= x10;
            x6 = x6 << 7 | x6 >>> 25;

            x3 += x7;
            x15 ^= x3;
            x15 = x15 << 16 | x15 >>> 16;
            x11 += x15;
            x7 ^= x11;
            x7 = x7 << 12 | x7 >>> 20;
            x3 += x7;
            x15 ^= x3;
            x15 = x15 << 8 | x15 >>> 24;
            x11 += x15;
            x7 ^= x11;
            x7 = x7 << 7 | x7 >>> 25;


            x0 += x5;
            x15 ^= x0;
            x15 = x15 << 16 | x15 >>> 16;
            x10 += x15;
            x5 ^= x10;
            x5 = x5 << 12 | x5 >>> 20;
            x0 += x5;
            x15 ^= x0;
            x15 = x15 << 8 | x15 >>> 24;
            x10 += x15;
            x5 ^= x10;
            x5 = x5 << 7 | x5 >>> 25;

            x1 += x6;
            x12 ^= x1;
            x12 = x12 << 16 | x12 >>> 16;
            x11 += x12;
            x6 ^= x11;
            x6 = x6 << 12 | x6 >>> 20;
            x1 += x6;
            x12 ^= x1;
            x12 = x12 << 8 | x12 >>> 24;
            x11 += x12;
            x6 ^= x11;
            x6 = x6 << 7 | x6 >>> 25;

            x2 += x7;
            x13 ^= x2;
            x13 = x13 << 16 | x13 >>> 16;
            x8 += x13;
            x7 ^= x8;
            x7 = x7 << 12 | x7 >>> 20;
            x2 += x7;
            x13 ^= x2;
            x13 = x13 << 8 | x13 >>> 24;
            x8 += x13;
            x7 ^= x8;
            x7 = x7 << 7 | x7 >>> 25;

            x3 += x4;
            x14 ^= x3;
            x14 = x14 << 16 | x14 >>> 16;
            x9 += x14;
            x4 ^= x9;
            x4 = x4 << 12 | x4 >>> 20;
            x3 += x4;
            x14 ^= x3;
            x14 = x14 << 8 | x14 >>> 24;
            x9 += x14;
            x4 ^= x9;
            x4 = x4 << 7 | x4 >>> 25;
        }

        k0 = x0;
        k1 = x1;
        k2 = x2;
        k3 = x3;
        k4 = x12;
        k5 = x13;
        k6 = x14;
        k7 = x15;
    }

    private void chaCha20Block(int counter, int n0, int n1) {
        int j0 = 0x61707865;
        int j1 = 0x3320646e;
        int j2 = 0x79622d32;
        int j3 = 0x6b206574;
        int j4 = k0;
        int j5 = k1;
        int j6 = k2;
        int j7 = k3;
        int j8 = k4;
        int j9 = k5;
        int j10 = k6;
        int j11 = k7;
        int j12 = counter;
        int j13 = 0;
        int j14 = n0;
        int j15 = n1;

        int x0 = j0;
        int x1 = j1;
        int x2 = j2;
        int x3 = j3;
        int x4 = j4;
        int x5 = j5;
        int x6 = j6;
        int x7 = j7;
        int x8 = j8;
        int x9 = j9;
        int x10 = j10;
        int x11 = j11;
        int x12 = j12;
        int x13 = j13;
        int x14 = j14;
        int x15 = j15;

        for (int i = 0; i < 20; i += 2) {
            x0 += x4;
            x12 ^= x0;
            x12 = x12 << 16 | x12 >>> 16;
            x8 += x12;
            x4 ^= x8;
            x4 = x4 << 12 | x4 >>> 20;
            x0 += x4;
            x12 ^= x0;
            x12 = x12 << 8 | x12 >>> 24;
            x8 += x12;
            x4 ^= x8;
            x4 = x4 << 7 | x4 >>> 25;

            x1 += x5;
            x13 ^= x1;
            x13 = x13 << 16 | x13 >>> 16;
            x9 += x13;
            x5 ^= x9;
            x5 = x5 << 12 | x5 >>> 20;
            x1 += x5;
            x13 ^= x1;
            x13 = x13 << 8 | x13 >>> 24;
            x9 += x13;
            x5 ^= x9;
            x5 = x5 << 7 | x5 >>> 25;

            x2 += x6;
            x14 ^= x2;
            x14 = x14 << 16 | x14 >>> 16;
            x10 += x14;
            x6 ^= x10;
            x6 = x6 << 12 | x6 >>> 20;
            x2 += x6;
            x14 ^= x2;
            x14 = x14 << 8 | x14 >>> 24;
            x10 += x14;
            x6 ^= x10;
            x6 = x6 << 7 | x6 >>> 25;

            x3 += x7;
            x15 ^= x3;
            x15 = x15 << 16 | x15 >>> 16;
            x11 += x15;
            x7 ^= x11;
            x7 = x7 << 12 | x7 >>> 20;
            x3 += x7;
            x15 ^= x3;
            x15 = x15 << 8 | x15 >>> 24;
            x11 += x15;
            x7 ^= x11;
            x7 = x7 << 7 | x7 >>> 25;


            x0 += x5;
            x15 ^= x0;
            x15 = x15 << 16 | x15 >>> 16;
            x10 += x15;
            x5 ^= x10;
            x5 = x5 << 12 | x5 >>> 20;
            x0 += x5;
            x15 ^= x0;
            x15 = x15 << 8 | x15 >>> 24;
            x10 += x15;
            x5 ^= x10;
            x5 = x5 << 7 | x5 >>> 25;

            x1 += x6;
            x12 ^= x1;
            x12 = x12 << 16 | x12 >>> 16;
            x11 += x12;
            x6 ^= x11;
            x6 = x6 << 12 | x6 >>> 20;
            x1 += x6;
            x12 ^= x1;
            x12 = x12 << 8 | x12 >>> 24;
            x11 += x12;
            x6 ^= x11;
            x6 = x6 << 7 | x6 >>> 25;

            x2 += x7;
            x13 ^= x2;
            x13 = x13 << 16 | x13 >>> 16;
            x8 += x13;
            x7 ^= x8;
            x7 = x7 << 12 | x7 >>> 20;
            x2 += x7;
            x13 ^= x2;
            x13 = x13 << 8 | x13 >>> 24;
            x8 += x13;
            x7 ^= x8;
            x7 = x7 << 7 | x7 >>> 25;

            x3 += x4;
            x14 ^= x3;
            x14 = x14 << 16 | x14 >>> 16;
            x9 += x14;
            x4 ^= x9;
            x4 = x4 << 12 | x4 >>> 20;
            x3 += x4;
            x14 ^= x3;
            x14 = x14 << 8 | x14 >>> 24;
            x9 += x14;
            x4 ^= x9;
            x4 = x4 << 7 | x4 >>> 25;
        }

        x0 += j0;
        x1 += j1;
        x2 += j2;
        x3 += j3;
        x4 += j4;
        x5 += j5;
        x6 += j6;
        x7 += j7;
        x8 += j8;
        x9 += j9;
        x10 += j10;
        x11 += j11;
        x12 += j12;
        x13 += j13;
        x14 += j14;
        x15 += j15;

        o[0] = (byte) (x0 & 0xff);
        o[1] = (byte) (x0 >>> 8 & 0xff);
        o[2] = (byte) (x0 >>> 16 & 0xff);
        o[3] = (byte) (x0 >>> 24 & 0xff);

        o[4] = (byte) (x1 & 0xff);
        o[5] = (byte) (x1 >>> 8 & 0xff);
        o[6] = (byte) (x1 >>> 16 & 0xff);
        o[7] = (byte) (x1 >>> 24 & 0xff);

        o[8] = (byte) (x2 & 0xff);
        o[9] = (byte) (x2 >>> 8 & 0xff);
        o[10] = (byte) (x2 >>> 16 & 0xff);
        o[11] = (byte) (x2 >>> 24 & 0xff);

        o[12] = (byte) (x3 & 0xff);
        o[13] = (byte) (x3 >>> 8 & 0xff);
        o[14] = (byte) (x3 >>> 16 & 0xff);
        o[15] = (byte) (x3 >>> 24 & 0xff);

        o[16] = (byte) (x4 & 0xff);
        o[17] = (byte) (x4 >>> 8 & 0xff);
        o[18] = (byte) (x4 >>> 16 & 0xff);
        o[19] = (byte) (x4 >>> 24 & 0xff);

        o[20] = (byte) (x5 & 0xff);
        o[21] = (byte) (x5 >>> 8 & 0xff);
        o[22] = (byte) (x5 >>> 16 & 0xff);
        o[23] = (byte) (x5 >>> 24 & 0xff);

        o[24] = (byte) (x6 & 0xff);
        o[25] = (byte) (x6 >>> 8 & 0xff);
        o[26] = (byte) (x6 >>> 16 & 0xff);
        o[27] = (byte) (x6 >>> 24 & 0xff);

        o[28] = (byte) (x7 & 0xff);
        o[29] = (byte) (x7 >>> 8 & 0xff);
        o[30] = (byte) (x7 >>> 16 & 0xff);
        o[31] = (byte) (x7 >>> 24 & 0xff);

        o[32] = (byte) (x8 & 0xff);
        o[33] = (byte) (x8 >>> 8 & 0xff);
        o[34] = (byte) (x8 >>> 16 & 0xff);
        o[35] = (byte) (x8 >>> 24 & 0xff);

        o[36] = (byte) (x9 & 0xff);
        o[37] = (byte) (x9 >>> 8 & 0xff);
        o[38] = (byte) (x9 >>> 16 & 0xff);
        o[39] = (byte) (x9 >>> 24 & 0xff);

        o[40] = (byte) (x10 & 0xff);
        o[41] = (byte) (x10 >>> 8 & 0xff);
        o[42] = (byte) (x10 >>> 16 & 0xff);
        o[43] = (byte) (x10 >>> 24 & 0xff);

        o[44] = (byte) (x11 & 0xff);
        o[45] = (byte) (x11 >>> 8 & 0xff);
        o[46] = (byte) (x11 >>> 16 & 0xff);
        o[47] = (byte) (x11 >>> 24 & 0xff);

        o[48] = (byte) (x12 & 0xff);
        o[49] = (byte) (x12 >>> 8 & 0xff);
        o[50] = (byte) (x12 >>> 16 & 0xff);
        o[51] = (byte) (x12 >>> 24 & 0xff);

        o[52] = (byte) (x13 & 0xff);
        o[53] = (byte) (x13 >>> 8 & 0xff);
        o[54] = (byte) (x13 >>> 16 & 0xff);
        o[55] = (byte) (x13 >>> 24 & 0xff);

        o[56] = (byte) (x14 & 0xff);
        o[57] = (byte) (x14 >>> 8 & 0xff);
        o[58] = (byte) (x14 >>> 16 & 0xff);
        o[59] = (byte) (x14 >>> 24 & 0xff);

        o[60] = (byte) (x15 & 0xff);
        o[61] = (byte) (x15 >>> 8 & 0xff);
        o[62] = (byte) (x15 >>> 16 & 0xff);
        o[63] = (byte) (x15 >>> 24 & 0xff);
    }
}
