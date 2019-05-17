package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceConnection;

public enum GatewayVersion {
    V3(null),
    V4(null);

    private final VoiceGatewayConnectionFactory factory;

    public VoiceGatewayConnection createConnection(VoiceConnection connection) {
        return factory.create(connection);
    }

    GatewayVersion(VoiceGatewayConnectionFactory factory) {
        this.factory = factory;
    }
}
