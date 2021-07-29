package moe.kyokobot.koe.media;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base interface for media frame providers. Note that Koe doesn't handle stuff such as speaking state, silent frames
 * or etc., these are implemented by codec-specific frame provider classes.
 *
 * @see OpusAudioFrameProvider for Opus audio codec specific implementation that handles speaking state and etc.
 */
public interface MediaFrameProvider {
    /**
     * Called when this {@link MediaFrameProvider} should clean up it's event handlers and etc.
     */
    void dispose();

    /**
     * @return Frame interval between polling attempts.
     */
    int getFrameInterval();

    /**
     * Sets delay between polling attempts by frame poller.
     */
    void setFrameInterval(int interval);

    /**
     * @return If true, Koe will request media data for given {@link Codec} by
     * calling {@link #retrieve(Codec, ByteBuf)} method.
     */
    boolean canSendFrame(Codec codec);

    /**
     * If {@link #canSendFrame()} returns true, Koe will attempt to retrieve an media frame encoded with specified
     * {@link Codec} type, by calling this method with target {@link ByteBuf} where the data should be written to.
     * Do not call {@link ByteBuf#release()} - memory management is already handled by Koe itself. In case if no
     * data gets written to the buffer, audio packet won't be sent.
     * <p>
     * Do not let this method block - all data should be queued on another thread or pre-loaded in
     * memory - otherwise it will very likely have significant impact on application performance.
     *
     * @param codec     {@link Codec} type this handler was registered with.
     * @param buf       {@link ByteBuf} the buffer where the media data should be written to.
     * @param timestamp {@link AtomicInteger} reference to current frame timestamp, which must be updated with
     *                  timestamp of written frame.
     * @param marker    Represents state of marker flag in RTP header.
     * @return If true, Koe will immediately attempt to poll a next frame, this is meant for video transmissions.
     */
    boolean retrieve(Codec codec, ByteBuf buf, AtomicInteger timestamp, @Nullable AtomicBoolean marker);
}
