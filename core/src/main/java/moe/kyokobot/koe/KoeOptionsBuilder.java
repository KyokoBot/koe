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
import moe.kyokobot.koe.codec.CodecRegistry;
import moe.kyokobot.koe.codec.DefaultCodecRegistry;
import moe.kyokobot.koe.gateway.GatewayVersion;
import moe.kyokobot.koe.internal.KoeOptionsImpl;
import moe.kyokobot.koe.poller.FramePollerFactory;
import moe.kyokobot.koe.poller.netty.NettyFramePollerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeOptionsBuilder {
    protected EventLoopGroup eventLoopGroup;
    protected Class<? extends SocketChannel> socketChannelClass;
    protected Class<? extends DatagramChannel> datagramChannelClass;
    protected ByteBufAllocator byteBufAllocator;
    protected GatewayVersion gatewayVersion;
    protected FramePollerFactory framePollerFactory;
    protected CodecRegistry codecRegistry;
    protected boolean experimental;
    protected boolean highPacketPriority;
    protected boolean deafened;
    protected boolean enableWSSPortOverride;
    protected boolean verifyWSSHostname;
    protected boolean enableDAVE;

    protected KoeOptionsBuilder() {
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
        this.codecRegistry = new DefaultCodecRegistry();
        this.experimental = false;
        this.highPacketPriority = true;
        this.deafened = false;
        this.enableWSSPortOverride = false;
        this.verifyWSSHostname = true;
        this.enableDAVE = true;
    }

    /**
     * Sets an implementation of Netty {@link EventLoopGroup} to use for the Koe client.
     * Defaults to Epoll if available (Linux) or NIO on everything else..
     * Use this if you need KQueue on macOS/BSD hosts or other native transport with Koe.
     *
     * @param eventLoopGroup An instance of {@link EventLoopGroup} to use for the Koe client.
     */
    public KoeOptionsBuilder setEventLoopGroup(@NotNull EventLoopGroup eventLoopGroup) {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    /**
     * Sets a class of Netty {@link SocketChannel} to use for the Koe client.
     * Defaults to {@link EpollSocketChannel} if Epoll is available (Linux) or {@link NioSocketChannel} on everything else.
     * Use this if you need KQueue on macOS/BSD hosts or other native transport with Koe.
     *
     * @param socketChannelClass An implementation class of {@link SocketChannel} to use for the Koe client.
     *                           Same as class passed into {@link io.netty.bootstrap.Bootstrap#channel(Class)}
     *                           method when creating socket channels.
     */
    public KoeOptionsBuilder setSocketChannelClass(@NotNull Class<? extends SocketChannel> socketChannelClass) {
        Objects.requireNonNull(socketChannelClass, "socketChannelClass cannot be null");
        this.socketChannelClass = socketChannelClass;
        return this;
    }

    /**
     * Sets a class of Netty {@link DatagramChannel} to use for the Koe client.
     * Defaults to {@link EpollDatagramChannel} if Epoll is available (Linux) or {@link NioDatagramChannel} on everything else.
     * Use this if you need KQueue on macOS/BSD hosts or other native transport with Koe.
     *
     * @param datagramChannelClass An implementation class of {@link DatagramChannel} to use for the Koe client.
     *                             Same as class passed into {@link io.netty.bootstrap.Bootstrap#channel(Class)}
     *                             method when creating datagram channels.
     */
    public KoeOptionsBuilder setDatagramChannelClass(@NotNull Class<? extends DatagramChannel> datagramChannelClass) {
        Objects.requireNonNull(datagramChannelClass, "datagramChannelClass cannot be null");
        this.datagramChannelClass = datagramChannelClass;
        return this;
    }

    /**
     * Sets a Netty {@link ByteBufAllocator} to use for the Koe client.
     * Defaults to {@link PooledByteBufAllocator}.
     *
     * @param byteBufAllocator An instance of {@link ByteBufAllocator} to use for the Koe client.
     */
    public KoeOptionsBuilder setByteBufAllocator(@NotNull ByteBufAllocator byteBufAllocator) {
        Objects.requireNonNull(byteBufAllocator, "byteBufAllocator cannot be null");
        this.byteBufAllocator = byteBufAllocator;
        return this;
    }

    /**
     * Sets the gateway version to use for the Koe client.
     *
     * @param gatewayVersion An instance of {@link GatewayVersion} to use for the Koe client.
     */
    public KoeOptionsBuilder setGatewayVersion(@NotNull GatewayVersion gatewayVersion) {
        Objects.requireNonNull(gatewayVersion, "gatewayVersion cannot be null");
        this.gatewayVersion = gatewayVersion;
        return this;
    }

    /**
     * Sets a factory for creating frame pollers.
     * Defaults to {@link NettyFramePollerFactory}.
     *
     * @param framePollerFactory An instance of {@link FramePollerFactory} to use for the Koe client.
     */
    public KoeOptionsBuilder setFramePollerFactory(@NotNull FramePollerFactory framePollerFactory) {
        Objects.requireNonNull(framePollerFactory, "framePollerFactory cannot be null");
        this.framePollerFactory = framePollerFactory;
        return this;
    }

    /**
     * Sets a custom codec registry.
     * Useful for registering custom codecs or removing built-in ones.
     * Defaults to {@link DefaultCodecRegistry} with built-in codecs.
     *
     * @param codecRegistry An instance of {@link CodecRegistry} to use for the Koe client.
     */
    public KoeOptionsBuilder setCodecRegistry(@NotNull CodecRegistry codecRegistry) {
        Objects.requireNonNull(codecRegistry, "codecRegistry cannot be null");
        this.codecRegistry = codecRegistry;
        return this;
    }

    /**
     * Sets whether to set IP_TOS flags to request high priority/low-delay for sent RTP packets.
     * Defaults to true.
     */
    public KoeOptionsBuilder setHighPacketPriority(boolean highPacketPriority) {
        this.highPacketPriority = highPacketPriority;
        return this;
    }

    /**
     * Sets whether the client should be deafened by default.
     * Defaults to false.
     */
    public void setDeafened(boolean deafened) {
        this.deafened = deafened;
    }

    /**
     * Sets whether to enable port :80 -> :443 override for voice server endpoint passed in {@link VoiceServerInfo}
     * Defaults to true.
     */
    public KoeOptionsBuilder setEnableWSSPortOverride(boolean enableWSSPortOverride) {
        this.enableWSSPortOverride = enableWSSPortOverride;
        return this;
    }

    /**
     * Sets whether to verify the server certificate hostname for WSS voice gateway connections.
     * Defaults to true. Set to false only for custom or self-signed voice endpoints.
     *
     * @param verifyWSSHostname true to verify hostname (default), false to disable verification
     */
    public KoeOptionsBuilder setVerifyWSSHostname(boolean verifyWSSHostname) {
        this.verifyWSSHostname = verifyWSSHostname;
        return this;
    }

    /**
     * Sets whether End-to-End encryption using Discord's <a href="https://daveprotocol.com">DAVE protocol</a> is enabled.
     * Defaults to true.
     */
    public KoeOptionsBuilder setDAVEEnabled(boolean enabled) {
        this.enableDAVE = enabled;
        return this;
    }

    public KoeOptions create() {
        return new KoeOptionsImpl(eventLoopGroup, socketChannelClass, datagramChannelClass, byteBufAllocator,
                gatewayVersion, framePollerFactory, codecRegistry, experimental,
                highPacketPriority, deafened, enableWSSPortOverride, verifyWSSHostname, enableDAVE);
    }
}
