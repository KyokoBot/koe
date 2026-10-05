/*
 * MIT License
 *
 * Copyright (c) 2016 tom zhou,iwebpp@gmail.com
 * Copyright (c) 2019 Alula
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package moe.kyokobot.koe.internal.crypto;

/**
 * XSalsa20-Poly1305 authenticated encryption (NaCl crypto_secretbox), which doesn't allocate after construction.
 * Not thread safe.
 */
@SuppressWarnings("Duplicates")
final class XSalsa20Poly1305 {
    private final byte[] o = new byte[64];
    private final Poly1305 poly1305 = new Poly1305();

    // HSalsa20 subkey
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
     * @param key           32 byte key
     * @param nonce         24 byte nonce
     * @param payload       data to encrypt in place
     * @param payloadLength number of bytes of {@code payload} to encrypt
     * @param tag           16 byte output for the authentication tag
     */
    void seal(byte[] key, byte[] nonce, byte[] payload, int payloadLength, byte[] tag) {
        hSalsa20(key, nonce);

        // Salsa20 with the derived subkey and the last 8 nonce bytes.
        int n0 = nonce[16] & 0xff | (nonce[17] & 0xff) << 8 | (nonce[18] & 0xff) << 16 | (nonce[19] & 0xff) << 24;
        int n1 = nonce[20] & 0xff | (nonce[21] & 0xff) << 8 | (nonce[22] & 0xff) << 16 | (nonce[23] & 0xff) << 24;

        // The first 32 keystream bytes are the Poly1305 key, the payload is encrypted with the ones after.
        salsa20Block(0, n0, n1);
        poly1305.init(o);
        int end = Math.min(32, payloadLength);
        for (int i = 0; i < end; i++) {
            payload[i] ^= o[32 + i];
        }

        int counter = 1;
        for (int offset = 32; offset < payloadLength; offset += 64) {
            salsa20Block(counter++, n0, n1);
            end = Math.min(64, payloadLength - offset);
            for (int i = 0; i < end; i++) {
                payload[offset + i] ^= o[i];
            }
        }

        poly1305.update(payload, 0, payloadLength);
        poly1305.finish(tag, 0);
    }

    private void hSalsa20(byte[] key, byte[] nonce) {
        int j0 = 0x61707865;
        int j1 = key[0] & 0xff | (key[1] & 0xff) << 8 | (key[2] & 0xff) << 16 | (key[3] & 0xff) << 24;
        int j2 = key[4] & 0xff | (key[5] & 0xff) << 8 | (key[6] & 0xff) << 16 | (key[7] & 0xff) << 24;
        int j3 = key[8] & 0xff | (key[9] & 0xff) << 8 | (key[10] & 0xff) << 16 | (key[11] & 0xff) << 24;
        int j4 = key[12] & 0xff | (key[13] & 0xff) << 8 | (key[14] & 0xff) << 16 | (key[15] & 0xff) << 24;
        int j5 = 0x3320646e;
        int j6 = nonce[0] & 0xff | (nonce[1] & 0xff) << 8 | (nonce[2] & 0xff) << 16 | (nonce[3] & 0xff) << 24;
        int j7 = nonce[4] & 0xff | (nonce[5] & 0xff) << 8 | (nonce[6] & 0xff) << 16 | (nonce[7] & 0xff) << 24;
        int j8 = nonce[8] & 0xff | (nonce[9] & 0xff) << 8 | (nonce[10] & 0xff) << 16 | (nonce[11] & 0xff) << 24;
        int j9 = nonce[12] & 0xff | (nonce[13] & 0xff) << 8 | (nonce[14] & 0xff) << 16 | (nonce[15] & 0xff) << 24;
        int j10 = 0x79622d32;
        int j11 = key[16] & 0xff | (key[17] & 0xff) << 8 | (key[18] & 0xff) << 16 | (key[19] & 0xff) << 24;
        int j12 = key[20] & 0xff | (key[21] & 0xff) << 8 | (key[22] & 0xff) << 16 | (key[23] & 0xff) << 24;
        int j13 = key[24] & 0xff | (key[25] & 0xff) << 8 | (key[26] & 0xff) << 16 | (key[27] & 0xff) << 24;
        int j14 = key[28] & 0xff | (key[29] & 0xff) << 8 | (key[30] & 0xff) << 16 | (key[31] & 0xff) << 24;
        int j15 = 0x6b206574;

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
        int u;

        for (int i = 0; i < 20; i += 2) {
            u = x0 + x12;
            x4 ^= u << 7 | u >>> 25;
            u = x4 + x0;
            x8 ^= u << 9 | u >>> 23;
            u = x8 + x4;
            x12 ^= u << 13 | u >>> 19;
            u = x12 + x8;
            x0 ^= u << 18 | u >>> 14;

            u = x5 + x1;
            x9 ^= u << 7 | u >>> 25;
            u = x9 + x5;
            x13 ^= u << 9 | u >>> 23;
            u = x13 + x9;
            x1 ^= u << 13 | u >>> 19;
            u = x1 + x13;
            x5 ^= u << 18 | u >>> 14;

            u = x10 + x6;
            x14 ^= u << 7 | u >>> 25;
            u = x14 + x10;
            x2 ^= u << 9 | u >>> 23;
            u = x2 + x14;
            x6 ^= u << 13 | u >>> 19;
            u = x6 + x2;
            x10 ^= u << 18 | u >>> 14;

            u = x15 + x11;
            x3 ^= u << 7 | u >>> 25;
            u = x3 + x15;
            x7 ^= u << 9 | u >>> 23;
            u = x7 + x3;
            x11 ^= u << 13 | u >>> 19;
            u = x11 + x7;
            x15 ^= u << 18 | u >>> 14;


            u = x0 + x3;
            x1 ^= u << 7 | u >>> 25;
            u = x1 + x0;
            x2 ^= u << 9 | u >>> 23;
            u = x2 + x1;
            x3 ^= u << 13 | u >>> 19;
            u = x3 + x2;
            x0 ^= u << 18 | u >>> 14;

            u = x5 + x4;
            x6 ^= u << 7 | u >>> 25;
            u = x6 + x5;
            x7 ^= u << 9 | u >>> 23;
            u = x7 + x6;
            x4 ^= u << 13 | u >>> 19;
            u = x4 + x7;
            x5 ^= u << 18 | u >>> 14;

            u = x10 + x9;
            x11 ^= u << 7 | u >>> 25;
            u = x11 + x10;
            x8 ^= u << 9 | u >>> 23;
            u = x8 + x11;
            x9 ^= u << 13 | u >>> 19;
            u = x9 + x8;
            x10 ^= u << 18 | u >>> 14;

            u = x15 + x14;
            x12 ^= u << 7 | u >>> 25;
            u = x12 + x15;
            x13 ^= u << 9 | u >>> 23;
            u = x13 + x12;
            x14 ^= u << 13 | u >>> 19;
            u = x14 + x13;
            x15 ^= u << 18 | u >>> 14;
        }

        k0 = x0;
        k1 = x5;
        k2 = x10;
        k3 = x15;
        k4 = x6;
        k5 = x7;
        k6 = x8;
        k7 = x9;
    }

