package moe.kyokobot.koe;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeOptions {
    private final EventLoopGroup eventLoopGroup;
    private final ByteBufAllocator byteBufAllocator;

    public KoeOptions(@NotNull EventLoopGroup eventLoopGroup,
                      @NotNull ByteBufAllocator byteBufAllocator) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
    }

    @NotNull
    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @NotNull
    public ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    @NotNull
    public static KoeOptions defaultOptions() {
        var eventLoop = Epoll.isAvailable()
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();

        var allocator = new PooledByteBufAllocator();

        return new KoeOptions(eventLoop, allocator);
    }
}
