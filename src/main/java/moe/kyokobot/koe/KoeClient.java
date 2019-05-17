package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KoeClient implements Closeable {
    private final long clientId;
    private final Map<Long, KoeConnection> connections;

    public KoeClient(long clientId) {
        this.clientId = clientId;
        this.connections = new ConcurrentHashMap<>();
    }

    public long getClientId() {
        return clientId;
    }

    @NotNull
    public KoeConnection createConnection(long guildId) {
        return connections.computeIfAbsent(guildId, KoeConnection::new);
    }

    @Nullable
    public KoeConnection getConnection(long guildId) {
        return connections.get(guildId);
    }

    @NotNull
    public Map<Long, KoeConnection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    @Override
    public void close() {

    }
}
