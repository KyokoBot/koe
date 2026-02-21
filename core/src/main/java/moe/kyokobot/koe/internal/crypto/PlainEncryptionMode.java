package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

public class PlainEncryptionMode implements EncryptionMode {
    @Override
    public boolean box(ByteBuf plain, int start, ByteBuf output, byte[] secretKey) {
        plain.readerIndex(start);
        output.writeBytes(plain);
        return true;
    }

    @Override
    public String getName() {
        return "plain";
    }
}
