package moe.kyokobot.koe;

import moe.kyokobot.koe.codec.CodecInfo;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.experimental.media.VideoFrameProvider;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.media.AudioFrameProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;

public interface MediaConnection extends Closeable {
    /**
     * Connects to Discord voice server using specified info.
     *
     * @param info Discord voice server connection information
     * @return future which completes once Koe is connected to both voice gateway and can successfully
     * send UDP packets, so you can send audio data.
     */
    CompletionStage<Void> connect(VoiceServerInfo info);

    /**
     * Stops polling media frames, disconnects from the gateway and cleans everything up.
     *
     * @see #connect(VoiceServerInfo)
     */
    void disconnect();

    void reconnect();

    @NotNull
    KoeClient getClient();

    @NotNull
    KoeOptions getOptions();

    @Nullable
    AudioFrameProvider getAudioSender();

    @Nullable
    VideoFrameProvider getVideoSender();

    long getGuildId();

    @Nullable
    MediaGatewayConnection getGatewayConnection();

    @Nullable
    VoiceServerInfo getVoiceServerInfo();

    ConnectionHandler<?> getConnectionHandler();

    void setAudioSender(@Nullable AudioFrameProvider sender);

    /**
     * Sets the audio codec instance for this connection.
     *
     * @param audioCodec the codec instance to use
     */
    void setAudioCodec(@NotNull CodecInstance audioCodec);

    /**
     * Sets the audio codec using a codec info (convenience method).
     * Creates a codec instance with default payload types.
     *
     * @param info the codec info to use
     */
    default void setAudioCodec(@NotNull CodecInfo info) {
        setAudioCodec(info.instantiate());
    }

    /**
     * Starts polling audio frames. Called automatically after connecting, you don't have to.
     */
    void startAudioFramePolling();

    /**
     * Stops polling audio frames.
     *
     * @see MediaConnection#startAudioFramePolling()
     */
    void stopAudioFramePolling();

    void setVideoSender(@Nullable VideoFrameProvider sender);

    /**
     * Sets the video codec instance for this connection.
     *
     * @param videoCodec the codec instance to use, or null to disable video
     */
    void setVideoCodec(@Nullable CodecInstance videoCodec);

    /**
     * Sets the video codec using a codec info (convenience method).
     * Creates a codec instance with default payload types.
     *
     * @param info the codec info to use
     */
    default void setVideoCodec(@NotNull CodecInfo info) {
        setVideoCodec(info.instantiate());
    }

    /**
     * Starts polling video frames. Called automatically after connecting if codec has been set.
     */
    void startVideoFramePolling();

    /**
     * Stops polling video frames.
     *
     * @see MediaConnection#startAudioFramePolling()
     */
    void stopVideoFramePolling();

    void registerListener(KoeEventListener listener);

    void unregisterListener(KoeEventListener listener);

    /**
     * Sends speaking state notification to the gateway.
     *
     * @param mask new speaking state
     */
    void updateSpeakingState(int mask);

    /**
     * Closes and disposes this connection, cannot be used after this method is called.
     */
    @Override
    void close();
}
