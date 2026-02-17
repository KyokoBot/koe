package moe.kyokobot.koe.poller;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;

@FunctionalInterface
public interface FramePollerFactory {
    AbstractFramePoller createFramePoller(CodecInstance codec, MediaConnection connection);
}
