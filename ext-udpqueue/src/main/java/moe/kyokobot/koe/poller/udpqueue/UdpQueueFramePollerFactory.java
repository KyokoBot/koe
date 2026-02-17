package moe.kyokobot.koe.poller.udpqueue;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.poller.AbstractFramePoller;
import moe.kyokobot.koe.poller.FramePollerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class UdpQueueFramePollerFactory implements FramePollerFactory {
    private final QueueManagerPool pool;

    public UdpQueueFramePollerFactory(@NotNull QueueManagerPool pool) {
        this.pool = Objects.requireNonNull(pool, "pool");
    }

    @Override
    @Nullable
    public AbstractFramePoller createFramePoller(CodecInstance codec, MediaConnection connection) {
        if (codec.getInfo() instanceof OpusCodecInfo) {
            return new UdpQueueOpusFramePoller(this.pool.getNextWrapper(), codec, connection);
        }
        return null;
    }
}
