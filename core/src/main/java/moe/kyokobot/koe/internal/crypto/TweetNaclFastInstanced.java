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

@SuppressWarnings("Duplicates")
public final class TweetNaclFastInstanced {
    private void coreSalsa20(byte[] o, byte[] p, byte[] k) {
        int j0 = sigma[0] & 0xff | (sigma[1] & 0xff) << 8 | (sigma[2] & 0xff) << 16 | (sigma[3] & 0xff) << 24;
        int j1 = k[0] & 0xff | (k[1] & 0xff) << 8 | (k[2] & 0xff) << 16 | (k[3] & 0xff) << 24;
        int j2 = k[4] & 0xff | (k[5] & 0xff) << 8 | (k[6] & 0xff) << 16 | (k[7] & 0xff) << 24;
        int j3 = k[8] & 0xff | (k[9] & 0xff) << 8 | (k[10] & 0xff) << 16 | (k[11] & 0xff) << 24;
        int j4 = k[12] & 0xff | (k[13] & 0xff) << 8 | (k[14] & 0xff) << 16 | (k[15] & 0xff) << 24;
        int j5 = sigma[4] & 0xff | (sigma[5] & 0xff) << 8 | (sigma[6] & 0xff) << 16 | (sigma[7] & 0xff) << 24;
        int j6 = p[0] & 0xff | (p[1] & 0xff) << 8 | (p[2] & 0xff) << 16 | (p[3] & 0xff) << 24;
        int j7 = p[4] & 0xff | (p[5] & 0xff) << 8 | (p[6] & 0xff) << 16 | (p[7] & 0xff) << 24;
        int j8 = p[8] & 0xff | (p[9] & 0xff) << 8 | (p[10] & 0xff) << 16 | (p[11] & 0xff) << 24;
        int j9 = p[12] & 0xff | (p[13] & 0xff) << 8 | (p[14] & 0xff) << 16 | (p[15] & 0xff) << 24;
        int j10 = sigma[8] & 0xff | (sigma[9] & 0xff) << 8 | (sigma[10] & 0xff) << 16 | (sigma[11] & 0xff) << 24;
        int j11 = k[16] & 0xff | (k[17] & 0xff) << 8 | (k[18] & 0xff) << 16 | (k[19] & 0xff) << 24;
        int j12 = k[20] & 0xff | (k[21] & 0xff) << 8 | (k[22] & 0xff) << 16 | (k[23] & 0xff) << 24;
        int j13 = k[24] & 0xff | (k[25] & 0xff) << 8 | (k[26] & 0xff) << 16 | (k[27] & 0xff) << 24;
        int j14 = k[28] & 0xff | (k[29] & 0xff) << 8 | (k[30] & 0xff) << 16 | (k[31] & 0xff) << 24;
        int j15 = sigma[12] & 0xff | (sigma[13] & 0xff) << 8 | (sigma[14] & 0xff) << 16 | (sigma[15] & 0xff) << 24;

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
            x4 ^= u << 7 | u >>> (32 - 7);
            u = x4 + x0;
            x8 ^= u << 9 | u >>> (32 - 9);
            u = x8 + x4;
            x12 ^= u << 13 | u >>> (32 - 13);
            u = x12 + x8;
            x0 ^= u << 18 | u >>> (32 - 18);

            u = x5 + x1;
            x9 ^= u << 7 | u >>> (32 - 7);
            u = x9 + x5;
            x13 ^= u << 9 | u >>> (32 - 9);
            u = x13 + x9;
            x1 ^= u << 13 | u >>> (32 - 13);
            u = x1 + x13;
            x5 ^= u << 18 | u >>> (32 - 18);

            u = x10 + x6;
            x14 ^= u << 7 | u >>> (32 - 7);
            u = x14 + x10;
            x2 ^= u << 9 | u >>> (32 - 9);
            u = x2 + x14;
            x6 ^= u << 13 | u >>> (32 - 13);
            u = x6 + x2;
            x10 ^= u << 18 | u >>> (32 - 18);

            u = x15 + x11;
            x3 ^= u << 7 | u >>> (32 - 7);
            u = x3 + x15;
            x7 ^= u << 9 | u >>> (32 - 9);
            u = x7 + x3;
            x11 ^= u << 13 | u >>> (32 - 13);
            u = x11 + x7;
            x15 ^= u << 18 | u >>> (32 - 18);

            u = x0 + x3;
            x1 ^= u << 7 | u >>> (32 - 7);
            u = x1 + x0;
            x2 ^= u << 9 | u >>> (32 - 9);
            u = x2 + x1;
            x3 ^= u << 13 | u >>> (32 - 13);
            u = x3 + x2;
            x0 ^= u << 18 | u >>> (32 - 18);

            u = x5 + x4;
            x6 ^= u << 7 | u >>> (32 - 7);
            u = x6 + x5;
            x7 ^= u << 9 | u >>> (32 - 9);
            u = x7 + x6;
            x4 ^= u << 13 | u >>> (32 - 13);
            u = x4 + x7;
            x5 ^= u << 18 | u >>> (32 - 18);

            u = x10 + x9;
            x11 ^= u << 7 | u >>> (32 - 7);
            u = x11 + x10;
            x8 ^= u << 9 | u >>> (32 - 9);
            u = x8 + x11;
            x9 ^= u << 13 | u >>> (32 - 13);
            u = x9 + x8;
            x10 ^= u << 18 | u >>> (32 - 18);

            u = x15 + x14;
            x12 ^= u << 7 | u >>> (32 - 7);
            u = x12 + x15;
            x13 ^= u << 9 | u >>> (32 - 9);
            u = x13 + x12;
            x14 ^= u << 13 | u >>> (32 - 13);
            u = x14 + x13;
            x15 ^= u << 18 | u >>> (32 - 18);
        }
        x0 = x0 + j0;
        x1 = x1 + j1;
        x2 = x2 + j2;
        x3 = x3 + j3;
        x4 = x4 + j4;
        x5 = x5 + j5;
        x6 = x6 + j6;
        x7 = x7 + j7;
        x8 = x8 + j8;
        x9 = x9 + j9;
        x10 = x10 + j10;
        x11 = x11 + j11;
        x12 = x12 + j12;
        x13 = x13 + j13;
        x14 = x14 + j14;
        x15 = x15 + j15;

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

