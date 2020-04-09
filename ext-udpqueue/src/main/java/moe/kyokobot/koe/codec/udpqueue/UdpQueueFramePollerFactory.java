package moe.kyokobot.koe.codec.udpqueue;

import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.FramePoller;
import moe.kyokobot.koe.codec.FramePollerFactory;
import moe.kyokobot.koe.codec.OpusCodec;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UdpQueueFramePollerFactory implements FramePollerFactory {
    private static final int DEFAULT_BUFFER_DURATION = 400;
    private static final int PACKET_INTERVAL = 20;
    private static final int MAXIMUM_PACKET_SIZE = 4096;

    private final int bufferDuration;
    private final AtomicLong identifierCounter = new AtomicLong();
    private final Object lock = new Object();
    private final KeySetView<UdpQueueOpusFramePoller, Boolean> pollers = ConcurrentHashMap.newKeySet();
    private UdpQueueManager queueManager;
    private ScheduledExecutorService scheduler;

    public UdpQueueFramePollerFactory() {
        this(DEFAULT_BUFFER_DURATION);
    }

    public UdpQueueFramePollerFactory(int bufferDuration) {
        this.bufferDuration = bufferDuration;
    }

    private void initialiseQueueManager() {
        scheduler = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory("native-udp"));
        queueManager = new UdpQueueManager(bufferDuration / PACKET_INTERVAL,
                TimeUnit.MILLISECONDS.toNanos(PACKET_INTERVAL), MAXIMUM_PACKET_SIZE);

        scheduler.scheduleWithFixedDelay(this::populateQueues, 0, 40, TimeUnit.MILLISECONDS);

        Thread thread = new Thread(process(queueManager));
        thread.setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);
        thread.setDaemon(true);
        thread.start();
    }

    private ScheduledExecutorService shutdownQueueManager() {
        queueManager.close();
        queueManager = null;

        ScheduledExecutorService currentScheduler = scheduler;
        scheduler = null;
        return currentScheduler;
    }

    void addInstance(UdpQueueOpusFramePoller poller) {
        synchronized (lock) {
            pollers.add(poller);

            if (queueManager == null) {
                initialiseQueueManager();
            }
        }
    }

    void removeInstance(UdpQueueOpusFramePoller poller) {
        ScheduledExecutorService schedulerToShutDown = null;

        synchronized (lock) {
            if (pollers.remove(poller) && pollers.isEmpty() && queueManager != null) {
                schedulerToShutDown = shutdownQueueManager();
            }
        }

        if (schedulerToShutDown != null) {
            ExecutorTools.shutdownExecutor(schedulerToShutDown, "native udp queue populator");
        }
    }

    private void populateQueues() {
        UdpQueueManager manager = queueManager; /* avoid getfield opcode */

        if (manager != null) {
            for (var system : pollers) {
                system.populateQueue(manager);
            }
        }
    }

    private static Runnable process(UdpQueueManager unbake) {
        return unbake::process;
    }

    @Override
    @Nullable
    public FramePoller createFramePoller(Codec codec, VoiceConnection connection) {
        if (OpusCodec.INSTANCE.equals(codec)) {
            return new UdpQueueOpusFramePoller(identifierCounter.incrementAndGet(), this, connection);
        }
        return null;
    }
}
