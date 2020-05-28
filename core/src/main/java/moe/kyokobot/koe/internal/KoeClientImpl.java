package moe.kyokobot.koe.internal;

import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KoeClientImpl implements KoeClient {
    private final long clientId;
    private final KoeOptions options;
    private final Map<Long, MediaConnection> connections;

    public KoeClientImpl(long clientId, KoeOptions options) {
        this.clientId = clientId;
        this.options = options;

        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    @NotNull
    public MediaConnection createConnection(long guildId) {
        return connections.computeIfAbsent(guildId, this::newVoiceConnection);
    }

    @Override
    @Nullable
    public MediaConnection getConnection(long guildId) {
        return connections.get(guildId);
    }

    @Override
    public void destroyConnection(long guildId) {
        var connection = connections.remove(guildId);

        if (connection != null) {
            connection.close();
        }
    }

    void removeConnection(long guildId) {
        connections.remove(guildId);
    }

    @Override
    @NotNull
    public Map<Long, MediaConnection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    @Override
    public void close() {
        if (!connections.isEmpty()) {
            var guilds = List.copyOf(connections.keySet());
            guilds.forEach(this::destroyConnection);
        }
    }

    public MediaConnection newVoiceConnection(long id) {
        return new MediaConnectionImpl(this, id);
    }

    @Override
    public long getClientId() {
        return clientId;
    }

    @Override
    @NotNull
    public KoeOptions getOptions() {
        return options;
    }

    @Override
    @NotNull
    public GatewayVersion getGatewayVersion() {
        return options.getGatewayVersion();
    }
}
