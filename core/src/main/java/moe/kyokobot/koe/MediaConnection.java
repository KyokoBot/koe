package moe.kyokobot.koe;

import moe.kyokobot.koe.media.MediaFrameProvider;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.handler.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;

public interface MediaConnection extends Closeable {
    /**
     * Connects to Discord voice server using specified info.
     * @param info Discord voice server connection information
     * @return future which completes once Koe is connected to both voice gateway and can successfully
     * send UDP packets, so you can send audio data.
     */
    CompletionStage<Void> connect(VoiceServerInfo info);

    /**
     * Stops polling media frames, disconnects from the gateway and cleans everything up.
     */
    void disconnect();

    @NotNull
    KoeClient getClient();

    @NotNull
    KoeOptions getOptions();

    @Nullable
    MediaFrameProvider getAudioSender();

    long getGuildId();

    @Nullable
    MediaGatewayConnection getGatewayConnection();

    @Nullable
    VoiceServerInfo getVoiceServerInfo();

    ConnectionHandler getConnectionHandler();

    void setAudioSender(@Nullable MediaFrameProvider sender);

    void setAudioCodec(@NotNull Codec audioCodec);

    /**
     * Starts polling media frames. Called automatically after connecting, you don't have to.
     */
    void startFramePolling();

    /**
     * Stops polling media frames.
     * @see MediaConnection#startFramePolling()
     */
    void stopFramePolling();

    void registerListener(KoeEventListener listener);

    void unregisterListener(KoeEventListener listener);

    /**
     * Sends speaking state notification to the gateway.
     * @param mask new speaking state
     */
    void updateSpeakingState(int mask);

    @Override
    void close();
}
