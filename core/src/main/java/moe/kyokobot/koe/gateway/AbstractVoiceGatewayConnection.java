package moe.kyokobot.koe.gateway;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutor;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.internal.json.JsonParser;
import moe.kyokobot.koe.internal.util.NettyFutureWrapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractVoiceGatewayConnection implements VoiceGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(AbstractVoiceGatewayConnection.class);

    private final VoiceConnection connection;
    private final VoiceServerInfo voiceServerInfo;
    private final URI websocketURI;
    private final Bootstrap bootstrap;
    private final SslContext sslContext;
    private final CompletableFuture<Void> connectFuture;

    protected EventExecutor eventExecutor;
    protected Channel channel;
    private volatile boolean open;

    public AbstractVoiceGatewayConnection(@NotNull VoiceConnection connection,
                                          @NotNull VoiceServerInfo voiceServerInfo,
                                          int version) {
        try {
            this.connection = Objects.requireNonNull(connection);
            this.voiceServerInfo = Objects.requireNonNull(voiceServerInfo);
            this.websocketURI = new URI(String.format("wss://%s:443/?v=%d",
                    voiceServerInfo.getEndpoint().replace(":80", ""), version));
            this.bootstrap = NettyBootstrapFactory.socket(connection.getOptions())
                    .handler(new WebSocketInitializer());
            this.sslContext = SslContextBuilder.forClient().build();
            this.connectFuture = new CompletableFuture<>();
        } catch (SSLException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CompletableFuture<Void> start() {
        if (connectFuture.isDone()) return connectFuture;

        var future = new CompletableFuture<Void>();
        logger.debug("Connecting to {}", websocketURI);

        var chFuture = bootstrap.connect(websocketURI.getHost(), websocketURI.getPort());
        chFuture.addListener(new NettyFutureWrapper<>(future));
        future.thenAccept(v -> this.channel = chFuture.channel());

        return connectFuture;
    }

    @Override
    public void close() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        if (!connectFuture.isDone()) {
            connectFuture.completeExceptionally(new NotYetConnectedException());
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    protected abstract void handlePayload(JsonObject object);

    protected abstract void handleClose(CloseWebSocketFrame frame);

    public abstract void sendSpeaking(boolean state);

    protected void sendPayload(int op, Object d) {
        sendRaw(new JsonObject().add("op", op).add("d", d));
    }

    protected void sendRaw(JsonObject object) {
        if (channel != null && channel.isOpen()) {
            var data = object.toString();
            logger.trace("<- {}", data);
            channel.writeAndFlush(new TextWebSocketFrame(data));
        }
    }

    private class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;

        WebSocketClientHandler() {
            this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13,
                    null, false, EmptyHttpHeaders.INSTANCE, 1280000);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            eventExecutor = ctx.executor();
            handshaker.handshake(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            var ch = ctx.channel();

            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                    AbstractVoiceGatewayConnection.this.open = true;
                    connectFuture.complete(null);
                } catch (WebSocketHandshakeException e) {
                    connectFuture.completeExceptionally(e);
                }
                return;
            }

            if (msg instanceof FullHttpResponse) {
                var response = (FullHttpResponse) msg;
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse (getStatus=" + response.status() +
                                ", content=" + response.content().toString(StandardCharsets.UTF_8) + ")");
            }

            if (msg instanceof TextWebSocketFrame) {
                var frame = (TextWebSocketFrame) msg;
                var object = JsonParser.object().from(frame.content());
                logger.trace("-> {}", object);
                frame.release();
                handlePayload(object);
            } else if (msg instanceof CloseWebSocketFrame) {
                var frame = (CloseWebSocketFrame) msg;
                if (logger.isDebugEnabled()) {
                    logger.debug("Websocket closed, code: {}, reason: {}", frame.statusCode(), frame.reasonText());
                }
                AbstractVoiceGatewayConnection.this.open = false;
                handleClose(frame);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (!connectFuture.isDone()) {
                connectFuture.completeExceptionally(cause);
            }

            ctx.close();
        }
    }

    private class WebSocketInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            var pipeline = ch.pipeline();
            var engine = sslContext.newEngine(ch.alloc());
            pipeline.addLast("ssl", new SslHandler(engine));
            pipeline.addLast("http-codec", new HttpClientCodec());
            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
            pipeline.addLast("handler", new WebSocketClientHandler());
        }
    }
}