    private void coreHSalsa20(byte[] o, byte[] p, byte[] k) {
        int j0 = sigma[0] & 0xff | (sigma[1] & 0xff) << 8 | (sigma[2] & 0xff) << 16 | (sigma[3] & 0xff) << 24;
        int j1 = k[0] & 0xff | (k[1] & 0xff) << 8 | (k[2] & 0xff) << 16 | (k[3] & 0xff) << 24;
        int j2 = k[4] & 0xff | (k[5] & 0xff) << 8 | (k[6] & 0xff) << 16 | (k[7] & 0xff) << 24;
        int j3 = k[8] & 0xff | (k[9] & 0xff) << 8 | (k[10] & 0xff) << 16 | (k[11] & 0xff) << 24;
        int j4 = k[12] & 0xff | (k[13] & 0xff) << 8 | (k[14] & 0xff) << 16 | (k[15] & 0xff) << 24;
        int j5 = sigma[4] & 0xff | (sigma[5] & 0xff) << 8 | (sigma[6] & 0xff) << 16 | (sigma[7] & 0xff) << 24;
        int j6 = p[0] & 0xff | (p[1] & 0xff) << 8 | (p[2] & 0xff) << 16 | (p[3] & 0xff) << 24;
        int j7 = p[4] & 0xff | (p[5] & 0xff) << 8 | (p[6] & 0xff) << 16 | (p[7] & 0xff) << 24;
        int j8 = p[8] & 0xff | (p[9] & 0xff) << 8 | (p[10] & 0xff) << 16 | (p[11] & 0xff) << 24;
        int j9 = p[12] & 0xff | (p[13] & 0xff) << 8 | (p[14] & 0xff) << 16 | (p[15] & 0xff) << 24;
        int j10 = sigma[8] & 0xff | (sigma[9] & 0xff) << 8 | (sigma[10] & 0xff) << 16 | (sigma[11] & 0xff) << 24;
        int j11 = k[16] & 0xff | (k[17] & 0xff) << 8 | (k[18] & 0xff) << 16 | (k[19] & 0xff) << 24;
        int j12 = k[20] & 0xff | (k[21] & 0xff) << 8 | (k[22] & 0xff) << 16 | (k[23] & 0xff) << 24;
        int j13 = k[24] & 0xff | (k[25] & 0xff) << 8 | (k[26] & 0xff) << 16 | (k[27] & 0xff) << 24;
        int j14 = k[28] & 0xff | (k[29] & 0xff) << 8 | (k[30] & 0xff) << 16 | (k[31] & 0xff) << 24;
        int j15 = sigma[12] & 0xff | (sigma[13] & 0xff) << 8 | (sigma[14] & 0xff) << 16 | (sigma[15] & 0xff) << 24;

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
            x4 ^= u << 7 | u >>> (32 - 7);
            u = x4 + x0;
            x8 ^= u << 9 | u >>> (32 - 9);
            u = x8 + x4;
            x12 ^= u << 13 | u >>> (32 - 13);
            u = x12 + x8;
            x0 ^= u << 18 | u >>> (32 - 18);

            u = x5 + x1;
            x9 ^= u << 7 | u >>> (32 - 7);
            u = x9 + x5;
            x13 ^= u << 9 | u >>> (32 - 9);
            u = x13 + x9;
            x1 ^= u << 13 | u >>> (32 - 13);
            u = x1 + x13;
            x5 ^= u << 18 | u >>> (32 - 18);

            u = x10 + x6;
            x14 ^= u << 7 | u >>> (32 - 7);
            u = x14 + x10;
            x2 ^= u << 9 | u >>> (32 - 9);
            u = x2 + x14;
            x6 ^= u << 13 | u >>> (32 - 13);
            u = x6 + x2;
            x10 ^= u << 18 | u >>> (32 - 18);

            u = x15 + x11;
            x3 ^= u << 7 | u >>> (32 - 7);
            u = x3 + x15;
            x7 ^= u << 9 | u >>> (32 - 9);
            u = x7 + x3;
            x11 ^= u << 13 | u >>> (32 - 13);
            u = x11 + x7;
            x15 ^= u << 18 | u >>> (32 - 18);

            u = x0 + x3;
            x1 ^= u << 7 | u >>> (32 - 7);
            u = x1 + x0;
            x2 ^= u << 9 | u >>> (32 - 9);
            u = x2 + x1;
            x3 ^= u << 13 | u >>> (32 - 13);
            u = x3 + x2;
            x0 ^= u << 18 | u >>> (32 - 18);

            u = x5 + x4;
            x6 ^= u << 7 | u >>> (32 - 7);
            u = x6 + x5;
            x7 ^= u << 9 | u >>> (32 - 9);
            u = x7 + x6;
            x4 ^= u << 13 | u >>> (32 - 13);
            u = x4 + x7;
            x5 ^= u << 18 | u >>> (32 - 18);

            u = x10 + x9;
            x11 ^= u << 7 | u >>> (32 - 7);
            u = x11 + x10;
            x8 ^= u << 9 | u >>> (32 - 9);
            u = x8 + x11;
            x9 ^= u << 13 | u >>> (32 - 13);
            u = x9 + x8;
            x10 ^= u << 18 | u >>> (32 - 18);

            u = x15 + x14;
            x12 ^= u << 7 | u >>> (32 - 7);
            u = x12 + x15;
            x13 ^= u << 9 | u >>> (32 - 9);
            u = x13 + x12;
            x14 ^= u << 13 | u >>> (32 - 13);
            u = x14 + x13;
            x15 ^= u << 18 | u >>> (32 - 18);
        }

