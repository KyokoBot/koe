package moe.kyokobot.koe;

import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Map;

public interface KoeClient extends Closeable {
    @NotNull
    VoiceConnection createConnection(long guildId);

    @Nullable
    VoiceConnection getConnection(long guildId);

    void destroyConnection(long guildId);

    @NotNull
    Map<Long, VoiceConnection> getConnections();

    @Override
    void close();

    VoiceConnection createVoiceConnection(long id);

    long getClientId();

    @NotNull
    KoeOptions getOptions();

    @NotNull
    GatewayVersion getGatewayVersion();
}
