package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.KoeEventListener;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.gateway.Op;
import moe.kyokobot.koe.internal.KoeClientImpl;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks what differs between the gateway versions, without a WebSocket connection.
 */
class GatewayProtocolTest {
    private static final VoiceServerInfo SERVER_INFO = VoiceServerInfo.builder()
            .setSessionId("session")
            .setToken("token")
            .setEndpoint("localhost:1")
            .setChannelId(3L)
            .build();

    private final List<JsonObject> sent = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private KoeOptions options;
    private MediaConnectionImpl connection;

    @BeforeEach
    void setUp() {
        options = KoeOptions.builder().setDAVEEnabled(false).create();
        connection = new MediaConnectionImpl(new KoeClientImpl(1L, options), 2L);
        connection.registerListener(new KoeEventListener() {
            @Override
            public void userStreamsChanged(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
                events.add("streams " + id + " " + audioSSRC);
            }

            @Override
            public void usersConnected(List<String> userIds) {
                events.add("connected " + userIds);
            }

            @Override
            public void userDisconnected(String id) {
                events.add("disconnected " + id);
            }
        });
    }

    @AfterEach
    void tearDown() {
        connection.close();
        options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }

    @Test
    void identifyAddsFieldsWithEachVersion() {
        var v4 = identify(gateway(4));
        assertEquals("1", v4.getString("user_id"));
        assertEquals("2", v4.getString("server_id"));
        assertEquals("session", v4.getString("session_id"));
        assertEquals("token", v4.getString("token"));
        assertFalse(v4.has("video"));

        var v5 = identify(gateway(5));
        assertTrue(v5.getBoolean("video"));
        assertFalse(v5.has("channel_id"));

        var v8 = identify(gateway(8));
        assertTrue(v8.getBoolean("video"));
        assertEquals("3", v8.getString("channel_id"));
        assertEquals(0, v8.getInt("max_dave_protocol_version"));
    }

    @Test
    void v8AcknowledgesTheLastSequenceOnResumeAndHeartbeat() {
        var gateway = gateway(8);
        gateway.handlePayload(new JsonObject().add("op", Op.RESUMED).add("seq", 42).add("d", new JsonObject()));

        gateway.resume();
        assertEquals(42, payload(Op.RESUME).getInt("seq_ack"));
        assertEquals(42, ((JsonObject) gateway.heartbeatPayload()).getInt("seq_ack"));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void olderVersionsDoNotAcknowledgeSequences(int version) {
        var gateway = gateway(version);
        gateway.handlePayload(new JsonObject().add("op", Op.RESUMED).add("seq", 42).add("d", new JsonObject()));

        gateway.resume();
        assertFalse(payload(Op.RESUME).has("seq_ack"));
        assertInstanceOf(Long.class, gateway.heartbeatPayload());
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 8})
    void userEventsReachListeners(int version) {
        var gateway = gateway(version);

        gateway.handlePayload(op(Op.CLIENT_CONNECT, new JsonObject().add("user_ids", JsonArray.from("10", "11"))));
        gateway.handlePayload(op(Op.VIDEO, new JsonObject()
                .add("user_id", "10").add("audio_ssrc", 5).add("video_ssrc", 0).add("rtx_ssrc", 0)));
        gateway.handlePayload(op(Op.CLIENT_DISCONNECT, new JsonObject().add("user_id", "11")));

        assertEquals(List.of("connected [10, 11]", "streams 10 5", "disconnected 11"), events);
    }

    @Test
    void v4HasNoMediaValve() {
        var gateway = gateway(4);
        gateway.handlePayload(op(Op.CLIENT_DISCONNECT, new JsonObject().add("user_id", "11")));

        assertNull(gateway.getValve());
        assertTrue(sent.isEmpty(), "sent " + sent);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 8})
    void mediaValveFollowsUserEvents(int version) {
        var gateway = gateway(version);
        assertNotNull(gateway.getValve());

        gateway.handlePayload(op(Op.VIDEO, new JsonObject()
                .add("user_id", "10").add("audio_ssrc", 5).add("video_ssrc", 6).add("rtx_ssrc", 7)
                .add("streams", JsonArray.from(new JsonObject().add("ssrc", 6)))));
        assertEquals(0, payload(Op.MEDIA_SINK_WANTS).getInt("6"));

        sent.clear();
        gateway.handlePayload(op(Op.CLIENT_DISCONNECT, new JsonObject().add("user_id", "10")));
        assertFalse(payload(Op.MEDIA_SINK_WANTS).has("6"));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 8})
    void backendVersionIsRequestedAfterReadyFromV6(int version) {
        var gateway = gateway(version);

        gateway.handlePayload(op(Op.READY, new JsonObject()
                .add("ssrc", 1).add("ip", "127.0.0.1").add("port", 9)
                .add("modes", JsonArray.from("aead_xchacha20_poly1305_rtpsize"))));

        long requests = sent.stream().filter(o -> o.getInt("op") == Op.VOICE_BACKEND_VERSION).count();
        assertEquals(version >= 6 ? 1 : 0, requests);
    }

    private AbstractMediaGatewayConnection gateway(int version) {
        switch (version) {
            case 4:
                return new MediaGatewayV4Connection(connection, SERVER_INFO) {
                    @Override
                    protected void sendRaw(JsonObject object) {
                        sent.add(object);
                    }
                };
            case 5:
                return new MediaGatewayV5Connection(connection, SERVER_INFO) {
                    @Override
                    protected void sendRaw(JsonObject object) {
                        sent.add(object);
                    }
                };
            case 8:
                return new MediaGatewayV8Connection(connection, SERVER_INFO) {
                    @Override
                    protected void sendRaw(JsonObject object) {
                        sent.add(object);
                    }
                };
            default:
                throw new IllegalArgumentException("unknown version " + version);
        }
    }

    private JsonObject identify(AbstractMediaGatewayConnection gateway) {
        sent.clear();
        gateway.identify();
        return payload(Op.IDENTIFY);
    }

    private JsonObject payload(int op) {
        var matching = sent.stream().filter(o -> o.getInt("op") == op).collect(Collectors.toList());
        assertEquals(1, matching.size(), "payloads with op " + op + ": " + sent);
        return matching.get(0).getObject("d");
    }

    private static JsonObject op(int op, JsonObject data) {
        return new JsonObject().add("op", op).add("d", data);
    }
}
