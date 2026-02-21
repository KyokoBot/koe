package moe.kyokobot.koe.poller;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.KoeEventAdapter;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.gateway.SpeakingFlags;
import moe.kyokobot.koe.media.AudioFrameProvider;
import moe.kyokobot.koe.media.IntReference;

public abstract class AbstractOpusFramePoller extends AbstractFramePoller {
    private static final long FRAME_INTERVAL_NANOS = 20_000_000L;
    private static final int RTP_TIMESTAMP_INCREMENT = 960;
    private static final int SILENCE_FRAME_COUNT = 5;

    protected final CodecInstance codec;

    private final IntReference timestamp = new IntReference();
    private final Op12HackListener hackListener;

    private int silenceCounter = 0;
    private boolean lastProvide = false;
    private boolean lastSpeaking = false;
    private boolean speaking = false;
    private int speakingMask = SpeakingFlags.NORMAL;

    protected AbstractOpusFramePoller(MediaConnection connection, CodecInstance codec) {
        super(connection);

        if (!(codec.getInfo() instanceof OpusCodecInfo)) {
            throw new IllegalArgumentException("Expected an Opus codec, got " + codec.getName());
        }

        this.codec = codec;
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
    protected long getFrameIntervalNanos() {
        return FRAME_INTERVAL_NANOS;
    }

    @Override
    protected boolean pollAndSend() {
        int attempts = getPollsPerTick();
        if (attempts <= 0) {
            return false;
        }

        boolean sentAny = false;
        for (int i = 0; i < attempts; i++) {
            if (!pollSingleFrame()) {
                break;
            }
            sentAny = true;
        }
        return sentAny;
    }

    protected int getPollsPerTick() {
        return 1;
    }

    protected AudioFrameProvider resolveProvider() {
        return connection.getAudioSender();
    }

    protected abstract boolean canSendFrame();

    protected abstract void sendFramePayload(ByteBuf buf, int len, int timestamp);

    private boolean pollSingleFrame() {
        if (!canSendFrame()) {
            return false;
        }

        AudioFrameProvider provider = resolveProvider();
        if (provider == null) {
            return false;
        }

        if (silenceCounter > 0) {
            return sendSilenceFrame();
        }

        boolean canProvide = provider.canProvide();
        if (lastProvide && !canProvide) {
            silenceCounter = SILENCE_FRAME_COUNT;
        }
        lastProvide = canProvide;

        if (silenceCounter > 0) {
            return sendSilenceFrame();
        }
        if (!canProvide) {
            return false;
        }

        ByteBuf buf = allocator.buffer();
        try {
            int start = buf.writerIndex();
            boolean wrote = provider.provideFrame(buf);
            int len = buf.writerIndex() - start;
            if (wrote && len > 0) {
                sendFramePayload(buf, len, timestamp.get());
                timestamp.add(RTP_TIMESTAMP_INCREMENT);
                setSpeaking(true);
                return true;
            }
        } finally {
            buf.release();
        }

        silenceCounter = SILENCE_FRAME_COUNT;
        return false;
    }

    private boolean sendSilenceFrame() {
        silenceCounter--;

        ByteBuf buf = allocator.buffer(OpusCodecInfo.SILENCE_FRAME.length);
        try {
            buf.writeBytes(OpusCodecInfo.SILENCE_FRAME);
            int len = buf.readableBytes();
            sendFramePayload(buf, len, timestamp.get());
            timestamp.add(RTP_TIMESTAMP_INCREMENT);
            setSpeaking(false);
            return true;
        } finally {
            buf.release();
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
        public void userStreamsChanged(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
            if (speaking) {
                connection.updateSpeakingState(speakingMask);
            }
        }
    }

    @Override
    public void close() {
        connection.unregisterListener(hackListener);
    }
}
