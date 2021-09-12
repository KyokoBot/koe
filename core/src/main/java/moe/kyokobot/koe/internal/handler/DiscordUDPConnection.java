package moe.kyokobot.koe.internal.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;
import moe.kyokobot.koe.internal.dto.data.SessionDescription;
import moe.kyokobot.koe.internal.util.RTPHeaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final AtomicInteger[] seqs;

    public DiscordUDPConnection(MediaConnection voiceConnection,
                                SocketAddress serverAddress,
                                int ssrc) {
        this.connection = voiceConnection;
        this.allocator = voiceConnection.getOptions().getByteBufAllocator();
        this.serverAddress = Objects.requireNonNull(serverAddress);
        this.bootstrap = NettyBootstrapFactory.datagram(voiceConnection.getOptions());
        this.ssrc = ssrc;
        // should be a random value https://tools.ietf.org/html/rfc1889#section-5.1
        this.seqs = new AtomicInteger[256];
    }

    @Override
    public CompletionStage<InetSocketAddress> connect() {
        logger.debug("Connecting to {}...", serverAddress);

        CompletableFuture<InetSocketAddress> future = new CompletableFuture<>();
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
    public void handleSessionDescription(SessionDescription object) {
        String mode = object.mode;
        String audioCodecName = object.audioCodec;

        encryptionMode = EncryptionMode.get(mode);
        Codec audioCodec = Codec.getAudio(audioCodecName);

        if (audioCodecName != null && audioCodec == null) {
            logger.warn("Unsupported audio codec type: {}, no audio data will be polled", audioCodecName);
        }

        if (encryptionMode == null) {
            throw new IllegalStateException("Encryption mode selected by Discord is not supported by Koe or the " +
                    "protocol changed! Open an issue at https://github.com/KyokoBot/koe");
        }

        int[] keyArray = object.secretKey;
        this.secretKey = new byte[keyArray.length];

        for (int i = 0; i < secretKey.length; i++) {
            this.secretKey[i] = (byte) (keyArray[i] & 0xff);
        }

        connection.startAudioFramePolling();
        connection.startVideoFramePolling();
    }

    @Override
    public void sendFrame(byte payloadType, int timestamp, int ssrc, ByteBuf data, int len, boolean extension) {
        ByteBuf buf = createPacket(payloadType, timestamp, ssrc, data, len, extension);
        if (buf != null) {
            channel.writeAndFlush(buf);
        }
    }

    public ByteBuf createPacket(byte payloadType, int timestamp, int ssrc, ByteBuf data, int len, boolean extension) {
        if (secretKey == null) {
            return null;
        }

        boolean pad = false;
        ByteBuf buf = allocator.buffer();
        buf.clear();
        if (len < 32) {
            pad = true;
            int toAdd = 32;
            for (int i = 0; i < toAdd - 1; i++) {
                data.writeByte(0);
            }
            data.writeByte(toAdd);
            len += toAdd;
        }

        RTPHeaderWriter.writeV2(buf, payloadType, nextSeq(payloadType), timestamp, ssrc, extension, pad);
        if (encryptionMode.box(data, len, buf, secretKey)) {
            return buf;
        } else {
            logger.debug("Encryption failed!");
            buf.release();
            // handle failed encryption?
        }

        return null;
    }

    public char nextSeq(byte payloadType) {
        AtomicInteger n = seqs[(int) payloadType & 0xff];
        if (n == null) {
            n = new AtomicInteger(ThreadLocalRandom.current().nextInt() & 0xffff);
            seqs[(int) payloadType & 0xff] = n;
        }
        return (char) (n.getAndIncrement() & 0xffff);
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

            HolepunchHandler handler = new HolepunchHandler(future, connection.ssrc);
            datagramChannel.pipeline().addFirst("handler", handler);
        }
    }
}
