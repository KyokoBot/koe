package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;

@FunctionalInterface
public interface VoiceGatewayConnectionFactory {
    VoiceGatewayConnection create(VoiceConnection connection, VoiceServerInfo voiceServerInfo);
}
