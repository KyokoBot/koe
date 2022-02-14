package moe.kyokobot.koe;

import moe.kyokobot.koe.media.MediaFrameProvider;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.handler.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
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
     * @see #connect(VoiceServerInfo)
     */
    void disconnect();

    void reconnect();

    @NotNull
    KoeClient getClient();

    @NotNull
    KoeOptions getOptions();

    @Nullable
    MediaFrameProvider getAudioSender();

    @Nullable
    MediaFrameProvider getVideoSender();

    long getGuildId();

    @Nullable
    MediaGatewayConnection getGatewayConnection();

    @Nullable
    VoiceServerInfo getVoiceServerInfo();

    ConnectionHandler<?> getConnectionHandler();

    void setAudioSender(@Nullable MediaFrameProvider sender);

    void setAudioCodec(@NotNull Codec audioCodec);

    /**
     * Starts polling audio frames. Called automatically after connecting, you don't have to.
     */
    void startAudioFramePolling();

    /**
     * Stops polling audio frames.
     * @see MediaConnection#startAudioFramePolling()
     */
    void stopAudioFramePolling();

    void setVideoSender(@Nullable MediaFrameProvider sender);

    void setVideoCodec(@Nullable Codec videoCodec);

    /**
     * Starts polling video frames. Called automatically after connecting if codec has been set.
     */
    void startVideoFramePolling();

    /**
     * Stops polling video frames.
     * @see MediaConnection#startAudioFramePolling()
     */
    void stopVideoFramePolling();

    void registerListener(KoeEventListener listener);

    void unregisterListener(KoeEventListener listener);

    /**
     * Sends speaking state notification to the gateway.
     * @param mask new speaking state
     */
    void updateSpeakingState(int mask);

    /**
     * Closes and disposes this connection, cannot be used after this method is called.
     */
    @Override
    void close();
}
