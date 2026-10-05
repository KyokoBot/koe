package moe.kyokobot.koe.internal;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import moe.kyokobot.koe.KoeOptionsBuilder;
import moe.kyokobot.koe.codec.CodecRegistry;
import moe.kyokobot.koe.experimental.KoeOptionsExperimental;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;
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
    private final boolean verifyWSSHostname;
    private final long gatewayConnectTimeout;
    private final boolean sendSpeakingStop;
    private final boolean enableDAVE;
    private final boolean enableDAVELogSink;
    private final CipherPreferencePolicy cipherPreferencePolicy;

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
            boolean verifyWSSHostname,
            long gatewayConnectTimeout,
            boolean sendSpeakingStop,
            boolean enableDAVE,
            boolean enableDAVELogSink,
            @NotNull CipherPreferencePolicy cipherPreferencePolicy
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
        this.verifyWSSHostname = verifyWSSHostname;
        this.gatewayConnectTimeout = gatewayConnectTimeout;
        this.sendSpeakingStop = sendSpeakingStop;
        this.enableDAVE = enableDAVE;
        this.enableDAVELogSink = enableDAVELogSink;
        this.cipherPreferencePolicy = Objects.requireNonNull(cipherPreferencePolicy);
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
    public boolean isVerifyWSSHostname() {
        return verifyWSSHostname;
    }

    @Override
    public long getGatewayConnectTimeout() {
        return gatewayConnectTimeout;
    }

    @Override
    public boolean isSendSpeakingStop() {
        return sendSpeakingStop;
    }

    @Override
    public boolean isEnableDAVE() {
        return enableDAVE;
    }

    @Override
    public boolean isEnableDAVELogSink() {
        return enableDAVELogSink;
    }

    @Override
    @NotNull
    public CipherPreferencePolicy getCipherPreferencePolicy() {
        return cipherPreferencePolicy;
    }
}
