package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;

public class PlainEncryptionMode implements EncryptionMode {
    @Override
    public boolean box(ByteBuf opus, int start, ByteBuf output, byte[] secretKey) {
        opus.readerIndex(start);
        output.writeBytes(opus);
        return true;
    }

    @Override
    public String getName() {
        return "plain";
    }
}
