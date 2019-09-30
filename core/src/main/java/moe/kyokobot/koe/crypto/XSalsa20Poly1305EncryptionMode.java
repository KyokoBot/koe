package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.internal.crypto.TweetNaclFast;

public class XSalsa20Poly1305EncryptionMode implements EncryptionMode {
    private final byte[] extendedNonce = new byte[24];
    private final byte[] m = new byte[984];
    private final byte[] c = new byte[984];

    @Override
    public boolean box(ByteBuf opus, int len, ByteBuf output, byte[] secretKey) {
        for (int i = 0; i < c.length; i++) {
            m[i] = 0;
            c[i] = 0;
        }

        opus.retain();
        for (int i = 0; i < len; i++) {
            m[i + 32] = opus.readByte();
        }
        opus.release();

        output.getBytes(0, extendedNonce, 0, 12);

        if (0 == TweetNaclFast.cryptoSecretboxXSalsa20Poly1305(c, m, len + 32, extendedNonce, secretKey)) {
            for (int i = 0; i < (len + 16); i++) {
                output.writeByte(c[i + 16]);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305";
    }
}
