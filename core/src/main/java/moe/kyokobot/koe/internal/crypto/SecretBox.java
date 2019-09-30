/*
 * MIT License
 *
 * Copyright (c) 2016 tom zhou,iwebpp@gmail.com
 * Copyright (c) 2019 Gabriel Konopiński (gabixdev)
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

import org.jetbrains.annotations.Nullable;

import static moe.kyokobot.koe.internal.crypto.TweetNaclFast.cryptoSecretboxXSalsa20Poly1305;

@SuppressWarnings("squid:S1168")
public class SecretBox {
    private byte[] key;

    public SecretBox(byte[] key) {
        this.key = key;
    }

    @Nullable
    public byte[] box(byte[] message, byte[] theNonce) {
        if (message == null) return null;
        return box(message, 0, message.length, theNonce);
    }

    @Nullable
    public byte[] box(byte[] message, final int offset, final int len, byte[] theNonce) {
        // check message
        if (!(message != null && message.length >= (offset + len) &&
                theNonce != null && theNonce.length == 24))
            return null;

        // message buffer
        byte[] m = new byte[len + 32];

        // cipher buffer
        byte[] c = new byte[m.length];

        if (len >= 0) System.arraycopy(message, offset, m, 32, len);

        if (0 != cryptoSecretboxXSalsa20Poly1305(c, m, m.length, theNonce, key))
            return null;

        byte[] ret = new byte[c.length - 16];
        System.arraycopy(c, 16, ret, 0, ret.length);
        return ret;
    }
}
