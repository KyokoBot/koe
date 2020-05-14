package moe.kyokobot.koe.media;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.KoeEventAdapter;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.gateway.SpeakingFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link MediaFrameProvider} which automatically takes care of
 * checking codec type, sending silent frames and updating speaking state.
 */
public abstract class OpusAudioFrameProvider implements MediaFrameProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpusAudioFrameProvider.class);
    private static final int SILENCE_FRAME_COUNT = 5;
    private final VoiceConnection connection;
    private final Op12HackListener hackListener;

    // volatile because of multiple event loop threads accessing these fields.
    private AtomicInteger counter = new AtomicInteger();
    private volatile long lastFramePolled = 0;
    private volatile boolean lastProvide = false;
    private volatile boolean lastSpeaking = false;
    private volatile boolean speaking = false;
    private int speakingMask = SpeakingFlags.NORMAL;

    public OpusAudioFrameProvider(VoiceConnection connection) {
        this.connection = Objects.requireNonNull(connection);
        this.hackListener = new Op12HackListener();
        this.connection.registerListener(this.hackListener);
    }

    public int getSpeakingMask() {
        return speakingMask;
    }

    public void setSpeakingMask(int speakingMask) {
        this.speakingMask = speakingMask;
    }

    @Override
    public final boolean canSendFrame(Codec codec) {
        if (codec.getPayloadType() != OpusCodec.PAYLOAD_TYPE) {
            return false;
        }

        if (counter.get() > 0) {
            return true;
        }

        boolean provide = canProvide();

        if (lastProvide != provide) {
            lastProvide = provide;
            if (!provide) {
                counter.set(SILENCE_FRAME_COUNT);
                return true;
            }
        }

        return provide;
    }

    @Override
    public final void retrieve(Codec codec, ByteBuf buf) {
        if (codec.getPayloadType() != OpusCodec.PAYLOAD_TYPE) {
            return;
        }

        if (counter.get() > 0) {
            counter.decrementAndGet();
            buf.writeBytes(OpusCodec.SILENCE_FRAME);

            if (speaking) {
                setSpeaking(false);
            }
            return;
        }

        int startIndex = buf.writerIndex();
        retrieveOpusFrame(buf);
        boolean written = buf.writerIndex() != startIndex;

        if (written && !speaking) {
            setSpeaking(true);
        }

        if (!written) {
            logger.debug("Write silent frames");
            counter.set(SILENCE_FRAME_COUNT);
        }

        long now = System.currentTimeMillis();
        boolean changeTalking = (now - lastFramePolled) > OpusCodec.FRAME_DURATION;
        lastFramePolled = now;
        if (changeTalking) {
            setSpeaking(written);
        }
    }

    private void setSpeaking(boolean state) {
        this.speaking = state;
        if (this.speaking != this.lastSpeaking) {
            this.lastSpeaking = state;

            connection.updateSpeakingState(state ? this.speakingMask : 0);
        }
    }

    private class Op12HackListener extends KoeEventAdapter {
        @Override
        public void userConnected(String id, int audioSSRC, int videoSSRC) {
            if (speaking) {
                connection.updateSpeakingState(speakingMask);
            }
        }
    }

    @Override
    public void dispose() {
        this.connection.unregisterListener(this.hackListener);
    }

    /**
     * Called every time Opus frame poller tries to retrieve an Opus audio frame.
     *
     * @return If this method returns true, Koe will attempt to retrieve an Opus audio frame.
     */
    public abstract boolean canProvide();

    /**
     * If {@link #canProvide()} returns true, this method will attempt to retrieve an Opus audio frame.
     * <p>
     * This method must not block, otherwise it might cause severe performance issues, due to event loop thread
     * getting blocked, therefore it's recommended to load all data before or in parallel, not when Koe frame poller
     * calls this method. If no data gets written, the frame won't be sent.
     *
     * @param targetBuffer the target {@link ByteBuf} audio data should be written to.
     */
    public abstract void retrieveOpusFrame(ByteBuf targetBuffer);
}
