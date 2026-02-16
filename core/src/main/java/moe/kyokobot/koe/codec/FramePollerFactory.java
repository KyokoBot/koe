package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.MediaConnection;

@FunctionalInterface
public interface FramePollerFactory {
    FramePoller createFramePoller(CodecInstance codec, MediaConnection connection);
}
