package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.MediaConnection;

@FunctionalInterface
public interface FramePollerFactory {
    FramePoller createFramePoller(Codec codec, MediaConnection connection);
}
