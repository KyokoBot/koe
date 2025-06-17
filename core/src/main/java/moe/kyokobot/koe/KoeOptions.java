package moe.kyokobot.koe;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import moe.kyokobot.koe.codec.FramePollerFactory;
import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * KoeOptions is a class that holds various options for configuring the Koe client.
 *
 * @see KoeOptionsBuilder The builder class for explanation of all options defined in this class.
 */
public class KoeOptions {
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends SocketChannel> socketChannelClass;
    private final Class<? extends DatagramChannel> datagramChannelClass;
    private final ByteBufAllocator byteBufAllocator;
    private final GatewayVersion gatewayVersion;
    private final FramePollerFactory framePollerFactory;
    private final boolean highPacketPriority;
    private final boolean deafened;
    private final boolean enableWSSPortOverride;

    KoeOptions(
            @NotNull EventLoopGroup eventLoopGroup,
            @NotNull Class<? extends SocketChannel> socketChannelClass,
            @NotNull Class<? extends DatagramChannel> datagramChannelClass,
            @NotNull ByteBufAllocator byteBufAllocator,
            @NotNull GatewayVersion gatewayVersion,
            @NotNull FramePollerFactory framePollerFactory,
            boolean highPacketPriority,
            boolean deafened,
            boolean enableWSSPortOverride
    ) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.socketChannelClass = Objects.requireNonNull(socketChannelClass);
        this.datagramChannelClass = Objects.requireNonNull(datagramChannelClass);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
        this.gatewayVersion = Objects.requireNonNull(gatewayVersion);
        this.framePollerFactory = Objects.requireNonNull(framePollerFactory);
        this.highPacketPriority = highPacketPriority;
        this.deafened = deafened;
        this.enableWSSPortOverride = enableWSSPortOverride;
    }

    /**
     * @deprecated Use {@link KoeOptionsBuilder} instead. Provided for binary compatibility with older versions.
     */
    @Deprecated(forRemoval = true, since = "2.2.0")
    public KoeOptions(
            @NotNull EventLoopGroup eventLoopGroup,
            @NotNull Class<? extends SocketChannel> socketChannelClass,
            @NotNull Class<? extends DatagramChannel> datagramChannelClass,
            @NotNull ByteBufAllocator byteBufAllocator,
            @NotNull GatewayVersion gatewayVersion,
            @NotNull FramePollerFactory framePollerFactory,
            boolean highPacketPriority,
            boolean deafened
    ) {
        this(eventLoopGroup, socketChannelClass, datagramChannelClass, byteBufAllocator, gatewayVersion,
                framePollerFactory, highPacketPriority, deafened, true);
    }

    /**
     * @deprecated Use {@link KoeOptionsBuilder} instead. Provided for binary compatibility with older versions.
     */
    @Deprecated(forRemoval = true, since = "2.2.0")
    public KoeOptions(
            @NotNull EventLoopGroup eventLoopGroup,
            @NotNull Class<? extends SocketChannel> socketChannelClass,
            @NotNull Class<? extends DatagramChannel> datagramChannelClass,
            @NotNull ByteBufAllocator byteBufAllocator,
            @NotNull GatewayVersion gatewayVersion,
            @NotNull FramePollerFactory framePollerFactory,
            boolean highPacketPriority
    ) {
        this(eventLoopGroup, socketChannelClass, datagramChannelClass, byteBufAllocator, gatewayVersion,
                framePollerFactory, highPacketPriority, false);
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

    public boolean isDeafened() {
        return deafened;
    }

    public boolean isEnableWSSPortOverride() {
        return enableWSSPortOverride;
    }

    /**
     * @return An instance of {@link KoeOptions} with default options.
     */
    @NotNull
    public static KoeOptions defaultOptions() {
        return new KoeOptionsBuilder().create();
    }

    public static KoeOptionsBuilder builder() {
        return new KoeOptionsBuilder();
    }
}
