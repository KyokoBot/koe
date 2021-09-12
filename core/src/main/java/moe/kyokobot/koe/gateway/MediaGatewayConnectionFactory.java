package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.MediaConnectionImpl;

@FunctionalInterface
public interface MediaGatewayConnectionFactory {
    MediaGatewayConnection create(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo);
}
