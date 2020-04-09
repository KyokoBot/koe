package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.VoiceConnectionImpl;

@FunctionalInterface
public interface VoiceGatewayConnectionFactory {
    VoiceGatewayConnection create(VoiceConnectionImpl connection, VoiceServerInfo voiceServerInfo);
}
