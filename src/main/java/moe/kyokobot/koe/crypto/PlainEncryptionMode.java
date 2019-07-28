package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;

public class PlainEncryptionMode implements EncryptionMode {
    @Override
    public void box(ByteBuf opus, ByteBuf output) {
        output.writeBytes(opus);
    }

    @Override
    public String getName() {
        return "plain";
    }
}
