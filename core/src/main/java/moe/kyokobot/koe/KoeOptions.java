package moe.kyokobot.koe;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import moe.kyokobot.koe.codec.CodecRegistry;
import moe.kyokobot.koe.gateway.GatewayVersion;
import moe.kyokobot.koe.poller.FramePollerFactory;
import org.jetbrains.annotations.NotNull;

public interface KoeOptions {
    /**
     * Creates a new {@link KoeOptionsBuilder} instance with default options.
     *
     * @return A new {@link KoeOptionsBuilder} instance.
     */
    static KoeOptionsBuilder builder() {
        return new KoeOptionsBuilder();
    }

    @NotNull EventLoopGroup getEventLoopGroup();

    @NotNull Class<? extends SocketChannel> getSocketChannelClass();

    @NotNull Class<? extends DatagramChannel> getDatagramChannelClass();

    @NotNull ByteBufAllocator getByteBufAllocator();

    @NotNull GatewayVersion getGatewayVersion();

    @NotNull FramePollerFactory getFramePollerFactory();

    @NotNull CodecRegistry getCodecRegistry();

    boolean isExperimental();

    boolean isHighPacketPriority();

    boolean isDeafened();

    boolean isEnableWSSPortOverride();

    boolean isEnableDAVE();
}
