package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceConnection;

@FunctionalInterface
public interface VoiceGatewayConnectionFactory {
    VoiceGatewayConnection create(VoiceConnection connection);
}
