package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeEventAdapter;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.gateway.CloseCode;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.ConnectException;
import java.util.concurrent.*;

import static moe.kyokobot.koe.TestUtils.await;
import static org.junit.jupiter.api.Assertions.*;

class GatewayFailureTest {
    private static final long WAIT_SECONDS = 5;

    private final RecordingListener listener = new RecordingListener();
    private KoeOptions options;
    private TcpStub stub;

    @AfterEach
    void tearDown() throws Exception {
        if (stub != null) {
            stub.close();
        }
        if (options != null) {
            options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
        }
    }

    @Test
    void refusedConnectionFailsTheFutureWithoutRetrying() throws Exception {
        var connection = connection(1000);
        var future = connection.connect(serverInfo(TcpStub.refusingEndpoint()));

        var cause = awaitFailure(future);
        assertInstanceOf(ConnectException.class, cause);
        assertSame(cause, listener.errors.poll(WAIT_SECONDS, TimeUnit.SECONDS));
        assertQuiet();
    }

    @Test
    void unresponsiveServerTimesOut() throws Exception {
        stub = new TcpStub(TcpStub.Behavior.SILENT);
        var connection = connection(200);
        var future = connection.connect(serverInfo(stub.endpoint()));

        assertInstanceOf(TimeoutException.class, awaitFailure(future));
        assertInstanceOf(TimeoutException.class, listener.errors.poll(WAIT_SECONDS, TimeUnit.SECONDS));
        await(() -> stub.closedByClient.get() == 1, WAIT_SECONDS, "socket was not closed after the timeout");
        assertQuiet();
        assertEquals(1, stub.accepted.get(), "a connection that never succeeded must not be retried");
    }

    @Test
    void serverDroppingTheConnectionFailsTheFuture() throws Exception {
        stub = new TcpStub(TcpStub.Behavior.DROP);
        var connection = connection(1000);

        awaitFailure(connection.connect(serverInfo(stub.endpoint())));
        assertNull(listener.closes.poll(500, TimeUnit.MILLISECONDS));
        assertEquals(1, stub.accepted.get(), "a connection that never succeeded must not be retried");
    }

    @Test
    void nonTlsResponseFailsTheFuture() throws Exception {
        stub = new TcpStub(TcpStub.Behavior.GARBAGE);
        var connection = connection(1000);

        awaitFailure(connection.connect(serverInfo(stub.endpoint())));
        assertNotNull(listener.errors.poll(WAIT_SECONDS, TimeUnit.SECONDS));
        await(() -> stub.closedByClient.get() == 1, WAIT_SECONDS, "socket was not closed");
        assertNull(listener.closes.poll(500, TimeUnit.MILLISECONDS));
        assertEquals(1, stub.accepted.get(), "a connection that never succeeded must not be retried");
    }

    @Test
    void disconnectWhileConnectingFailsTheFuture() throws Exception {
        stub = new TcpStub(TcpStub.Behavior.SILENT);
        var connection = connection(0);
        var future = connection.connect(serverInfo(stub.endpoint()));
        await(() -> stub.accepted.get() == 1, WAIT_SECONDS, "never connected");

        connection.disconnect();

        awaitFailure(future);
        await(() -> stub.closedByClient.get() == 1, WAIT_SECONDS, "socket was not closed on disconnect");
        assertQuiet();
        assertNull(connection.getGatewayConnection());
    }

    @Test
    void connectAfterFailureStartsAFreshAttempt() throws Exception {
        stub = new TcpStub(TcpStub.Behavior.DROP);
        var connection = connection(1000);
        awaitFailure(connection.connect(serverInfo(stub.endpoint())));

        awaitFailure(connection.connect(serverInfo(stub.endpoint())));
        assertEquals(2, stub.accepted.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {
            CloseCode.NORMAL,
            CloseCode.AUTHENTICATION_FAILED,
            CloseCode.SESSION_NO_LONGER_VALID,
            CloseCode.SERVER_NOT_FOUND,
            CloseCode.DISCONNECTED,
            CloseCode.RATE_LIMIT_EXCEEDED,
            CloseCode.DISCONNECTED_ALL_CLIENTS,
    })
    void fatalCloseIsReportedWithoutReconnecting(int code) throws Exception {
        stub = new TcpStub(TcpStub.Behavior.SILENT);
        var gateway = establishedGateway(200);

        gateway.remoteClose(code);

        var close = listener.closes.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(close);
        assertEquals(code, close.code);
        assertTrue(close.byRemote);
        Thread.sleep(300);
        assertEquals(0, stub.accepted.get());
        assertQuiet();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            CloseCode.GOING_AWAY,
            CloseCode.ABNORMAL_CLOSURE,
            CloseCode.INTERNAL_ERROR,
            CloseCode.UNKNOWN_OPCODE,
            CloseCode.FAILED_TO_DECODE_PAYLOAD,
            CloseCode.NOT_AUTHENTICATED,
            CloseCode.ALREADY_AUTHENTICATED,
            CloseCode.SESSION_TIMEOUT,
            CloseCode.UNKNOWN_PROTOCOL,
            CloseCode.VOICE_SERVER_CRASHED,
            CloseCode.UNKNOWN_ENCRYPTION_MODE,
            CloseCode.BAD_REQUEST,
            CloseCode.KOE_RECONNECT,
    })
    void recoverableCloseReconnects(int code) throws Exception {
        stub = new TcpStub(TcpStub.Behavior.SILENT);
        var gateway = establishedGateway(0);

        gateway.remoteClose(code);

        await(() -> stub.accepted.get() == 1, WAIT_SECONDS, "did not reconnect");
        assertNull(listener.closes.poll(300, TimeUnit.MILLISECONDS), "close must not be reported while reconnecting");
        gateway.close(CloseCode.NORMAL, null);
    }

