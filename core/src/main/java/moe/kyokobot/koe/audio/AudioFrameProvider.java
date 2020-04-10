package moe.kyokobot.koe.audio;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.Codec;

public interface AudioFrameProvider {
    /**
     * @return If true, Koe will request audio data by calling {@link #retrieve(Codec, ByteBuf)} method.
     */
    boolean canSendFrame();

    /**
     * If {@link #canSendFrame()} returns true, Koe will attempt to retrieve an audio frame encoded with specified
     * {@link Codec} type, by calling this method with target {@link ByteBuf} where the data should be written to.
     * Do not call {@link ByteBuf#release()} - memory management is already handled by Koe itself. In case if no
     * data gets written to the buffer, audio packet won't be sent.
     * <p>
     * Do not let this method block - all data should be queued on another thread or pre-loaded in
     * memory - otherwise it will very likely have significant impact of application performance.
     *
     * @param codec {@link Codec} type this handler was registered with.
     * @param buf {@link ByteBuf} the buffer where the audio data should be written to.
     */
    void retrieve(Codec codec, ByteBuf buf);
}
