package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class VoiceConnection implements Closeable {
    private final KoeClient client;
    private final long guildId;

    private VoiceServerInfo info;
    private volatile int ssrc;

    public VoiceConnection(@NotNull KoeClient client, long guildId) {
        this.client = Objects.requireNonNull(client);
        this.guildId = guildId;
    }

    public CompletionStage<Void> connect(VoiceServerInfo info) {
        var future = new CompletableFuture<Void>();

        this.info = info;

        return future;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getSsrc() {
        return ssrc;
    }

    @Nullable
    public VoiceServerInfo getVoiceServerInfo() {
        return info;
    }

    @Override
    public void close() {
        client.removeConnection(guildId);
    }
}
