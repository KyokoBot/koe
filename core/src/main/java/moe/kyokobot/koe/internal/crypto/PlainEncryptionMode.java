package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

public class PlainEncryptionMode implements EncryptionMode {
    @Override
    public void validateKey(byte[] secretKey) {
        // unused
    }

    @Override
    public boolean box(ByteBuf plain, int len, ByteBuf output, byte[] secretKey) {
        output.writeBytes(plain, len);
        return true;
    }

    @Override
    public String getName() {
        return "plain";
    }
}
