package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.gateway.MediaGatewayConnectionFactory;
import moe.kyokobot.koe.internal.gateway.MediaGatewayV4Connection;
import moe.kyokobot.koe.internal.gateway.MediaGatewayV5Connection;
import moe.kyokobot.koe.internal.gateway.MediaGatewayV8Connection;

public enum GatewayVersion {
    V4(MediaGatewayV4Connection::new),
    V5(MediaGatewayV5Connection::new),
    V8(MediaGatewayV8Connection::new);

    private final MediaGatewayConnectionFactory factory;

    public MediaGatewayConnectionFactory getFactory() {
        return factory;
    }

    GatewayVersion(MediaGatewayConnectionFactory factory) {
        this.factory = factory;
    }
}
