package moe.kyokobot.koe.codec.udpqueue;

import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.AbstractFramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class UdpQueueOpusFramePoller extends AbstractFramePoller {
    private static final Logger logger = LoggerFactory.getLogger(UdpQueueOpusFramePoller.class);

    private final long queueKey;
    private final UdpQueueFramePollerFactory factory;
    private AtomicInteger timestamp;

    public UdpQueueOpusFramePoller(long queueKey, UdpQueueFramePollerFactory factory, VoiceConnection connection) {
        super(connection);
        this.queueKey = queueKey;
        this.factory = factory;
        this.timestamp = new AtomicInteger();
    }

    @Override
    public void start() {
        if (this.polling) {
            throw new IllegalStateException("Polling already started!");
        }

        factory.addInstance(this);
        this.polling = true;
    }

    @Override
    public void stop() {
        if (this.polling) {
            factory.removeInstance(this);
        }
        this.polling = false;
    }

    void populateQueue(UdpQueueManager queueManager) {
        int remaining = queueManager.getRemainingCapacity(queueKey);
        //boolean emptyQueue = queueManager.getCapacity() - remaining > 0;

        var handler = (DiscordUDPConnection) connection.getConnectionHandler();
        var sender = connection.getSender();

        for (int i = 0; i < remaining; i++) {
            if (sender != null && handler != null && sender.canSendFrame()) {
                var buf = allocator.buffer();
                int start = buf.writerIndex();
                sender.retrieve(OpusCodec.INSTANCE, buf);
                int len = buf.writerIndex() - start;
                //handler.sendFrame(OpusCodec.PAYLOAD_TYPE, timestamp.getAndAdd(960), buf, len);
                var packet = handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.getAndAdd(960), buf, len);
                if (packet != null) {
                    queueManager.queuePacket(queueKey, packet.nioBuffer(),
                            (InetSocketAddress) handler.getServerAddress());
                }
                buf.release();
            }
        }
    }
}
