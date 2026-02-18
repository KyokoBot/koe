package moe.kyokobot.koe.internal;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import moe.kyokobot.koe.KoeOptionsBuilder;
import moe.kyokobot.koe.codec.CodecRegistry;
import moe.kyokobot.koe.experimental.KoeOptionsExperimental;
import moe.kyokobot.koe.gateway.GatewayVersion;
import moe.kyokobot.koe.poller.FramePollerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * KoeOptions is a class that holds various options for configuring the Koe client.
 *
 * @see KoeOptionsBuilder The builder class for explanation of all options defined in this class.
 */
public class KoeOptionsImpl implements KoeOptionsExperimental {
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends SocketChannel> socketChannelClass;
    private final Class<? extends DatagramChannel> datagramChannelClass;
    private final ByteBufAllocator byteBufAllocator;
    private final GatewayVersion gatewayVersion;
    private final FramePollerFactory framePollerFactory;
    private final CodecRegistry codecRegistry;
    private final boolean experimental;
    private final boolean highPacketPriority;
    private final boolean deafened;
    private final boolean enableWSSPortOverride;
    private final boolean enableDAVE;

    public KoeOptionsImpl(
            @NotNull EventLoopGroup eventLoopGroup,
            @NotNull Class<? extends SocketChannel> socketChannelClass,
            @NotNull Class<? extends DatagramChannel> datagramChannelClass,
            @NotNull ByteBufAllocator byteBufAllocator,
            @NotNull GatewayVersion gatewayVersion,
            @NotNull FramePollerFactory framePollerFactory,
            @NotNull CodecRegistry codecRegistry,
            boolean experimental,
            boolean highPacketPriority,
            boolean deafened,
            boolean enableWSSPortOverride,
            boolean daveEnabled
    ) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.socketChannelClass = Objects.requireNonNull(socketChannelClass);
        this.datagramChannelClass = Objects.requireNonNull(datagramChannelClass);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
        this.gatewayVersion = Objects.requireNonNull(gatewayVersion);
        this.framePollerFactory = Objects.requireNonNull(framePollerFactory);
        this.codecRegistry = Objects.requireNonNull(codecRegistry);
        this.experimental = experimental;
        this.highPacketPriority = highPacketPriority;
        this.deafened = deafened;
        this.enableWSSPortOverride = enableWSSPortOverride;
        this.enableDAVE = daveEnabled;
    }

    @NotNull
    @Override
    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @NotNull
    @Override
    public Class<? extends SocketChannel> getSocketChannelClass() {
        return socketChannelClass;
    }

    @NotNull
    @Override
    public Class<? extends DatagramChannel> getDatagramChannelClass() {
        return datagramChannelClass;
    }

    @NotNull
    @Override
    public ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    @NotNull
    @Override
    public GatewayVersion getGatewayVersion() {
        return gatewayVersion;
    }

    @NotNull
    @Override
    public FramePollerFactory getFramePollerFactory() {
        return framePollerFactory;
    }

    @NotNull
    @Override
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    @Override
    public boolean isExperimental() {
        return experimental;
    }

    @Override
    public boolean isHighPacketPriority() {
        return highPacketPriority;
    }

    @Override
    public boolean isDeafened() {
        return deafened;
    }

    @Override
    public boolean isEnableWSSPortOverride() {
        return enableWSSPortOverride;
    }

    @Override
    public boolean isEnableDAVE() {
        return enableDAVE;
    }

}
