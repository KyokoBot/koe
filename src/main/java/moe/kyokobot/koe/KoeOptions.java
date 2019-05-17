package moe.kyokobot.koe;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeOptions {
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends SocketChannel> socketChannelClass;
    private final ByteBufAllocator byteBufAllocator;
    private final GatewayVersion gatewayVersion;

    public KoeOptions(@NotNull EventLoopGroup eventLoopGroup,
                      @NotNull Class<? extends SocketChannel> socketChannelClass,
                      @NotNull ByteBufAllocator byteBufAllocator,
                      @NotNull GatewayVersion gatewayVersion) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.socketChannelClass = Objects.requireNonNull(socketChannelClass);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
        this.gatewayVersion = Objects.requireNonNull(gatewayVersion);
    }

    @NotNull
    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public Class<? extends SocketChannel> getSocketChannelClass() {
        return socketChannelClass;
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
        boolean epoll = Epoll.isAvailable();
        var eventLoop = epoll
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();
        var socketChannelClass = epoll
                ? EpollSocketChannel.class
                : NioSocketChannel.class;

        var allocator = new PooledByteBufAllocator();
        var gatewayVersion = GatewayVersion.V4;

        return new KoeOptions(eventLoop, socketChannelClass, allocator, gatewayVersion);
    }
}
