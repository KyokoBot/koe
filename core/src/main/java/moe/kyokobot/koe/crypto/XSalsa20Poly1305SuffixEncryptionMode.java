package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;

public class XSalsa20Poly1305SuffixEncryptionMode implements EncryptionMode {
    @Override
    public boolean box(ByteBuf opus, int len, ByteBuf output, byte[] secretKey) {
        // TODO
        return false;
    }

    @Override
    public String getName() {
        return "xsalsa20_poly1305_suffix";
    }
}
