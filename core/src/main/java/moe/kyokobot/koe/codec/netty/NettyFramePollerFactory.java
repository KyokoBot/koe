package moe.kyokobot.koe.codec.netty;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NettyFramePollerFactory implements FramePollerFactory {
    private final Map<Codec, Function<MediaConnection, FramePoller>> codecMap;

    public NettyFramePollerFactory() {
        codecMap = new HashMap<>();
        codecMap.put(OpusCodec.INSTANCE, NettyOpusFramePoller::new);
        codecMap.put(H264Codec.INSTANCE, NettyH264FramePoller::new);
    }

    @Override
    @Nullable
    public FramePoller createFramePoller(Codec codec, MediaConnection connection) {
        var constructor = codecMap.get(codec);
        if (constructor != null) {
            return constructor.apply(connection);
        }
        return null;
    }
}
