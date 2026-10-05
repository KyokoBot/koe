package moe.kyokobot.koe.poller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.gateway.SpeakingFlags;
import moe.kyokobot.koe.internal.DAVEManager;
import moe.kyokobot.koe.internal.KoeClientImpl;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.media.AudioFrameProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AbstractOpusFramePollerTest {
    private static final byte[] FRAME_A = {1, 2, 3, 4, 5};
    private static final byte[] FRAME_B = {6, 7, 8, 9};
    private static final Object NOTHING_WRITTEN = new Object();

    private final UnpooledByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
    private final ScriptedProvider provider = new ScriptedProvider();
    private KoeOptions options;
    private RecordingConnection connection;
    private TestPoller poller;

    @AfterEach
    void tearDown() {
        poller.close();
        connection.close();
        options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
        assertEquals(0, allocator.metric().usedDirectMemory(), "leaked buffers");
    }

    @Test
    void framesAreSentWithConsecutiveTimestampsAndSpeakingIsAnnouncedOnce() {
        setUp(false);
        provider.script(FRAME_A, FRAME_B, FRAME_A);

        tick(3);

        assertSent(FRAME_A, FRAME_B, FRAME_A);
        assertEquals(List.of(0, 960, 1920), poller.timestamps);
        assertEquals(List.of(SpeakingFlags.NORMAL), connection.speaking);
    }

    @Test
    void endOfAudioIsFollowedByFiveSilenceFrames() {
        setUp(false);
        provider.script(FRAME_A);

        tick(8);

        assertSent(FRAME_A, silence(), silence(), silence(), silence(), silence());
        assertEquals(List.of(SpeakingFlags.NORMAL), connection.speaking);
    }

    @Test
    void speakingStopIsSentOnlyWhenEnabled() {
        setUp(true);
        provider.script(FRAME_A);

        tick(8);

        assertEquals(List.of(SpeakingFlags.NORMAL, 0), connection.speaking);
    }

    @Test
    void providerWritingNothingEndsWithSilence() {
        setUp(false);
        provider.script(FRAME_A, NOTHING_WRITTEN, FRAME_B);

        tick(8); // the tick which got nothing from the provider sends nothing

        assertSent(FRAME_A, silence(), silence(), silence(), silence(), silence(), FRAME_B);
    }

    @Test
    void providerExceptionDoesNotBreakPolling() {
        setUp(false);
        provider.script(FRAME_A, new IllegalStateException("decoder blew up"), FRAME_B);

        tick(1);
        assertThrows(IllegalStateException.class, poller::pollAndSend);
        tick(1);

        assertSent(FRAME_A, FRAME_B);
        assertEquals(List.of(0, 960), poller.timestamps);
    }

    @Test
    void transportExceptionDoesNotAnnounceSpeaking() {
        setUp(false);
        provider.script(FRAME_A, FRAME_B);
        poller.sendFailure = new IllegalStateException("socket closed");

        assertThrows(IllegalStateException.class, poller::pollAndSend);
        assertEquals(List.of(), connection.speaking);

        poller.sendFailure = null;
        tick(1);
        assertSent(FRAME_B);
        assertEquals(List.of(SpeakingFlags.NORMAL), connection.speaking);
    }

    @Test
    void framesAreKeptWhileTransportIsNotReady() {
        setUp(false);
        provider.script(FRAME_A);
        poller.ready = false;

        assertFalse(poller.pollAndSend());
        assertEquals(1, provider.remaining(), "frame was consumed and lost");

        poller.ready = true;
        tick(1);
        assertSent(FRAME_A);
    }

    @Test
    void framesAreKeptUntilKeyRatchetIsReady() {
        setUp(false, true);
        provider.script(FRAME_A);
        var dave = connection.createDAVEManager();
        assertNotNull(dave, "libdave natives are not available on this platform");
        connection.dave = dave;
        dave.handleSessionDescription(new JsonObject().add("dave_protocol_version", 1), 3L);

        assertFalse(poller.pollAndSend());
        assertEquals(1, provider.remaining(), "frame was consumed and lost");
        assertEquals(List.of(), connection.speaking);

        dave.handleSecureFramesPrepareProtocolTransition(5, 0);
        dave.handleSecureFramesExecuteTransition(5);
        tick(1);
        assertSent(FRAME_A);
    }

    @Test
    void speakingIsReannouncedOnSessionChangesOnlyWhileSpeaking() {
        setUp(false);
        provider.script(FRAME_A, FRAME_B);
        tick(1);

        connection.getDispatcher().sessionDescription(new JsonObject());
        connection.getDispatcher().userStreamsChanged("1", 1, 0, 0);
        assertEquals(List.of(SpeakingFlags.NORMAL, SpeakingFlags.NORMAL, SpeakingFlags.NORMAL), connection.speaking);

        tick(7);
        connection.speaking.clear();
        connection.getDispatcher().sessionDescription(new JsonObject());
        connection.getDispatcher().userStreamsChanged("1", 1, 0, 0);
        assertEquals(List.of(), connection.speaking);
    }

    @Test
    void closedPollerStopsListening() {
        setUp(false);
        provider.script(FRAME_A, FRAME_B);
        tick(1);
        connection.speaking.clear();

        poller.close();
        connection.getDispatcher().sessionDescription(new JsonObject());

        assertEquals(List.of(), connection.speaking);
    }

    private void setUp(boolean sendSpeakingStop) {
        setUp(sendSpeakingStop, false);
    }

    private void setUp(boolean sendSpeakingStop, boolean enableDAVE) {
        options = KoeOptions.builder()
                .setDAVEEnabled(enableDAVE)
                .setByteBufAllocator(allocator)
                .setSendSpeakingStop(sendSpeakingStop)
                .create();
        connection = new RecordingConnection(new KoeClientImpl(1L, options));
        connection.setAudioSender(provider);
        poller = new TestPoller(connection, OpusCodecInfo.INSTANCE.instantiate());
    }

    private void tick(int times) {
        for (int i = 0; i < times; i++) {
            poller.pollAndSend();
        }
    }

    private void assertSent(byte[]... frames) {
        assertEquals(frames.length, poller.sent.size(), "frame count");
        for (int i = 0; i < frames.length; i++) {
            assertArrayEquals(frames[i], poller.sent.get(i), "frame " + i);
        }
    }

    private static byte[] silence() {
        return OpusCodecInfo.SILENCE_FRAME;
    }

    private static final class TestPoller extends AbstractOpusFramePoller {
        final List<byte[]> sent = new ArrayList<>();
        final List<Integer> timestamps = new ArrayList<>();
        boolean ready = true;
        RuntimeException sendFailure;

        TestPoller(MediaConnection connection, CodecInstance codec) {
            super(connection, codec);
        }

        @Override
        protected boolean canSendFrame() {
            return ready;
        }

        @Override
        protected void sendFramePayload(ByteBuf buf, int len, int timestamp) {
            if (sendFailure != null) {
                throw sendFailure;
            }
            var bytes = new byte[len];
            buf.getBytes(buf.readerIndex(), bytes);
            sent.add(bytes);
            timestamps.add(timestamp);
        }
    }

    /**
     * Provides queued frames, throws queued exceptions and writes nothing for {@link #NOTHING_WRITTEN}.
     */
    private static final class ScriptedProvider implements AudioFrameProvider {
        private final Deque<Object> script = new ArrayDeque<>();

        void script(Object... steps) {
            script.addAll(List.of(steps));
        }

        int remaining() {
            return script.size();
        }

        @Override
        public void onCodecChanged(@NotNull CodecInstance codec) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean canProvide() {
            return !script.isEmpty();
        }

        @Override
        public boolean provideFrame(ByteBuf buf) {
            var step = script.poll();
            if (step instanceof RuntimeException) {
                buf.writeByte(0x7f); // partially written frame
                throw (RuntimeException) step;
            }
            if (step instanceof byte[]) {
                buf.writeBytes((byte[]) step);
                return true;
            }
            return false;
        }
    }

    private static final class RecordingConnection extends MediaConnectionImpl {
        final List<Integer> speaking = new ArrayList<>();
        DAVEManager dave;

        RecordingConnection(KoeClientImpl client) {
            super(client, 2L);
        }

        @Override
        public DAVEManager getDAVEManager() {
            return dave;
        }

        @Override
        public void updateSpeakingState(int mask) {
            speaking.add(mask);
        }
    }
}
