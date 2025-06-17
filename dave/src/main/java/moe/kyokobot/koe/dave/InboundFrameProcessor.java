package moe.kyokobot.koe.dave;

import io.netty.buffer.ByteBuf;

public class InboundFrameProcessor {
    private boolean isEncrypted;
    private int originalSize;
    private int truncatedNonce;
    private ByteBuf buffer;
}
