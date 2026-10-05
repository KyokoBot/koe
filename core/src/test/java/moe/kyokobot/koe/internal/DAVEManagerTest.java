package moe.kyokobot.koe.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.libdave.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DAVEManagerTest {
    private static final int SSRC = 0xDEADBEEF;
    private static final long CHANNEL_ID = 3L;
    private static final byte[] GARBAGE = {0x13, 0x37, 0x00, (byte) 0xff, 0x42};

    private KoeOptions options;
    private RecordingConnection connection;
    private DAVEManager dave;

    @BeforeEach
    void setUp() {
        options = KoeOptions.builder().create();
        var client = new KoeClientImpl(1234L, options);
        assertNotNull(client.getDaveFactory(), "libdave natives are not available on this platform");

        connection = new RecordingConnection(client);
        dave = connection.createDAVEManager();
        connection.dave = dave;
    }

    @AfterEach
    void tearDown() {
        connection.close();
        options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }

    @Test
    void e2eeSessionReplacesAudioWithSilenceUntilKeyRatchetIsReady() {
        startSession(1);

        assertEquals(List.of("sendMLSKeyPackage"), connection.sent);
        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, opusFrame()));
    }

    @Test
    void e2eeSessionDropsVideoUntilKeyRatchetIsReady() {
        startSession(1);

        assertEncryptFails(MediaType.VIDEO, opusFrame());
    }

    @Test
    void silenceIsSentAsIs() {
        startSession(1);

        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, OpusCodecInfo.SILENCE_FRAME));
    }

    @Test
    void sessionWithoutE2EEPassesMediaThrough() {
        startSession(0);

        var frame = opusFrame();
        assertArrayEquals(frame, encryptOk(MediaType.AUDIO, frame));
        assertArrayEquals(frame, encryptOk(MediaType.VIDEO, frame));
    }

    @Test
    void closedManagerRefusesToEncrypt() throws Exception {
        startSession(0);
        dave.close();

        assertEncryptFails(MediaType.AUDIO, opusFrame());
    }

    @Test
    void invalidWelcomeIsReportedAndRejoinRequested() {
        startSession(1);
        connection.sent.clear();

        dave.handleMLSWelcome(7, GARBAGE);

        assertEquals(List.of("sendMLSInvalidCommitWelcome(7)", "sendMLSKeyPackage"), connection.sent);
        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, opusFrame()));
    }

    @Test
    void commitWithoutGroupStateIsIgnored() {
        startSession(1);
        connection.sent.clear();

        // libdave ignores commits until a group exists, so an in-flight commit during a reset doesn't cause another one.
        dave.handleMLSPrepareCommitTransition(9, GARBAGE);

        assertEquals(List.of(), connection.sent);
        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, opusFrame()));
        assertEncryptFails(MediaType.VIDEO, opusFrame());
    }

    @Test
    void malformedProposalsAndExternalSenderAreTolerated() {
        startSession(1);

        assertDoesNotThrow(() -> dave.handleMLSExternalSender(GARBAGE));
        assertDoesNotThrow(() -> dave.handleMLSProposals(GARBAGE));
        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, opusFrame()));
    }

    @Test
    void downgradeTransitionAppliesOnlyOnceExecuted() {
        startSession(1);
        connection.sent.clear();

        dave.handleSecureFramesPrepareProtocolTransition(5, 0);
        assertEquals(List.of("sendSecureFramesReadyForTransition(5)"), connection.sent);
        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, opusFrame()));

        dave.handleSecureFramesExecuteTransition(5);
        var frame = opusFrame();
        assertArrayEquals(frame, encryptOk(MediaType.AUDIO, frame));
    }

    @Test
    void executingUnknownTransitionIsIgnored() {
        startSession(1);

        dave.handleSecureFramesExecuteTransition(42);

        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, encryptOk(MediaType.AUDIO, opusFrame()));
        assertEncryptFails(MediaType.VIDEO, opusFrame());
    }

    @Test
    void udpPacketsNeverCarryPlaintextBeforeKeyRatchet() {
        startSession(1);
        var udp = new DiscordUDPConnection(connection, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9), SSRC);
        // "plain" transport encryption, so the RTP payload is exactly what DAVE produced.
        var key = new JsonArray();
        for (int i = 0; i < 32; i++) key.add(0);
        udp.handleSessionDescription(new JsonObject().add("mode", "plain").add("secret_key", key));

        var frame = opusFrame();
        assertArrayEquals(OpusCodecInfo.SILENCE_FRAME, rtpPayload(udp, CodecType.AUDIO, frame));
        assertNull(rtpPayload(udp, CodecType.VIDEO, frame));
    }

    private void startSession(int protocolVersion) {
        dave.handleSessionDescription(new JsonObject().add("dave_protocol_version", protocolVersion), CHANNEL_ID);
    }

    private byte[] encryptOk(MediaType type, byte[] frame) {
        ByteBuf in = Unpooled.directBuffer(frame.length).writeBytes(frame);
        ByteBuf out = Unpooled.directBuffer();
        try {
            int result = dave.encrypt(type, SSRC, out, in, frame.length);
            assertTrue(result >= 0, "encrypt failed: " + result);
            return ByteBufUtil.getBytes(out);
        } finally {
            in.release();
            out.release();
        }
    }

    private void assertEncryptFails(MediaType type, byte[] frame) {
        ByteBuf in = Unpooled.directBuffer(frame.length).writeBytes(frame);
        ByteBuf out = Unpooled.directBuffer();
        try {
            int result = dave.encrypt(type, SSRC, out, in, frame.length);
            assertTrue(result < 0, "expected failure, got " + result);
            assertEquals(0, out.readableBytes(), "failed encryption produced output");
        } finally {
            in.release();
            out.release();
        }
    }

    private static byte[] rtpPayload(DiscordUDPConnection udp, CodecType type, byte[] frame) {
        ByteBuf in = Unpooled.directBuffer(frame.length).writeBytes(frame);
        try {
            var packet = udp.createPacket(type, (byte) 120, 0, in, frame.length, false);
            if (packet == null) {
                return null;
            }
            try {
                var bytes = ByteBufUtil.getBytes(packet);
                return Arrays.copyOfRange(bytes, 12, bytes.length);
            } finally {
                packet.release();
            }
        } finally {
            in.release();
        }
    }

    private static byte[] opusFrame() {
        var frame = new byte[120];
        for (int i = 0; i < frame.length; i++) frame[i] = (byte) (0x40 + i);
        return frame;
    }

    private static final class RecordingConnection extends MediaConnectionImpl {
        final List<String> sent = new CopyOnWriteArrayList<>();
        final MediaGatewayConnection gateway = (MediaGatewayConnection) Proxy.newProxyInstance(
                MediaGatewayConnection.class.getClassLoader(),
                new Class<?>[]{MediaGatewayConnection.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("send")) {
                        sent.add(args[0] instanceof Integer ? method.getName() + "(" + args[0] + ")" : method.getName());
                    }
                    var type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == long.class) return 0L;
                    if (type == int.class) return 0;
                    return null;
                });
        DAVEManager dave;

        RecordingConnection(KoeClientImpl client) {
            super(client, 2L);
        }

        @Override
        public MediaGatewayConnection getGatewayConnection() {
            return gateway;
        }

        @Override
        public DAVEManager getDAVEManager() {
            return dave;
        }
    }
}