        o[0] = (byte) (x0 & 0xff);
        o[1] = (byte) (x0 >>> 8 & 0xff);
        o[2] = (byte) (x0 >>> 16 & 0xff);
        o[3] = (byte) (x0 >>> 24 & 0xff);

        o[4] = (byte) (x5 & 0xff);
        o[5] = (byte) (x5 >>> 8 & 0xff);
        o[6] = (byte) (x5 >>> 16 & 0xff);
        o[7] = (byte) (x5 >>> 24 & 0xff);

        o[8] = (byte) (x10 & 0xff);
        o[9] = (byte) (x10 >>> 8 & 0xff);
        o[10] = (byte) (x10 >>> 16 & 0xff);
        o[11] = (byte) (x10 >>> 24 & 0xff);

        o[12] = (byte) (x15 & 0xff);
        o[13] = (byte) (x15 >>> 8 & 0xff);
        o[14] = (byte) (x15 >>> 16 & 0xff);
        o[15] = (byte) (x15 >>> 24 & 0xff);

        o[16] = (byte) (x6 & 0xff);
        o[17] = (byte) (x6 >>> 8 & 0xff);
        o[18] = (byte) (x6 >>> 16 & 0xff);
        o[19] = (byte) (x6 >>> 24 & 0xff);

        o[20] = (byte) (x7 & 0xff);
        o[21] = (byte) (x7 >>> 8 & 0xff);
        o[22] = (byte) (x7 >>> 16 & 0xff);
        o[23] = (byte) (x7 >>> 24 & 0xff);

