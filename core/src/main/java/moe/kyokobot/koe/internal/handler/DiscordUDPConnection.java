package moe.kyokobot.koe.internal.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.util.RTPHeaderWriter;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;

public class DiscordUDPConnection implements Closeable, ConnectionHandler<InetSocketAddress> {
    private static final Logger logger = LoggerFactory.getLogger(DiscordUDPConnection.class);

    private final MediaConnection connection;
    private final ByteBufAllocator allocator;
    private final SocketAddress serverAddress;
    private final Bootstrap bootstrap;
    private final int ssrc;

    private EncryptionMode encryptionMode;
    private DatagramChannel channel;
    private byte[] secretKey;

    private char seq;

    public DiscordUDPConnection(MediaConnection voiceConnection,
                                SocketAddress serverAddress,
                                int ssrc) {
        this.connection = voiceConnection;
        this.allocator = voiceConnection.getOptions().getByteBufAllocator();
        this.serverAddress = Objects.requireNonNull(serverAddress);
        this.bootstrap = NettyBootstrapFactory.datagram(voiceConnection.getOptions());
        this.ssrc = ssrc;
        // should be a random value https://tools.ietf.org/html/rfc1889#section-5.1
        this.seq = (char) (ThreadLocalRandom.current().nextInt() & 0xffff);
    }

    @Override
    public CompletionStage<InetSocketAddress> connect() {
        logger.debug("Connecting to {}...", serverAddress);

        var future = new CompletableFuture<InetSocketAddress>();
        bootstrap.handler(new Initializer(this, future))
                .connect(serverAddress)
                .addListener(res -> {
                    if (!res.isSuccess()) {
                        future.completeExceptionally(res.cause());
                    }
                });
        return future;
    }

    @Override
    public void close() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public void handleSessionDescription(JsonObject object) {
        var mode = object.getString("mode");
        var audioCodecName = object.getString("audio_codec");

        encryptionMode = EncryptionMode.get(mode);
        var audioCodec = Codec.getAudio(audioCodecName);

        if (audioCodecName != null && audioCodec == null) {
            logger.warn("Unsupported audio codec type: {}, no audio data will be polled", audioCodecName);
        }

        if (encryptionMode == null) {
            throw new IllegalStateException("Encryption mode selected by Discord is not supported by Koe or the " +
                    "protocol changed! Open an issue at https://github.com/KyokoBot/koe");
        }

        var keyArray = object.getArray("secret_key");
        this.secretKey = new byte[keyArray.size()];

        for (int i = 0; i < secretKey.length; i++) {
            this.secretKey[i] = (byte) (keyArray.getInt(i) & 0xff);
        }

        connection.startAudioFramePolling();
        connection.startVideoFramePolling();
    }

    @Override
    public void sendFrame(byte payloadType, int timestamp, ByteBuf data, int len, boolean extension) {
        var buf = createPacket(payloadType, timestamp, data, len, extension);
        if (buf != null) {
            channel.writeAndFlush(buf);
        }
    }

    public ByteBuf createPacket(byte payloadType, int timestamp, ByteBuf data, int len, boolean extension) {
        if (secretKey == null) {
            return null;
        }

        var buf = allocator.buffer();
        buf.clear();
        RTPHeaderWriter.writeV2(buf, payloadType, nextSeq(), timestamp, ssrc, extension);
        if (encryptionMode.box(data, len, buf, secretKey)) {
            return buf;
        } else {
            logger.debug("Encryption failed!");
            buf.release();
            // handle failed encryption?
        }

        return null;
    }

    public char nextSeq() {
        if ((seq + 1) > 0xffff) {
            seq = 0;
        } else {
            seq++;
        }

        return seq;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public int getSsrc() {
        return ssrc;
    }

    public EncryptionMode getEncryptionMode() {
        return encryptionMode;
    }

    public SocketAddress getServerAddress() {
        return serverAddress;
    }

    private static class Initializer extends ChannelInitializer<DatagramChannel> {
        private final DiscordUDPConnection connection;
        private final CompletableFuture<InetSocketAddress> future;

        private Initializer(DiscordUDPConnection connection, CompletableFuture<InetSocketAddress> future) {
            this.connection = connection;
            this.future = future;
        }

        @Override
        protected void initChannel(DatagramChannel datagramChannel) {
            connection.channel = datagramChannel;

            var handler = new HolepunchHandler(future, connection.ssrc);
            var pipeline = datagramChannel.pipeline();
            pipeline.addFirst("handler", handler);
            pipeline.addLast("rtcp", new RTCPHandler());
        }
    }
}
