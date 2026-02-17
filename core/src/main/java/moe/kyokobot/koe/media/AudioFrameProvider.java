package moe.kyokobot.koe.media;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.CodecInstance;

import org.jetbrains.annotations.NotNull;

/**
 * Base interface for audio frame providers.
 * <p>
 * For audio-only usage with the default Opus codec, implementations may omit codec checks.
 * This is provided for completeness and for future or experimental codec support.
 */
public interface AudioFrameProvider {
    /**
     * Notifies this provider of the current codec for the connection.
     * <p>
     * <b>Guarantees:</b>
     * <ul>
     *   <li>This method is called before the first {@link #canProvide()} invocation for a provider
     *       instance attached to a connection (e.g. when set via {@code setAudioSender}).</li>
     *   <li>It is called again whenever the codec is changed via {@code setAudioCodec}
     *       while this provider is attached.</li>
     * </ul>
     * Implementations should update internal codec-dependent state in this callback. If the provider
     * cannot supply data in the requested format, it must return {@code false} from {@link #canProvide()}
     * until the codec changes to a supported one.
     *
     * @param codec the current codec instance for this connection
     */
    void onCodecChanged(@NotNull CodecInstance codec);

    /**
     * Called when this {@link AudioFrameProvider} should clean up it's event handlers and etc.
     */
    void dispose();

    /**
     * @return If true, this provider has media data available and can provide frames. This is used by the
     * frame poller to determine if it should attempt to retrieve frames from this provider.
     * Must return false if the provider cannot supply data for the current codec (see {@link #onCodecChanged}).
     */
    boolean canProvide();

    /**
     * Retrieves a media frame and writes it to the provided {@link ByteBuf}. The buffer is guaranteed to be empty
     * and have enough capacity for the frame. The provider should write the frame data to the buffer and return true
     * if a frame was provided, or return false if no frame is available (e.g. end of stream).
     *
     * @param buf The buffer to write the frame data to.
     * @return true if a frame was provided and written to the buffer, false if no frame is available.
     */
    boolean provideFrame(ByteBuf buf);
}
