package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceConnection;

public class VoiceGatewayV4Connection implements VoiceGatewayConnection {
    private final VoiceConnection connection;

    private volatile boolean open;

    public VoiceGatewayV4Connection(VoiceConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {

    }
}
