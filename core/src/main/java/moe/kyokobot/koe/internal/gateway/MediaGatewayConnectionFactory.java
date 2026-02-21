package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.internal.MediaConnectionImpl;

@FunctionalInterface
public interface MediaGatewayConnectionFactory {
    MediaGatewayConnection create(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo);
}
