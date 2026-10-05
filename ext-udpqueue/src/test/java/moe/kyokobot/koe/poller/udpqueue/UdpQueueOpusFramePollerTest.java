package moe.kyokobot.koe.poller.udpqueue;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.media.AudioFrameProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sends audio through the native udp-queue to local UDP sockets standing in for Discord voice servers.
 * The "plain" transport mode is used, so the RTP payload on the wire is exactly the provided frame.
 */
class UdpQueueOpusFramePollerTest {
    private static final int HEADER_LENGTH = 12;
    private static final long WAIT_SECONDS = 5;
    // Everything still queued (DEFAULT_BUFFER_DURATION of audio) has to be flushed well within this.
    private static final long DRAIN_MILLIS = 1000;

    private final List<FakeVoiceServer> servers = new ArrayList<>();
    private QueueManagerPool pool;
    private KoeOptions options;
    private KoeClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (pool != null) {
            pool.close();
        }
        if (options != null) {
            options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
        }
        servers.forEach(FakeVoiceServer::close);
    }

    @Test
    void packetsArriveInOrderWithoutGaps() throws Exception {
        setUp(1);
        var server = server();
        connect(1L, 0x11223344, server);

        var packets = server.receive(60);

        byte payloadType = OpusCodecInfo.INSTANCE.instantiate().getPayloadType();
        for (int i = 0; i < packets.size(); i++) {
            var packet = packets.get(i);
            assertEquals((byte) 0x80, packet.raw[0], "RTP version");
            assertEquals(payloadType, packet.raw[1], "payload type");
            assertEquals(0x11223344, packet.ssrc);
            assertEquals(1L, packet.guildId);
            if (i > 0) {
                var previous = packets.get(i - 1);
                assertEquals(previous.frame + 1, packet.frame, "frames must not be dropped or reordered");
                assertEquals((char) (previous.seq + 1), packet.seq, "RTP sequence");
                assertEquals(previous.timestamp + 960, packet.timestamp, "RTP timestamp");
            }
        }
    }

    @Test
    void packetsArePacedInRealTime() throws Exception {
        setUp(1);
        var server = server();
        connect(1L, 1, server);

        // The queue is filled in a burst, so skip the start and measure the steady state.
        server.receive(10);
        long start = System.nanoTime();
        server.receive(50);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMillis >= 800, "50 frames of 20ms were sent in " + elapsedMillis + "ms");
    }

    @Test
    void serverChangeMovesTrafficToTheNewServer() throws Exception {
        setUp(1);
        var oldServer = server();
        var newServer = server();
        var connection = connect(1L, 1, oldServer);
        oldServer.receive(10);

        reconnect(connection, 2, newServer);

        var packets = newServer.receive(30);
        assertTrue(packets.stream().allMatch(p -> p.ssrc == 2), "packets with the old SSRC reached the new server");
        oldServer.awaitSilence();
        assertFalse(oldServer.received.stream().anyMatch(p -> p.ssrc == 2), "packets for the new server reached the old one");
    }

    @Test
    void reconnectToTheSameServerKeepsDelivering() throws Exception {
        setUp(1);
        var server = server();
        var connection = connect(1L, 1, server);
        server.receive(10);

        reconnect(connection, 2, server);

        server.awaitPacket(p -> p.ssrc == 2);
        var packets = server.receive(30);
        assertTrue(packets.stream().allMatch(p -> p.ssrc == 2));
    }

    @Test
    void connectionsSharingAQueueManagerDoNotCrossDeliver() throws Exception {
        setUp(1);
        var serverA = server();
        var serverB = server();
        connect(1L, 1, serverA);
        connect(2L, 2, serverB);

        var packetsA = serverA.receive(40);
        var packetsB = serverB.receive(40);

        assertTrue(packetsA.stream().allMatch(p -> p.ssrc == 1 && p.guildId == 1L), "server A got foreign packets");
        assertTrue(packetsB.stream().allMatch(p -> p.ssrc == 2 && p.guildId == 2L), "server B got foreign packets");
    }

    @Test
    void closedConnectionStopsSending() throws Exception {
        setUp(1);
        var server = server();
        var connection = connect(1L, 1, server);
        server.receive(10);

        connection.close();

        server.awaitSilence();
    }

    private void setUp(int poolSize) {
        pool = new QueueManagerPool(poolSize, QueueManagerPool.DEFAULT_BUFFER_DURATION);
        options = KoeOptions.builder()
                .setDAVEEnabled(false)
                .setFramePollerFactory(new UdpQueueFramePollerFactory(pool))
                .create();
        client = Koe.koe(options).newClient(1234L);
    }

    private FakeVoiceServer server() throws SocketException {
        var server = new FakeVoiceServer();
        servers.add(server);
        return server;
    }

    private MediaConnectionImpl connect(long guildId, int ssrc, FakeVoiceServer server) {
        var connection = (MediaConnectionImpl) client.createConnection(guildId);
        connection.setAudioSender(new CountingProvider(guildId));
        startSession(connection, ssrc, server);
        return connection;
    }

    /**
     * Same steps as {@link MediaConnectionImpl#connect}, which tears the old session down first.
     */
    private static void reconnect(MediaConnectionImpl connection, int ssrc, FakeVoiceServer server) {
        connection.disconnect();
        startSession(connection, ssrc, server);
    }

    private static void startSession(MediaConnectionImpl connection, int ssrc, FakeVoiceServer server) {
        var udp = new DiscordUDPConnection(connection, server.address(), ssrc);
        connection.setConnectionHandler(udp);

        var key = new JsonArray();
        for (int i = 0; i < 32; i++) key.add(0);
        udp.handleSessionDescription(new JsonObject().add("mode", "plain").add("secret_key", key));
    }

    /**
     * Endless audio, each frame is the guild ID followed by a frame counter.
     */
    private static final class CountingProvider implements AudioFrameProvider {
        private final long guildId;
        private int frame;

        CountingProvider(long guildId) {
            this.guildId = guildId;
        }

        @Override
        public void onCodecChanged(@NotNull CodecInstance codec) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean canProvide() {
            return true;
        }

        @Override
        public boolean provideFrame(ByteBuf buf) {
            buf.writeLong(guildId);
            buf.writeInt(frame++);
            return true;
        }
    }

    private static final class Packet {
        final byte[] raw;
        final char seq;
        final int timestamp;
        final int ssrc;
        final long guildId;
        final int frame;

        Packet(byte[] raw) {
            this.raw = raw;
            var buf = ByteBuffer.wrap(raw);
            this.seq = buf.getChar(2);
            this.timestamp = buf.getInt(4);
            this.ssrc = buf.getInt(8);
            this.guildId = buf.getLong(HEADER_LENGTH);
            this.frame = buf.getInt(HEADER_LENGTH + 8);
        }
    }

    private static final class FakeVoiceServer {
        private final DatagramSocket socket;
        private final BlockingQueue<Packet> incoming = new LinkedBlockingQueue<>();
        final List<Packet> received = new java.util.concurrent.CopyOnWriteArrayList<>();

        FakeVoiceServer() throws SocketException {
            socket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
            var thread = new Thread(this::receiveLoop, "FakeVoiceServer-" + socket.getLocalPort());
            thread.setDaemon(true);
            thread.start();
        }

        InetSocketAddress address() {
            return (InetSocketAddress) socket.getLocalSocketAddress();
        }

        List<Packet> receive(int count) throws InterruptedException {
            var packets = new ArrayList<Packet>(count);
            for (int i = 0; i < count; i++) {
                var packet = incoming.poll(WAIT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(packet, "received only " + i + " of " + count + " packets");
                packets.add(packet);
            }
            return packets;
        }

        void awaitPacket(java.util.function.Predicate<Packet> condition) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            while (System.nanoTime() < deadline) {
                var packet = incoming.poll(100, TimeUnit.MILLISECONDS);
                if (packet != null && condition.test(packet)) {
                    return;
                }
            }
            fail("no matching packet received");
        }

        /**
         * Waits until the queued packets are flushed, then expects nothing else to arrive.
         */
        void awaitSilence() throws InterruptedException {
            Thread.sleep(DRAIN_MILLIS);
            incoming.clear();
            var packet = incoming.poll(500, TimeUnit.MILLISECONDS);
            assertNull(packet, () -> "still receiving packets, SSRC " + packet.ssrc);
        }

        private void receiveLoop() {
            var buf = new byte[2048];
            while (!socket.isClosed()) {
                try {
                    var datagram = new DatagramPacket(buf, buf.length);
                    socket.receive(datagram);
                    var packet = new Packet(Arrays.copyOf(datagram.getData(), datagram.getLength()));
                    received.add(packet);
                    incoming.add(packet);
                } catch (IOException ignored) {
                    // closed
                }
            }
        }

        void close() {
            socket.close();
        }
    }
}
