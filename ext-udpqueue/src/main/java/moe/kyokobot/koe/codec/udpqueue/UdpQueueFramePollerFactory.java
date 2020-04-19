package moe.kyokobot.koe.codec.udpqueue;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.FramePoller;
import moe.kyokobot.koe.codec.FramePollerFactory;
import moe.kyokobot.koe.codec.OpusCodec;
import org.jetbrains.annotations.Nullable;

public class UdpQueueFramePollerFactory implements FramePollerFactory {
    public static final int DEFAULT_BUFFER_DURATION = 400;
    public static final int MAXIMUM_PACKET_SIZE = 4096;

    private final int bufferDuration;

    public UdpQueueFramePollerFactory() {
        this(DEFAULT_BUFFER_DURATION);
    }

    public UdpQueueFramePollerFactory(int bufferDuration) {
        this.bufferDuration = bufferDuration;
    }

    @Override
    @Nullable
    public FramePoller createFramePoller(Codec codec, VoiceConnection connection) {
        if (OpusCodec.INSTANCE.equals(codec)) {
            return new UdpQueueOpusFramePoller(bufferDuration, connection);
        }
        return null;
    }
}
