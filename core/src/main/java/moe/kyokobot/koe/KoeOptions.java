package moe.kyokobot.koe;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import moe.kyokobot.koe.codec.FramePollerFactory;
import moe.kyokobot.koe.codec.netty.NettyFramePollerFactory;
import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeOptions {
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends SocketChannel> socketChannelClass;
    private final Class<? extends DatagramChannel> datagramChannelClass;
    private final ByteBufAllocator byteBufAllocator;
    private final GatewayVersion gatewayVersion;
    private final FramePollerFactory framePollerFactory;
    private final boolean highPacketPriority;

    public KoeOptions(@NotNull EventLoopGroup eventLoopGroup,
                      @NotNull Class<? extends SocketChannel> socketChannelClass,
                      @NotNull Class<? extends DatagramChannel> datagramChannelClass,
                      @NotNull ByteBufAllocator byteBufAllocator,
                      @NotNull GatewayVersion gatewayVersion,
                      @NotNull FramePollerFactory framePollerFactory,
                      boolean highPacketPriority) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.socketChannelClass = Objects.requireNonNull(socketChannelClass);
        this.datagramChannelClass = Objects.requireNonNull(datagramChannelClass);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
        this.gatewayVersion = Objects.requireNonNull(gatewayVersion);
        this.framePollerFactory = Objects.requireNonNull(framePollerFactory);
        this.highPacketPriority = highPacketPriority;
    }

    @NotNull
    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @NotNull
    public Class<? extends SocketChannel> getSocketChannelClass() {
        return socketChannelClass;
    }

    @NotNull
    public Class<? extends DatagramChannel> getDatagramChannelClass() {
        return datagramChannelClass;
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
    public FramePollerFactory getFramePollerFactory() {
        return framePollerFactory;
    }

    public boolean isHighPacketPriority() {
        return highPacketPriority;
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

        var datagramChannelClass = epoll
                ? EpollDatagramChannel.class
                : NioDatagramChannel.class;

        var allocator = new PooledByteBufAllocator();
        var gatewayVersion = GatewayVersion.V4;
        var framePollerFactory = new NettyFramePollerFactory();

        return new KoeOptions(eventLoop, socketChannelClass, datagramChannelClass, allocator, gatewayVersion, framePollerFactory, true);
    }
}
