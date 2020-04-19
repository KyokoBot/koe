package moe.kyokobot.koe.codec.udpqueue;

import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.AbstractFramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static moe.kyokobot.koe.codec.OpusCodec.FRAME_DURATION;
import static moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory.MAXIMUM_PACKET_SIZE;

public class UdpQueueOpusFramePoller extends AbstractFramePoller {
    private final int bufferDuration;
    private UdpQueueManager queueManager;
    private AtomicInteger timestamp;
    private long lastFrame;
    private long queueKey;

    public UdpQueueOpusFramePoller(int bufferDuration, VoiceConnection connection) {
        super(connection);
        this.bufferDuration = bufferDuration;
        this.timestamp = new AtomicInteger();
        this.queueKey = 0;
    }

    @Override
    public void start() {
        if (this.polling) {
            throw new IllegalStateException("Polling already started!");
        }

        this.polling = true;
        this.lastFrame = System.currentTimeMillis();
        queueManager = new UdpQueueManager(bufferDuration / FRAME_DURATION,
                TimeUnit.MILLISECONDS.toNanos(FRAME_DURATION), MAXIMUM_PACKET_SIZE);

        Thread thread = new Thread(queueManager::process);
        thread.setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);
        thread.setDaemon(true);
        thread.start();
        eventLoop.execute(this::populateQueue);
    }

    @Override
    public void stop() {
        if (this.polling) {
            this.polling = false;
            queueManager.close();
            queueManager = null;
        }
    }

    void populateQueue() {
        if (!this.polling || queueManager == null) {
            return;
        }

        var manager = this.queueManager;
        int remaining = manager.getRemainingCapacity(queueKey);

        var handler = (DiscordUDPConnection) connection.getConnectionHandler();
        var sender = connection.getSender();
        var codec = OpusCodec.INSTANCE;

        for (int i = 0; i < remaining; i++) {
            if (sender != null && handler != null && sender.canSendFrame(codec)) {
                var buf = allocator.buffer();
                int start = buf.writerIndex();
                sender.retrieve(codec, buf);
                int len = buf.writerIndex() - start;
                var packet = handler.createPacket(OpusCodec.PAYLOAD_TYPE, timestamp.getAndAdd(960), buf, len);
                if (packet != null) {
                    manager.queuePacket(queueKey, packet.nioBuffer(), (InetSocketAddress) handler.getServerAddress());
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
