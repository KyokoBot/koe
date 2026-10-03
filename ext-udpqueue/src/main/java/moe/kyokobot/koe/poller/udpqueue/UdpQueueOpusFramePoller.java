package moe.kyokobot.koe.poller.udpqueue;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.poller.AbstractOpusFramePoller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

@ApiStatus.NonExtendable
public class UdpQueueOpusFramePoller extends AbstractOpusFramePoller {
    private final @Nullable QueueManagerPool pool;
    private QueueManagerPool.UdpQueueWrapper manager;
    private InetSocketAddress lastAddress;

    UdpQueueOpusFramePoller(@NotNull QueueManagerPool pool,
                            @NotNull CodecInstance codec,
                            @NotNull MediaConnection connection) {
        super(connection, codec);
        this.pool = pool;
        this.manager = pool.getNextWrapper();
    }

    @Override
    protected int getPollsPerTick() {
        return manager == null ? 0 : manager.getRemainingCapacity();
    }

    @Override
    protected boolean canSendFrame() {
        return manager != null && connection.getConnectionHandler() instanceof DiscordUDPConnection;
    }

    @Override
    protected void sendFramePayload(ByteBuf buf, int len, int timestamp) {
        var connectionHandler = connection.getConnectionHandler();
        if (!(connectionHandler instanceof DiscordUDPConnection)) {
            return;
        }
        var handler = (DiscordUDPConnection) connectionHandler;

        var packet = handler.createPacket(codec.getType(), codec.getPayloadType(), timestamp, buf, len, false);
        if (packet == null) {
            return;
        }

        try {
            var address = (InetSocketAddress) handler.getServerAddress();
            if (pool != null && lastAddress != null && !lastAddress.equals(address)) {
                manager = pool.getNextWrapper();
            }
            lastAddress = address;

            manager.queuePacket(packet.nioBuffer(), address);
        } finally {
            packet.release();
        }
    }
}
