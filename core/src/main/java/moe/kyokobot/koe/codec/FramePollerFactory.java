package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.VoiceConnection;

@FunctionalInterface
public interface FramePollerFactory {
    FramePoller createFramePoller(Codec codec, VoiceConnection connection);
}
