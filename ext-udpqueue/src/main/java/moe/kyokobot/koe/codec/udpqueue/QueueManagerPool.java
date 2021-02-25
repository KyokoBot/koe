package moe.kyokobot.koe.codec.udpqueue;

import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static moe.kyokobot.koe.codec.OpusCodec.FRAME_DURATION;
import static moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory.MAXIMUM_PACKET_SIZE;

public class QueueManagerPool {
    private final AtomicLong queueKeySeq;
    private final UdpQueueManager[] managers;
    private boolean closed;

    public QueueManagerPool(int size, int bufferDuration) {
        if (size <= 0) {
            throw new IllegalArgumentException("Pool size must be higher or equal to 1.");
        }

        this.queueKeySeq = new AtomicLong();
        this.managers = new UdpQueueManager[size];
        for (int i = 0; i < size; i++) {
            UdpQueueManager queueManager = new UdpQueueManager(bufferDuration / FRAME_DURATION,
                    TimeUnit.MILLISECONDS.toNanos(FRAME_DURATION), MAXIMUM_PACKET_SIZE);
            this.managers[i] = queueManager;
            Thread thread = new Thread(queueManager::process, "QueueManagerPool-" + i);
            thread.setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void close() {
        if (closed || this.managers == null) {
            return;
        }

        this.closed = true;
        for (UdpQueueManager manager : this.managers) {
            if (manager != null) {
                manager.close();
            }
        }
    }

    public UdpQueueWrapper getNextWrapper() {
        long queueKey = this.queueKeySeq.getAndIncrement();
        return getWrapperForKey(queueKey);
    }

    public UdpQueueWrapper getWrapperForKey(long queueKey) {
        UdpQueueManager manager = this.managers[(int) (queueKey % (long) this.managers.length)];
        return new UdpQueueWrapper(manager, queueKey);
    }

    public static class UdpQueueWrapper {
        private final UdpQueueManager manager;
        private final long queueKey;

        public UdpQueueWrapper(UdpQueueManager manager, long queueKey) {
            this.manager = manager;
            this.queueKey = queueKey;
        }

        public boolean queuePacket(ByteBuffer packet, InetSocketAddress address) {
            return this.manager.queuePacket(this.queueKey, packet, address);
        }

        public int getRemainingCapacity() {
            return this.manager.getRemainingCapacity(this.queueKey);
        }

        public UdpQueueManager getManager() {
            return manager;
        }

        public long getQueueKey() {
            return queueKey;
        }
    }
}
