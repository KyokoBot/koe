package moe.kyokobot.koe.codec.netty;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class NettyFramePollerFactory implements FramePollerFactory {
    private final Map<String, BiFunction<CodecInstance, MediaConnection, FramePoller>> codecMap;

    public NettyFramePollerFactory() {
        codecMap = new HashMap<>();
        codecMap.put("opus", NettyOpusFramePoller::new);
        codecMap.put("H264", NettyH264FramePoller::new);
    }

    @Override
    @Nullable
    public FramePoller createFramePoller(CodecInstance codec, MediaConnection connection) {
        var constructor = codecMap.get(codec.getName());
        if (constructor != null) {
            return constructor.apply(codec, connection);
        }
        return null;
    }
}
