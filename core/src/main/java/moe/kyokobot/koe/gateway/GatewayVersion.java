package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.VoiceConnectionImpl;

public enum GatewayVersion {
    V4(VoiceGatewayV4Connection::new),
    V5(VoiceGatewayV5Connection::new);

    private final VoiceGatewayConnectionFactory factory;

    public VoiceGatewayConnection createConnection(VoiceConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        return factory.create(connection, voiceServerInfo);
    }

    GatewayVersion(VoiceGatewayConnectionFactory factory) {
        this.factory = factory;
    }
}
