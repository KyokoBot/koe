package moe.kyokobot.koe.gateway;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface VoiceGatewayConnection extends Closeable {
    boolean isOpen();

    CompletableFuture<Void> start();

    @Override
    void close();
}
