package moe.kyokobot.koe.poller.netty;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.H264CodecInfo;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.poller.AbstractFramePoller;
import moe.kyokobot.koe.poller.FramePollerFactory;
import org.jetbrains.annotations.Nullable;

public class NettyFramePollerFactory implements FramePollerFactory {
    @Override
    @Nullable
    public AbstractFramePoller createFramePoller(CodecInstance codec, MediaConnection connection) {
        if (codec.getInfo() instanceof OpusCodecInfo) {
            return new NettyOpusFramePoller(codec, connection);
        }
        return null;
    }
}
