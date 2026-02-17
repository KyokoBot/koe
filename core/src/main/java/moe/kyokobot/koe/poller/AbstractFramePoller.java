package moe.kyokobot.koe.poller;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import moe.kyokobot.koe.MediaConnection;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractFramePoller implements AutoCloseable {
    protected final MediaConnection connection;
    protected final EventLoopGroup eventLoop;
    protected final ByteBufAllocator allocator;
    protected final AtomicBoolean polling = new AtomicBoolean(false);

    private volatile long lastFrameTime = 0L;
    private volatile long totalFrames = 0L;
    private volatile long droppedFrames = 0L;

    public AbstractFramePoller(@NotNull MediaConnection connection) {
        this.connection = connection;
        this.allocator = connection.getOptions().getByteBufAllocator();
        this.eventLoop = connection.getOptions().getEventLoopGroup();
    }

    public boolean isPolling() {
        return polling.get();
    }

    public void start() {
        if (!polling.compareAndSet(false, true)) {
            throw new IllegalStateException("Polling already started!");
        }
        lastFrameTime = System.nanoTime();
        scheduleNext();
    }

    public void stop() {
        polling.set(false);
    }

    protected abstract long getFrameIntervalNanos();

    protected abstract boolean pollAndSend();

    // Thread-safety note: poll() is always scheduled from within a running poll() (or from start() exactly once),
    // and all scheduling targets the same single-threaded Netty EventLoop. This means poll() executes strictly
    // sequentially — there is never more than one poll() task enqueued at a time, so no concurrent execution
    // is possible and no additional locking is needed here.
    private void scheduleNext() {
        if (!polling.get()) {
            return;
        }

        long now = System.nanoTime();
        long elapsed = now - lastFrameTime;
        long frameInterval = getFrameIntervalNanos();
        long delay = frameInterval - elapsed;

        if (delay < 0L) {
            long missedFrames = -(delay / frameInterval);
            if (missedFrames > 3L) {
                lastFrameTime = now;
            } else {
                lastFrameTime = safeAdd(lastFrameTime, frameInterval);
            }
            delay = 0L;
            droppedFrames = saturatingAdd(droppedFrames, missedFrames);
        } else {
            lastFrameTime = safeAdd(lastFrameTime, frameInterval);
        }

        if (delay > 0L) {
            eventLoop.schedule(this::poll, delay, TimeUnit.NANOSECONDS);
        } else {
            eventLoop.execute(this::poll);
        }
    }

    private void poll() {
        if (!polling.get()) {
            return;
        }

        try {
            boolean sent = pollAndSend();
            if (sent) {
                totalFrames = saturatingAdd(totalFrames, 1L);
            }
        } catch (Exception ignored) {
            // Keep polling even when transport/provider raises.
        }

        scheduleNext();
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    private static long safeAdd(long left, long right) {
        return saturatingAdd(left, right);
    }

    public long getTotalFrames() {
        return totalFrames;
    }

    public long getDroppedFrames() {
        return droppedFrames;
    }

    @Override
    public void close() {
        // Optional override by subclasses.
    }
}
