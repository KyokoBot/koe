package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.gateway.VoiceGatewayV4Connection;

public enum GatewayVersion {
    V3(null),
    V4(VoiceGatewayV4Connection::new);

    private final VoiceGatewayConnectionFactory factory;

    public VoiceGatewayConnection createConnection(VoiceConnection connection, VoiceServerInfo voiceServerInfo) {
        return factory.create(connection, voiceServerInfo);
    }

    GatewayVersion(VoiceGatewayConnectionFactory factory) {
        this.factory = factory;
    }
}
