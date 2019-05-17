package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceConnection;

public class VoiceGatewayV4Connection implements VoiceGatewayConnection {
    private final VoiceConnection connection;

    public VoiceGatewayV4Connection(VoiceConnection connection) {
        this.connection = connection;
    }
}
