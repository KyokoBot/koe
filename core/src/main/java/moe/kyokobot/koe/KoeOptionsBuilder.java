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

public class KoeOptionsBuilder {
    private EventLoopGroup eventLoopGroup;
    private Class<? extends SocketChannel> socketChannelClass;
    private Class<? extends DatagramChannel> datagramChannelClass;
    private ByteBufAllocator byteBufAllocator;
    private GatewayVersion gatewayVersion;
    private FramePollerFactory framePollerFactory;
    private boolean highPacketPriority;

    KoeOptionsBuilder() {
        boolean epoll = Epoll.isAvailable();
        this.eventLoopGroup = epoll
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();
        this.socketChannelClass = epoll
                ? EpollSocketChannel.class
                : NioSocketChannel.class;

        this.datagramChannelClass = epoll
                ? EpollDatagramChannel.class
                : NioDatagramChannel.class;

        this.byteBufAllocator = new PooledByteBufAllocator();
        this.gatewayVersion = GatewayVersion.V8;
        this.framePollerFactory = new NettyFramePollerFactory();
        this.highPacketPriority = true;
    }

    public KoeOptionsBuilder setEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    public KoeOptionsBuilder setSocketChannelClass(Class<? extends SocketChannel> socketChannelClass) {
        this.socketChannelClass = socketChannelClass;
        return this;
    }

    public KoeOptionsBuilder setDatagramChannelClass(Class<? extends DatagramChannel> datagramChannelClass) {
        this.datagramChannelClass = datagramChannelClass;
        return this;
    }

    public KoeOptionsBuilder setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
        return this;
    }

    public KoeOptionsBuilder setGatewayVersion(GatewayVersion gatewayVersion) {
        this.gatewayVersion = gatewayVersion;
        return this;
    }

    public KoeOptionsBuilder setFramePollerFactory(FramePollerFactory framePollerFactory) {
        this.framePollerFactory = framePollerFactory;
        return this;
    }

    public KoeOptionsBuilder setHighPacketPriority(boolean highPacketPriority) {
        this.highPacketPriority = highPacketPriority;
        return this;
    }

    public KoeOptions create() {
        return new KoeOptions(eventLoopGroup, socketChannelClass, datagramChannelClass, byteBufAllocator, gatewayVersion, framePollerFactory, highPacketPriority);
    }
}
