package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KoeClient implements Closeable {
    private final long clientId;
    private final Map<Long, VoiceConnection> connections;

    public KoeClient(long clientId) {
        this.clientId = clientId;
        this.connections = new ConcurrentHashMap<>();
    }

    public long getClientId() {
        return clientId;
    }

    @NotNull
    public VoiceConnection createConnection(long guildId) {
        return connections.computeIfAbsent(guildId, this::createVoiceConnection);
    }

    @Nullable
    public VoiceConnection getConnection(long guildId) {
        return connections.get(guildId);
    }

    public void destroyConnection(long guildId) {
        var connection = connections.remove(guildId);

        if (connection != null) {
            connection.close();
        }
    }

    void removeConnection(long guildId) {
        connections.remove(guildId);
    }

    @NotNull
    public Map<Long, VoiceConnection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    @Override
    public void close() {
        if (!connections.isEmpty()) {
            var guilds = List.copyOf(connections.keySet());
            guilds.forEach(this::destroyConnection);
        }
    }

    private VoiceConnection createVoiceConnection(long id) {
        return new VoiceConnection(this, id);
    }
}
