package moe.kyokobot.koe.codec.netty;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.FramePoller;
import moe.kyokobot.koe.codec.FramePollerFactory;
import moe.kyokobot.koe.codec.OpusCodec;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NettyFramePollerFactory implements FramePollerFactory {
    private Map<Codec, Function<VoiceConnection, FramePoller>> codecMap;

    public NettyFramePollerFactory() {
        codecMap = new HashMap<>();
        codecMap.put(OpusCodec.INSTANCE, NettyOpusFramePoller::new);
    }

    @Override
    @Nullable
    public FramePoller createFramePoller(Codec codec, VoiceConnection connection) {
        var constructor = codecMap.get(codec);
        if (constructor != null) {
            return constructor.apply(connection);
        }
        return null;
    }
}
