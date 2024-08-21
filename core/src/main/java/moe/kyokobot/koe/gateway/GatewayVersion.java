package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.MediaConnectionImpl;

public enum GatewayVersion {
    V4(MediaGatewayV4Connection::new),
    V5(MediaGatewayV5Connection::new),
    V8(MediaGatewayV8Connection::new);

    private final MediaGatewayConnectionFactory factory;

    public MediaGatewayConnection createConnection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        return factory.create(connection, voiceServerInfo);
    }

    GatewayVersion(MediaGatewayConnectionFactory factory) {
        this.factory = factory;
    }
}
