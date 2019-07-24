package moe.kyokobot.koe.internal.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RTPConnection implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(RTPConnection.class);

    private final EncryptionMode encryptionMode;
    private final SocketAddress serverAddress;
    private final Bootstrap bootstrap;
    private final short ssrc;

    private DatagramChannel channel;

    public RTPConnection(VoiceConnection voiceConnection,
                         EncryptionMode encryptionMode,
                         SocketAddress serverAddress,
                         short ssrc) {
        this.encryptionMode = Objects.requireNonNull(encryptionMode);
        this.serverAddress = Objects.requireNonNull(serverAddress);
        this.bootstrap = NettyBootstrapFactory.datagram(voiceConnection.getOptions());
        this.ssrc = ssrc;
    }

    public CompletionStage<SocketAddress> connect() {
        logger.debug("Connecting to {}...", serverAddress);

        var future = new CompletableFuture<SocketAddress>();
        bootstrap.handler(new RTPInitializer(this, future))
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

    private static class RTPInitializer extends ChannelInitializer<DatagramChannel> {
        private final RTPConnection connection;
        private final CompletableFuture<SocketAddress> future;

        private RTPInitializer(RTPConnection connection, CompletableFuture<SocketAddress> future) {
            this.connection = connection;
            this.future = future;
        }

        @Override
        protected void initChannel(DatagramChannel datagramChannel) {
            connection.channel = datagramChannel;

            var handler = new HolepunchHandler(future, connection.ssrc);
            datagramChannel.pipeline().addFirst("handler", handler);
        }
    }
}
