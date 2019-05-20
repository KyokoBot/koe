package moe.kyokobot.koe;

import moe.kyokobot.koe.gateway.VoiceGatewayConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class VoiceConnection implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(VoiceConnection.class);

    private final KoeClient client;
    private final long guildId;

    private VoiceGatewayConnection gatewayConnection;
    private VoiceServerInfo info;

    public VoiceConnection(@NotNull KoeClient client, long guildId) {
        this.client = Objects.requireNonNull(client);
        this.guildId = guildId;
    }

    public CompletionStage<Void> connect(VoiceServerInfo info) {
        disconnect();
        var conn = client.getGatewayVersion().createConnection(this, info);

        return conn.start().thenAccept(nothing -> {
            VoiceConnection.this.info = info;
            VoiceConnection.this.gatewayConnection = conn;
        });
    }

    public void disconnect() {
        if (gatewayConnection != null && gatewayConnection.isOpen()) {
            gatewayConnection.close();
            gatewayConnection = null;
        }
    }

    @NotNull
    public KoeClient getClient() {
        return client;
    }

    @NotNull
    public KoeOptions getOptions() {
        return client.getOptions();
    }

    public long getGuildId() {
        return guildId;
    }

    @Nullable
    public VoiceGatewayConnection getGatewayConnection() {
        return gatewayConnection;
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
