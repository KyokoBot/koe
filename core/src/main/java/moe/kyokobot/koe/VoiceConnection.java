package moe.kyokobot.koe;

import moe.kyokobot.koe.audio.AudioFrameProvider;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.gateway.VoiceGatewayConnection;
import moe.kyokobot.koe.handler.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;

public interface VoiceConnection extends Closeable {
    CompletionStage<Void> connect(VoiceServerInfo info);

    void disconnect();

    @NotNull
    KoeClient getClient();

    @NotNull
    KoeOptions getOptions();

    @Nullable
    AudioFrameProvider getSender();

    long getGuildId();

    @Nullable
    VoiceGatewayConnection getGatewayConnection();

    @Nullable
    VoiceServerInfo getVoiceServerInfo();

    ConnectionHandler getConnectionHandler();

    void setAudioSender(@Nullable AudioFrameProvider sender);

    void setAudioCodec(@NotNull Codec audioCodec);

    void startFramePolling();

    void stopFramePolling();

    void registerListener(KoeEventListener listener);

    void unregisterListener(KoeEventListener listener);

    @Override
    void close();
}