    @Test
    void reconnectsGiveUpAfterRepeatedFailures() throws Exception {
        stub = new TcpStub(TcpStub.Behavior.SILENT);
        var gateway = establishedGateway(100);

        gateway.remoteClose(CloseCode.VOICE_SERVER_CRASHED);

        var close = listener.closes.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(close, "gave up without reporting the close");
        assertEquals(CloseCode.ABNORMAL_CLOSURE, close.code);

        int attempts = stub.accepted.get();
        Thread.sleep(500);
        assertEquals(attempts, stub.accepted.get(), "kept reconnecting after reporting the close");
        assertEquals(3, attempts, "reconnect attempts");
        await(() -> stub.closedByClient.get() == attempts, WAIT_SECONDS, "leaked sockets of failed attempts");
        assertEquals(attempts, listener.errors.size(), "each failed attempt is reported once: " + listener.errors);
        assertTrue(listener.closes.isEmpty(), "close reported more than once: " + listener.closes);
    }

    private MediaConnection connection(long connectTimeout) {
        options = KoeOptions.builder()
                .setDAVEEnabled(false)
                .setGatewayConnectTimeout(connectTimeout)
                .create();
        var connection = Koe.koe(options).newClient(1L).createConnection(2L);
        connection.registerListener(listener);
        return connection;
    }

    private EstablishedGateway establishedGateway(long connectTimeout) {
        var connection = (MediaConnectionImpl) connection(connectTimeout);
        return new EstablishedGateway(connection, serverInfo(stub.endpoint()));
    }

    private static VoiceServerInfo serverInfo(String endpoint) {
        return VoiceServerInfo.builder()
                .setSessionId("session")
                .setToken("token")
                .setEndpoint(endpoint)
                .setChannelId(3L)
                .build();
    }

    private static Throwable awaitFailure(CompletionStage<Void> future) {
        var ex = assertThrows(ExecutionException.class,
                () -> future.toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS));
        return ex.getCause();
    }

    /**
     * Fails if anything else gets reported shortly after.
     */
    private void assertQuiet() throws InterruptedException {
        Thread.sleep(300);
        assertEquals(0, listener.errors.size(), "unexpected gateway errors: " + listener.errors);
        assertEquals(0, listener.closes.size(), "unexpected gateway closes: " + listener.closes);
    }

    /**
     * Gateway which skips the WebSocket handshake, as if the session was established before the test began.
     */
    private static final class EstablishedGateway extends MediaGatewayV8Connection {
        EstablishedGateway(MediaConnectionImpl connection, VoiceServerInfo info) {
            super(connection, info);
            connectFuture.complete(null);
        }

        void remoteClose(int code) {
            onClose(code, "test", true);
        }
    }

    private static final class Close {
        final int code;
        final boolean byRemote;

        Close(int code, boolean byRemote) {
            this.code = code;
            this.byRemote = byRemote;
        }

        @Override
        public String toString() {
            return code + (byRemote ? " (remote)" : " (local)");
        }
    }

    private static final class RecordingListener extends KoeEventAdapter {
        final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
        final BlockingQueue<Close> closes = new LinkedBlockingQueue<>();

        @Override
        public void gatewayError(Throwable cause) {
            errors.add(cause);
        }

        @Override
        public void gatewayClosed(int code, String reason, boolean byRemote) {
            closes.add(new Close(code, byRemote));
        }
    }
}
