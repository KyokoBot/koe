package moe.kyokobot.koe.gateway;

import java.io.Closeable;

public interface VoiceGatewayConnection extends Closeable {
    boolean isOpen();

    @Override
    void close();
}
