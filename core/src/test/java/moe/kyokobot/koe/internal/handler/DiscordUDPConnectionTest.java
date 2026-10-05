package moe.kyokobot.koe.internal.handler;

import io.netty.buffer.Unpooled;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.internal.KoeClientImpl;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DiscordUDPConnectionTest {
    private KoeOptions options;
    private MediaConnectionImpl connection;
    private DiscordUDPConnection udp;

    @BeforeEach
    void setUp() {
        options = KoeOptions.builder().setDAVEEnabled(false).create();
        connection = new MediaConnectionImpl(new KoeClientImpl(1L, options), 2L);
        udp = new DiscordUDPConnection(connection, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9), 1);
        connection.setConnectionHandler(udp);
    }

    @AfterEach
    void tearDown() {
        connection.close();
        options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }

    @Test
    void sessionWithValidKeyProducesPackets() {
        udp.handleSessionDescription(session("aead_aes256_gcm_rtpsize", 32));

        assertTrue(canCreatePacket());
    }

    @Test
    void sessionWithInvalidKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> udp.handleSessionDescription(session("aead_aes256_gcm_rtpsize", 16)));

        assertNull(udp.getSecretKey());
        assertFalse(canCreatePacket(), "packets must not be created with a rejected key");
    }

    @Test
    void sessionWithUnsupportedModeIsRejected() {
        assertThrows(IllegalStateException.class, () -> udp.handleSessionDescription(session("made_up_mode", 32)));

        assertFalse(canCreatePacket());
    }

    private boolean canCreatePacket() {
        var frame = Unpooled.wrappedBuffer(new byte[]{1, 2, 3, 4});
        try {
            var packet = udp.createPacket(CodecType.AUDIO, (byte) 120, 0, frame, frame.readableBytes(), false);
            if (packet == null) {
                return false;
            }
            packet.release();
            return true;
        } finally {
            frame.release();
        }
    }

    private static JsonObject session(String mode, int keyLength) {
        var key = new JsonArray();
        for (int i = 0; i < keyLength; i++) key.add(i);
        return new JsonObject().add("mode", mode).add("secret_key", key);
    }
}
