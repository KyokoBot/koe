package moe.kyokobot.koe.codec;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import moe.kyokobot.koe.MediaConnection;

public abstract class AbstractFramePoller implements FramePoller {
    protected final MediaConnection connection;
    protected final ByteBufAllocator allocator;
    protected final EventLoopGroup eventLoop;
    protected boolean polling = false;

    public AbstractFramePoller(MediaConnection connection) {
        this.connection = connection;
        this.allocator = connection.getOptions().getByteBufAllocator();
        this.eventLoop = connection.getOptions().getEventLoopGroup();
    }

    @Override
    public boolean isPolling() {
        return polling;
    }
}