    private void salsa20Block(int counter, int n0, int n1) {
        int j0 = 0x61707865;
        int j1 = k0;
        int j2 = k1;
        int j3 = k2;
        int j4 = k3;
        int j5 = 0x3320646e;
        int j6 = n0;
        int j7 = n1;
        int j8 = counter;
        int j9 = 0;
        int j10 = 0x79622d32;
        int j11 = k4;
        int j12 = k5;
        int j13 = k6;
        int j14 = k7;
        int j15 = 0x6b206574;

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
        int u;

        for (int i = 0; i < 20; i += 2) {
            u = x0 + x12;
            x4 ^= u << 7 | u >>> 25;
            u = x4 + x0;
            x8 ^= u << 9 | u >>> 23;
            u = x8 + x4;
            x12 ^= u << 13 | u >>> 19;
            u = x12 + x8;
            x0 ^= u << 18 | u >>> 14;

            u = x5 + x1;
            x9 ^= u << 7 | u >>> 25;
            u = x9 + x5;
            x13 ^= u << 9 | u >>> 23;
            u = x13 + x9;
            x1 ^= u << 13 | u >>> 19;
            u = x1 + x13;
            x5 ^= u << 18 | u >>> 14;

            u = x10 + x6;
            x14 ^= u << 7 | u >>> 25;
            u = x14 + x10;
            x2 ^= u << 9 | u >>> 23;
            u = x2 + x14;
            x6 ^= u << 13 | u >>> 19;
            u = x6 + x2;
            x10 ^= u << 18 | u >>> 14;

            u = x15 + x11;
            x3 ^= u << 7 | u >>> 25;
            u = x3 + x15;
            x7 ^= u << 9 | u >>> 23;
            u = x7 + x3;
            x11 ^= u << 13 | u >>> 19;
            u = x11 + x7;
            x15 ^= u << 18 | u >>> 14;


            u = x0 + x3;
            x1 ^= u << 7 | u >>> 25;
            u = x1 + x0;
            x2 ^= u << 9 | u >>> 23;
            u = x2 + x1;
            x3 ^= u << 13 | u >>> 19;
            u = x3 + x2;
            x0 ^= u << 18 | u >>> 14;

            u = x5 + x4;
            x6 ^= u << 7 | u >>> 25;
            u = x6 + x5;
            x7 ^= u << 9 | u >>> 23;
            u = x7 + x6;
            x4 ^= u << 13 | u >>> 19;
            u = x4 + x7;
            x5 ^= u << 18 | u >>> 14;

            u = x10 + x9;
            x11 ^= u << 7 | u >>> 25;
            u = x11 + x10;
            x8 ^= u << 9 | u >>> 23;
            u = x8 + x11;
            x9 ^= u << 13 | u >>> 19;
            u = x9 + x8;
            x10 ^= u << 18 | u >>> 14;

            u = x15 + x14;
            x12 ^= u << 7 | u >>> 25;
            u = x12 + x15;
            x13 ^= u << 9 | u >>> 23;
            u = x13 + x12;
            x14 ^= u << 13 | u >>> 19;
            u = x14 + x13;
            x15 ^= u << 18 | u >>> 14;
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
