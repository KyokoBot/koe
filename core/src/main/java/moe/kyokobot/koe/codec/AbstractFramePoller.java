package moe.kyokobot.koe.codec;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import moe.kyokobot.koe.MediaConnection;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFramePoller implements FramePoller {
    protected final MediaConnection connection;
    protected final CodecInstance codec;
    protected final ByteBufAllocator allocator;
    protected final EventLoopGroup eventLoop;
    protected boolean polling = false;

    public AbstractFramePoller(@NotNull CodecInstance codec, @NotNull MediaConnection connection) {
        this.codec = codec;
        this.connection = connection;
        this.allocator = connection.getOptions().getByteBufAllocator();
        this.eventLoop = connection.getOptions().getEventLoopGroup();
    }

    @Override
    public boolean isPolling() {
        return polling;
    }
}
