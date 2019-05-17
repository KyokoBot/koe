package moe.kyokobot.koe;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeOptions {
    private final EventLoopGroup eventLoopGroup;
    private final ByteBufAllocator byteBufAllocator;
    private final GatewayVersion gatewayVersion;

    public KoeOptions(@NotNull EventLoopGroup eventLoopGroup,
                      @NotNull ByteBufAllocator byteBufAllocator,
                      @NotNull GatewayVersion gatewayVersion) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
        this.gatewayVersion = Objects.requireNonNull(gatewayVersion);
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
    public GatewayVersion getGatewayVersion() {
        return gatewayVersion;
    }

    @NotNull
    public static KoeOptions defaultOptions() {
        var eventLoop = Epoll.isAvailable()
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();
        var allocator = new PooledByteBufAllocator();
        var gatewayVersion = GatewayVersion.V4;

        return new KoeOptions(eventLoop, allocator, gatewayVersion);
    }
}
