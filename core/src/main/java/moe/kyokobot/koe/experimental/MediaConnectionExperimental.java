package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInfo;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.experimental.media.VideoFrameProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MediaConnectionExperimental extends MediaConnection {
    @Nullable
    VideoFrameProvider getVideoSender();

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
     * @see #startVideoFramePolling()
     */
    void stopVideoFramePolling();

}
