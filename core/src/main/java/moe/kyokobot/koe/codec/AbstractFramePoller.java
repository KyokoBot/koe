package moe.kyokobot.koe.codec;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import moe.kyokobot.koe.VoiceConnection;

public abstract class AbstractFramePoller implements FramePoller {
    protected final VoiceConnection connection;
    protected final ByteBufAllocator allocator;
    protected final EventLoopGroup eventLoop;
    protected volatile boolean polling = false;

    public AbstractFramePoller(VoiceConnection connection) {
        this.connection = connection;
        this.allocator = connection.getOptions().getByteBufAllocator();
        this.eventLoop = connection.getOptions().getEventLoopGroup();
    }

    @Override
    public boolean isPolling() {
        return polling;
    }
}
