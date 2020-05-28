package moe.kyokobot.koe;

import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Map;

public interface KoeClient extends Closeable {
    @NotNull
    MediaConnection createConnection(long guildId);

    @Nullable
    MediaConnection getConnection(long guildId);

    void destroyConnection(long guildId);

    @NotNull
    Map<Long, MediaConnection> getConnections();

    @Override
    void close();

    long getClientId();

    @NotNull
    KoeOptions getOptions();

    @NotNull
    GatewayVersion getGatewayVersion();
}
