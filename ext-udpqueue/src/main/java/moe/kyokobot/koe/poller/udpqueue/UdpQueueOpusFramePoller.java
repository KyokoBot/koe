package moe.kyokobot.koe.poller.udpqueue;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.poller.AbstractOpusFramePoller;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

public class UdpQueueOpusFramePoller extends AbstractOpusFramePoller {
    private final QueueManagerPool.UdpQueueWrapper manager;

    public UdpQueueOpusFramePoller(QueueManagerPool.UdpQueueWrapper manager,
                                   @NotNull CodecInstance codec,
                                   @NotNull MediaConnection connection) {
        super(connection, codec);
        this.manager = manager;
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

        var packet = handler.createPacket(codec.getPayloadType(), timestamp, buf, len, false);
        if (packet == null) {
            return;
        }

        try {
            manager.queuePacket(packet.nioBuffer(), (InetSocketAddress) handler.getServerAddress());
        } finally {
            packet.release();
        }
    }
}
