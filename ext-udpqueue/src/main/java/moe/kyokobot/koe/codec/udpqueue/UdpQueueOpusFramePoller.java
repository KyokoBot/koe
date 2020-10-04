package moe.kyokobot.koe.codec.udpqueue;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.AbstractFramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.media.IntReference;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class UdpQueueOpusFramePoller extends AbstractFramePoller {
    private final QueueManagerPool.UdpQueueWrapper manager;
    private final IntReference timestamp = new IntReference();
    private long lastFrame;

    public UdpQueueOpusFramePoller(QueueManagerPool.UdpQueueWrapper manager, MediaConnection connection) {
        super(connection);
        this.manager = manager;
    }

    @Override
    public void start() {
        if (this.polling) {
            throw new IllegalStateException("Polling already started!");
        }

        this.polling = true;
        this.lastFrame = System.currentTimeMillis();
        eventLoop.execute(this::populateQueue);
    }

    @Override
    public void stop() {
        if (this.polling) {
            this.polling = false;
        }
    }

    void populateQueue() {
        if (!this.polling || manager == null) {
            return;
        }

        int remaining = manager.getRemainingCapacity();

        var handler = (DiscordUDPConnection) connection.getConnectionHandler();
        var sender = connection.getAudioSender();
        var codec = OpusCodec.INSTANCE;

        for (int i = 0; i < remaining; i++) {
            if (sender != null && handler != null && sender.canSendFrame(codec)) {
                var buf = allocator.buffer();
                int start = buf.writerIndex();
                sender.retrieve(codec, buf, timestamp);
                int len = buf.writerIndex() - start;
                var packet = handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.get(), buf, len, false);
                if (packet != null) {
                    manager.queuePacket(packet.nioBuffer(), (InetSocketAddress) handler.getServerAddress());
                    packet.release();
                }
                buf.release();
            }
        }

        long frameDelay = 40 - (System.currentTimeMillis() - lastFrame);

        if (frameDelay > 0) {
            eventLoop.schedule(this::loop, frameDelay, TimeUnit.MILLISECONDS);
        } else {
            loop();
        }
    }

    private void loop() {
        if (System.currentTimeMillis() < lastFrame + 60) {
            lastFrame += 40;
        } else {
            lastFrame = System.currentTimeMillis();
        }

        populateQueue();
    }
}
