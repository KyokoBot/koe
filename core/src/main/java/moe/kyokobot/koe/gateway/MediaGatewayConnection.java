package moe.kyokobot.koe.gateway;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface MediaGatewayConnection {
    long getPing();

    boolean isOpen();

    /**
     * @return The {@link MediaValve} used by this connection, or null if this gateway version does not support it.
     */
    @Nullable
    MediaValve getValve();

    CompletableFuture<Void> start();

    void close(int code, @Nullable String reason);

    void reconnect();

    void updateSpeaking(int mask);
}
