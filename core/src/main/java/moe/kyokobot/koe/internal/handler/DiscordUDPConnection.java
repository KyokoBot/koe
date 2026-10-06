package moe.kyokobot.koe.internal.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import moe.kyokobot.koe.codec.CodecInfo;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.CodecRegistry;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;
import moe.kyokobot.koe.internal.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.internal.util.RTPHeaderWriter;
import moe.kyokobot.libdave.EncryptorResultCode;
import moe.kyokobot.libdave.MediaType;
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
    private static final int RTP_HEADER_EXTENSION_PREAMBLE_LENGTH = 4;

    private final MediaConnectionImpl connection;
    private final ByteBufAllocator allocator;
    private final SocketAddress serverAddress;
    private final Bootstrap bootstrap;
    private final int ssrc;

    private volatile EncryptionMode encryptionMode;
    private volatile DatagramChannel channel;
    private volatile byte[] secretKey;

    private char seq;

    public DiscordUDPConnection(MediaConnectionImpl voiceConnection,
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
        CodecInstance audioCodec = null;
        if (audioCodecName != null) {
            CodecRegistry registry = connection.getOptions().getCodecRegistry();
            CodecInfo audioCodecInfo = registry.getByName(audioCodecName);
            if (audioCodecInfo != null) {
                audioCodec = audioCodecInfo.instantiate();
            } else {
                logger.warn("Unsupported audio codec type: {}, no audio data will be polled", audioCodecName);
            }
        }

        if (encryptionMode == null) {
            throw new IllegalStateException("Encryption mode selected by Discord is not supported by Koe or the " +
                    "protocol changed! Open an issue at https://github.com/KyokoBot/koe");
        }

        var keyArray = object.getArray("secret_key");
        var key = new byte[keyArray.size()];

        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (keyArray.getInt(i) & 0xff);
        }

        encryptionMode.validateKey(key);
        this.secretKey = key;

        connection.startAudioFramePolling();
        connection.startVideoFramePolling();
    }

    @Override
    public void sendFrame(CodecType codecType, byte payloadType, int timestamp, ByteBuf data, int len, boolean extension) {
        var buf = createPacket(codecType, payloadType, timestamp, data, len, extension);
        if (buf != null) {
            var ch = channel;
            if (ch != null && ch.isOpen()) {
                ch.writeAndFlush(buf);
            } else {
                buf.release();
            }
        }
    }

    public ByteBuf createPacket(CodecType codecType, byte payloadType, int timestamp, ByteBuf data, int len, boolean extension) {
        if (secretKey == null) {
            return null;
        }

        var mediaType = codecType == CodecType.AUDIO ? MediaType.AUDIO : MediaType.VIDEO;
        int extensionLength = extension ? headerExtensionLength(data, len) : 0;
        if (extensionLength < 0) {
            logger.debug("Dropping a frame with an invalid RTP header extension");
            return null;
        }

        ByteBuf buf = null;
        var inputBuffer = data;
        var inputLen = len;
        var inputBufferIsOwned = false; // true if we allocated inputBuffer (DAVE path)

        try {
            buf = allocator.directBuffer();
            buf.clear();
            var dave = connection.getDAVEManager();
            if (dave != null) {
                inputBuffer = allocator.directBuffer();
                inputBufferIsOwned = true;
                var media = data;
                if (extensionLength > 0) {
                    // The header extension is part of the RTP packet, not of the frame encrypted by DAVE.
                    inputBuffer.writeBytes(data, data.readerIndex(), extensionLength);
                    media = data.slice(data.readerIndex() + extensionLength, len - extensionLength);
                }
                var result = dave.encrypt(mediaType, ssrc, inputBuffer, media, len - extensionLength);
                inputLen = inputBuffer.readableBytes();

                if (result < 0) {
                    logger.debug("DAVE encryption failed with code {}", result);
                    return null;
                }
            } else {
                inputBuffer.retain();
                inputBufferIsOwned = true; // we retained, so we must release
            }

            RTPHeaderWriter.writeV2(buf, payloadType, nextSeq(), timestamp, ssrc, extension);
            var mode = encryptionMode;
            if (extension && mode.isRtpSize()) {
                buf.writeBytes(inputBuffer, RTP_HEADER_EXTENSION_PREAMBLE_LENGTH);
                inputLen -= RTP_HEADER_EXTENSION_PREAMBLE_LENGTH;
            }
            if (mode.box(inputBuffer, inputLen, buf, secretKey)) {
                inputBuffer.release();
                inputBufferIsOwned = false;

                var result = buf;
                buf = null; // do not release in finally — caller owns it
                return result;
            }

            logger.debug("Encryption failed!");
            return null;
        } catch (Exception e) {
            logger.debug("Encryption exception!", e);
            return null;
        } finally {
            if (buf != null && buf.refCnt() > 0) {
                buf.release();
            }

            if (inputBufferIsOwned && inputBuffer != null && inputBuffer.refCnt() > 0) {
                inputBuffer.release();
            }
        }
    }

    /**
     * @return the length of the header extension at the start of {@code data}, preamble included, or -1 if it doesn't
     * fit in {@code len} bytes
     */
    private static int headerExtensionLength(ByteBuf data, int len) {
        if (len < RTP_HEADER_EXTENSION_PREAMBLE_LENGTH) {
            return -1;
        }
        // The preamble ends with the length of the extension in 32-bit words, without the preamble itself.
        int length = RTP_HEADER_EXTENSION_PREAMBLE_LENGTH + data.getUnsignedShort(data.readerIndex() + 2) * 4;
        return length <= len ? length : -1;
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