        o[24] = (byte) (x8 & 0xff);
        o[25] = (byte) (x8 >>> 8 & 0xff);
        o[26] = (byte) (x8 >>> 16 & 0xff);
        o[27] = (byte) (x8 >>> 24 & 0xff);

        o[28] = (byte) (x9 & 0xff);
        o[29] = (byte) (x9 >>> 8 & 0xff);
        o[30] = (byte) (x9 >>> 16 & 0xff);
        o[31] = (byte) (x9 >>> 24 & 0xff);
    }

    private void cryptoCoreSalsa20(byte[] out, byte[] in, byte[] k) {
        coreSalsa20(out, in, k);
    }

    private void cryptoCoreHSalsa20(byte[] out, byte[] in, byte[] k) {
        coreHSalsa20(out, in, k);
    }

    private static final byte[] sigma = {101, 120, 112, 97, 110, 100, 32, 51, 50, 45, 98, 121, 116, 101, 32, 107};

    private final byte[] z = new byte[16];
    private final byte[] x = new byte[64];

    private void cryptoStreamSalsa20Xor(byte[] c, byte[] m, long b, byte[] n, byte[] k) {
        int cpos = 0;
        int mpos = 0;
        int u;
        int i;

        for (i = 0; i < 16; i++) {
            z[i] = 0;
            x[i] = 0;
        }
        for (i = 16; i < 64; i++) {
            x[i] = 0;
        }

        //for (i = 0; i < 16; i++) z[i] = 0;
        System.arraycopy(n, 0, z, 0, 8);

        while (b >= 64) {
            cryptoCoreSalsa20(x, z, k);
            for (i = 0; i < 64; i++) c[cpos + i] = (byte) ((m[mpos + i] ^ x[i]) & 0xff);
            u = 1;
            for (i = 8; i < 16; i++) {
                u = u + (z[i] & 0xff);
                z[i] = (byte) (u & 0xff);
                u >>>= 8;
            }
            b -= 64;
            cpos += 64;
            mpos += 64;
        }
        if (b > 0) {
            cryptoCoreSalsa20(x, z, k);
            for (i = 0; i < b; i++) c[cpos + i] = (byte) ((m[mpos + i] ^ x[i]) & 0xff);
        }
    }

    private final byte[] str = new byte[32];
    private final byte[] sn = new byte[8];

    private void cryptoStreamXor(byte[] c, byte[] m, long d, byte[] n, byte[] k) {
        int i;

        for (i = 0; i < 8; i++) {
            str[i] = 0;
            sn[i] = 0;
        }
        for (i = 8; i < 32; i++) {
            str[i] = 0;
        }

        cryptoCoreHSalsa20(str, n, k);
        System.arraycopy(n, 16, sn, 0, 8);
        cryptoStreamSalsa20Xor(c, m, d, sn, str);
    }

    private final Poly1305 poly1305 = new Poly1305();

    private void cryptoOnetimeAuth(
            byte[] out,
            byte[] m,
            int n,
            byte[] k) {
        poly1305.init(k);
        poly1305.update(m, 32, n);
        poly1305.finish(out, 16);
    }

    public int cryptoSecretboxXSalsa20Poly1305NonSync(byte[] c, byte[] m, int d, byte[] n, byte[] k) {
        if (d < 32) return -1;
        cryptoStreamXor(c, m, d, n, k);
        cryptoOnetimeAuth(c, c, d - 32, c);
        return 0;
    }

    public synchronized int cryptoSecretboxXSalsa20Poly1305(byte[] c, byte[] m, int d, byte[] n, byte[] k) {
        if (d < 32) return -1;
        cryptoStreamXor(c, m, d, n, k);
        cryptoOnetimeAuth(c, c, d - 32, c);
        return 0;
    }
}
