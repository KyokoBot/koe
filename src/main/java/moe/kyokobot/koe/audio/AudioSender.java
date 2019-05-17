package moe.kyokobot.koe.audio;

import io.netty.buffer.ByteBuf;

public interface AudioSender {
    /**
     * @return If true, Koe will request audio data by calling {@link #retrieve(ByteBuf)} method.
     */
    boolean hasAudioData();

    /**
     * If {@link #hasAudioData()} returns true, Koe will attempt to retrieve raw 20ms Opus frame, by calling this method
     * with target {@link ByteBuf} where the data should be written to. Do not call {@link ByteBuf#release()} - memory
     * management is already handled by Koe itself. Also in case if no data gets written to the buffer, audio packet
     * won't be sent.
     * <p>
     * Do not let this method block - all data should be queued on another thread or pre-loaded in
     * memory - otherwise it will very likely have significant impact of application performance.
     *
     * @param buf {@link ByteBuf} the buffer where the audio data should be written to.
     */
    void retrieve(ByteBuf buf);
}