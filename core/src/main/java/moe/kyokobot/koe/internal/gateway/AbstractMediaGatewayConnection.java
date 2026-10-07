package moe.kyokobot.koe.internal.gateway;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutor;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.CodecInfo;
import moe.kyokobot.koe.gateway.CloseCode;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.gateway.MediaValve;
import moe.kyokobot.koe.gateway.Op;
import moe.kyokobot.koe.internal.BuildConstants;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;
import moe.kyokobot.koe.internal.crypto.CipherPolicies;
import moe.kyokobot.koe.internal.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.internal.json.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public abstract class AbstractMediaGatewayConnection implements MediaGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMediaGatewayConnection.class);

    protected final MediaConnectionImpl connection;
    protected final VoiceServerInfo voiceServerInfo;
    protected final URI websocketURI;
    protected final Bootstrap bootstrap;
    protected final SslContext sslContext;
    protected final ByteBufAllocator allocator;
    protected final CompletableFuture<Void> connectFuture;
    protected final int version;

    protected EventExecutor eventExecutor;
    protected Channel channel;
    protected int connectAttempt = 0;
    protected boolean resumable = false;
    private boolean started = false;
    private boolean open = false;
    private boolean closed = false;
    private int ssrc;
    private UUID rtcConnectionId;
    private ScheduledFuture<?> heartbeatFuture;
    private long lastHeartbeatSent;
    private long ping;

    public AbstractMediaGatewayConnection(@NotNull MediaConnectionImpl connection,
                                          @NotNull VoiceServerInfo voiceServerInfo,
                                          int version) {
        try {
            var endpoint = stripScheme(voiceServerInfo.getEndpoint());

            if (connection.getOptions().isEnableWSSPortOverride()) {
                endpoint = stripPort80(endpoint);
            }

            this.connection = Objects.requireNonNull(connection);
            this.voiceServerInfo = Objects.requireNonNull(voiceServerInfo);
            this.version = version;

            this.websocketURI = new URI(String.format("wss://%s/?v=%d", endpoint, version));
            this.bootstrap = NettyBootstrapFactory.socket(connection.getOptions())
                    .handler(new WebSocketInitializer());

            var sslBuilder = SslContextBuilder.forClient();
            if (!connection.getOptions().isVerifyWSSHostname()) {
                sslBuilder.endpointIdentificationAlgorithm(null);
            }
            this.sslContext = sslBuilder.build();

            this.allocator = connection.getOptions().getByteBufAllocator();
            this.connectFuture = new CompletableFuture<>();
        } catch (SSLException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    @Override
    public MediaValve getValve() {
        return null;
    }

    @Override
    public CompletableFuture<Void> start() {
        if (!started) {
            started = true;
            connect();
        }

        return connectFuture;
    }

    private void connect() {
        logger.debug("Connecting to {}, attempt {}/3", websocketURI, connectAttempt);
        open = false;
        closed = false;

        var chFuture = bootstrap.connect(websocketURI.getHost(), websocketURI.getPort() == -1 ? 443 : websocketURI.getPort());
        var ch = chFuture.channel();
        this.channel = ch;
        chFuture.addListener(future -> {
            if (!future.isSuccess()) {
                onConnectFailure(ch, future.cause());
            }
        });

        long timeout = connection.getOptions().getGatewayConnectTimeout();
        if (timeout > 0) {
            ch.eventLoop().schedule(() -> {
                if (!open) {
                    onConnectFailure(ch, new TimeoutException("Voice gateway connection timed out after " + timeout + " ms"));
                }
            }, timeout, TimeUnit.MILLISECONDS);
        }
    }

    private void onConnectFailure(Channel ch, Throwable cause) {
        // Ignore failures of channels which were already replaced by a newer connection attempt.
        if (ch != channel || closed) {
            return;
        }

        logger.warn("Failed to connect to the voice gateway (Guild ID={})", connection.getGuildId(), cause);
        connection.getDispatcher().gatewayError(cause);

        if (!connectFuture.isDone()) {
            connectFuture.completeExceptionally(cause);
        }

        close(CloseCode.ABNORMAL_CLOSURE, cause.getMessage());
    }

    @Override
    public void close(int code, @Nullable String reason) {
        var ch = channel;
        if (ch != null && ch.isOpen()) {
            // Code 1006 must never be sent, according to RFC 6455
            if (code != 1006) {
                ch.writeAndFlush(new CloseWebSocketFrame(code, reason));
            }
            ch.close();
        }

        onClose(code, reason, false);
    }

    @Override
    public void reconnect() {
        if (open) {
            close(CloseCode.KOE_RECONNECT, "Koe: Reconnect");
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getPing() {
        return this.ping;
    }

    @Override
    public void updateSpeaking(int mask) {
        sendInternalPayload(Op.SPEAKING, new JsonObject()
                .add("speaking", mask)
                .add("delay", 0)
                .add("ssrc", Integer.toUnsignedLong(ssrc)));
    }

    protected void identify() {
        logger.debug("Identifying...");
        sendInternalPayload(Op.IDENTIFY, identifyPayload());
    }

    protected JsonObject identifyPayload() {
        return new JsonObject()
                .addAsString("server_id", connection.getGuildId())
                .addAsString("user_id", connection.getClient().getClientId())
                .add("session_id", voiceServerInfo.getSessionId())
                .add("token", voiceServerInfo.getToken());
    }

    protected void resume() {
        logger.debug("Resuming...");
        sendInternalPayload(Op.RESUME, resumePayload());
    }

    protected JsonObject resumePayload() {
        return new JsonObject()
                .addAsString("server_id", connection.getGuildId())
                .add("session_id", voiceServerInfo.getSessionId())
                .add("token", voiceServerInfo.getToken());
    }

    protected Object heartbeatPayload() {
        return System.currentTimeMillis();
    }

    /**
     * @return the codecs announced when selecting the protocol
     */
    protected Collection<CodecInfo> codecs() {
        return connection.getOptions().getCodecRegistry().getAudioCodecs();
    }

    /**
     * Handles the opcodes all gateway versions share, newer versions override it to handle their own opcodes first.
     */
    protected void handlePayload(JsonObject object) {
        var op = object.getInt("op");

        switch (op) {
            case Op.HELLO: {
                var data = object.getObject("d");
                int interval = data.getInt("heartbeat_interval");

                logger.debug("Received HELLO, heartbeat interval: {}", interval);
                setupHeartbeats(interval);
                break;
            }
            case Op.READY: {
                resumable = true;

                var data = object.getObject("d");
                var port = data.getInt("port");
                var ip = data.getString("ip");
                ssrc = data.getInt("ssrc");
                var encryptionModes = data.getArray("modes")
                        .stream()
                        .map(o -> (String) o)
                        .collect(Collectors.toList());
                var address = new InetSocketAddress(ip, port);

                connection.getDispatcher().gatewayReady(address, ssrc);
                logger.debug("Voice READY, ssrc: {}", ssrc);
                onReady();
                if (version >= 6) {
                    // Only sent on request, the official client asks right after READY too.
                    sendInternalPayload(Op.VOICE_BACKEND_VERSION, new JsonObject());
                }
                selectProtocol("udp", address, encryptionModes);
                break;
            }
            case Op.SESSION_DESCRIPTION: {
                var data = object.getObject("d");
                connectAttempt = 0;
                logger.debug("Got session description: {}", data);

                if (connection.getConnectionHandler() == null) {
                    logger.warn("Received session description before protocol selection? (connection id = {})",
                            this.rtcConnectionId);
                    break;
                }

                connection.getDispatcher().sessionDescription(data);
                connection.getConnectionHandler().handleSessionDescription(data);
                onSessionDescription(data);
                break;
            }
            case Op.HEARTBEAT_ACK: {
                this.ping = System.currentTimeMillis() - this.lastHeartbeatSent;
                break;
            }
            case Op.VOICE_BACKEND_VERSION: {
                logger.debug("Voice backend version: {}", object.getObject("d"));
                break;
            }
            case Op.RESUMED: {
                connectAttempt = 0;

                logger.debug("Resumed successfully");
                break;
            }
            case Op.VIDEO: {
                onStreamsChanged(object);

                var data = object.getObject("d");
                var user = data.getString("user_id");
                var audioSsrc = data.getInt("audio_ssrc", 0);
                var videoSsrc = data.getInt("video_ssrc", 0);
                var rtxSsrc = data.getInt("rtx_ssrc", 0);
                connection.getDispatcher().userStreamsChanged(user, audioSsrc, videoSsrc, rtxSsrc);
                break;
            }
            case Op.CLIENT_CONNECT: {
                var data = object.getObject("d");
                var userIds = data.getArray("user_ids");

                List<String> userIdList = userIds.stream()
                        .map(o -> (String) o)
                        .collect(Collectors.toList());
                connection.getDispatcher().usersConnected(userIdList);
                onUsersConnected(userIdList);
                break;
            }
            case Op.CLIENT_DISCONNECT: {
                var data = object.getObject("d");
                var user = data.getString("user_id");
                onUserDisconnected(user);
                connection.getDispatcher().userDisconnected(user);
                break;
            }
            default:
                break;
        }
    }

    protected void handlePayload(ByteBuf byteBuf) {
        // no binary messages before v8
    }

    /**
     * Called after READY was dispatched, right before selecting the protocol.
     */
    protected void onReady() {
        //
    }

    /**
     * Called after the connection handler got the session description.
     */
    protected void onSessionDescription(JsonObject data) {
        //
    }

    /**
     * Called with the whole VIDEO payload, before the listeners are notified.
     */
    protected void onStreamsChanged(JsonObject payload) {
        //
    }

    /**
     * Called after the listeners were notified.
     */
    protected void onUsersConnected(List<String> userIds) {
        //
    }

    /**
     * Called before the listeners are notified.
     */
    protected void onUserDisconnected(String userId) {
        //
    }

    /**
     * Called right after SELECT_PROTOCOL was sent, before announcing our streams.
     */
    protected void onProtocolSelected() {
        //
    }

    private void setupHeartbeats(int interval) {
        if (eventExecutor != null) {
            heartbeatFuture = eventExecutor.scheduleAtFixedRate(this::heartbeat, interval, interval,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void heartbeat() {
        this.lastHeartbeatSent = System.currentTimeMillis();
        sendInternalPayload(Op.HEARTBEAT, heartbeatPayload());
    }

    private void selectProtocol(String protocol, InetSocketAddress address, List<String> encryptionModes) {
        var mode = EncryptionMode.select(encryptionModes, CipherPolicies.forOptions(connection.getOptions()));
        logger.debug("Selected preferred encryption mode: {}", mode);

        rtcConnectionId = UUID.randomUUID();
        logger.debug("Generated new connection id: {}", rtcConnectionId);

        // known values: ["udp", "webrtc"]
        if (protocol.equals("udp")) {
            var conn = new DiscordUDPConnection(connection, address, ssrc);
            conn.connect().thenAccept(ourAddress -> {
                logger.debug("Connected, our external address is: {}", ourAddress);
                connection.getDispatcher().externalIPDiscovered(ourAddress);

                var udpInfo = new JsonObject()
                        .add("address", ourAddress.getAddress().getHostAddress())
                        .add("port", ourAddress.getPort())
                        .add("mode", mode);

                var codecs = new JsonArray();
                codecs().stream()
                        .map(codecInfo -> codecInfo.toJson())
                        .forEach(codecs::add);

                sendInternalPayload(Op.SELECT_PROTOCOL, new JsonObject()
                        .add("protocol", "udp")
                        .add("codecs", codecs)
                        .add("rtc_connection_id", rtcConnectionId.toString())
                        .add("data", udpInfo)
                        .combine(udpInfo));

                onProtocolSelected();

                sendInternalPayload(Op.VIDEO, new JsonObject()
                        .add("audio_ssrc", Integer.toUnsignedLong(ssrc))
                        .add("video_ssrc", 0)
                        .add("rtx_ssrc", 0));
            });

            connection.setConnectionHandler(conn);
            logger.debug("Waiting for session description...");
        } else if (protocol.equals("webrtc")) {
            // do ICE and then generate SDP with info like above?
            throw new IllegalArgumentException("WebRTC protocol is not supported yet!");
        }
    }

    protected void onClose(int code, @Nullable String reason, boolean remote) {
        closeSession(code, reason, remote);

        var heartbeat = heartbeatFuture;
        if (heartbeat != null) {
            heartbeat.cancel(true);
        }
    }

    private void closeSession(int code, @Nullable String reason, boolean remote) {
        if (!closed) {
            closed = true;
            open = false;

            if (!connectFuture.isDone()) {
                connectFuture.completeExceptionally(new IOException(String.format(
                        "Voice gateway connection closed before it was established (code=%d, reason=%s)", code, reason)));
            }

            // Only sessions that have been established make sense to resume.
            if (connectFuture.isCompletedExceptionally()) {
                return;
            }

            if (connectAttempt < 3) {
                switch (code) {
                    case CloseCode.GOING_AWAY:
                    case CloseCode.ABNORMAL_CLOSURE:
                    case CloseCode.INTERNAL_ERROR:
                    case CloseCode.UNKNOWN_OPCODE:
                    case CloseCode.FAILED_TO_DECODE_PAYLOAD:
                    case CloseCode.NOT_AUTHENTICATED:
                    case CloseCode.ALREADY_AUTHENTICATED:
                    case CloseCode.SESSION_TIMEOUT:
                    case CloseCode.UNKNOWN_PROTOCOL:
                    case CloseCode.VOICE_SERVER_CRASHED:
                    case CloseCode.UNKNOWN_ENCRYPTION_MODE:
                    case CloseCode.BAD_REQUEST:
                    case CloseCode.KOE_RECONNECT:
                        connectAttempt++;
                        connect();
                        break;
                    case CloseCode.SESSION_NO_LONGER_VALID:
                        if (resumable) {
                            // The server forgot the session, so try to start a new one instead of resuming.
                            resumable = false;
                            connectAttempt++;
                            connect();
                        } else {
                            reportClose(code, reason, remote);
                        }
                        break;
                    default:
                        reportClose(code, reason, remote);
                        break;
                }
            } else {
                reportClose(code, reason, remote);
            }
        }
    }

    private void reportClose(int code, @Nullable String reason, boolean remote) {
        connection.getDispatcher().gatewayClosed(code, reason, remote);
        if (code == CloseCode.SESSION_NO_LONGER_VALID) {
            connection.getDispatcher().sessionLost(code, reason);
        }
    }

    public void sendInternalPayload(int op, Object d) {
        sendRaw(new JsonObject().add("op", op).add("d", d));
    }

    public void sendInternalBinPayload(int op, byte[] d) {
        var buf = this.allocator.buffer(1 + d.length);
        buf.writeByte(op);
        buf.writeBytes(d);
        this.sendRawBin(buf);
    }

    protected void sendRaw(JsonObject object) {
        if (channel != null && channel.isOpen()) {
            var data = object.toString();
            logger.trace("<- {}", data);
            channel.writeAndFlush(new TextWebSocketFrame(data));
        }
    }

    protected void sendRawBin(ByteBuf buffer) {
        if (channel != null && channel.isOpen()) {
            logger.trace("<- <binary: {} readable bytes>", buffer.readableBytes());
            channel.writeAndFlush(new BinaryWebSocketFrame(buffer));
        }
    }

    private static final HttpHeaders HANDSHAKE_HEADERS;

    static {
        HANDSHAKE_HEADERS = new DefaultHttpHeaders();
        HANDSHAKE_HEADERS.set(HttpHeaderNames.USER_AGENT,
                "DiscordBot (https://github.com/KyokoBot/koe, " + BuildConstants.VERSION + ")");
    }

    private class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;

        WebSocketClientHandler() {
            this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13,
                    null, false, HANDSHAKE_HEADERS, 1280000);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            eventExecutor = ctx.executor();
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(@NotNull ChannelHandlerContext ctx) {
            if (ctx.channel() == channel) {
                close(1006, "Abnormal closure");
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            var ch = ctx.channel();
            if (ch != channel) {
                return;
            }

            if (!handshaker.isHandshakeComplete()) {
                if (msg instanceof FullHttpResponse) {
                    try {
                        handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                        AbstractMediaGatewayConnection.this.open = true;

                        connectFuture.complete(null);

                        if (resumable) {
                            AbstractMediaGatewayConnection.this.resume();
                        } else {
                            AbstractMediaGatewayConnection.this.identify();
                        }
                    } catch (WebSocketHandshakeException e) {
                        onConnectFailure(ch, e);
                    }
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
            } else if (msg instanceof BinaryWebSocketFrame) {
                var frame = (BinaryWebSocketFrame) msg;
                logger.trace("-> <binary: {} readable bytes>", frame.content().readableBytes());
                handlePayload(frame.content());
            } else if (msg instanceof CloseWebSocketFrame) {
                var frame = (CloseWebSocketFrame) msg;
                logger.debug("Websocket closed, code: {}, reason: {}", frame.statusCode(), frame.reasonText());
                AbstractMediaGatewayConnection.this.open = false;
                onClose(frame.statusCode(), frame.reasonText(), true);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (ctx.channel() != channel) {
                ctx.close();
                return;
            }

            if (!connectFuture.isDone()) {
                connectFuture.completeExceptionally(cause);
            }

            connection.getDispatcher().gatewayError(cause);
            logger.warn("Exception occurred in WebSocket client handler (Guild ID={})", connection.getGuildId(), cause);

            close(4000, "Internal error");
            ctx.close();
        }
    }

    private class WebSocketInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            var pipeline = ch.pipeline();
            var engine = connection.getOptions().isVerifyWSSHostname()
                    ? sslContext.newEngine(ch.alloc(), websocketURI.getHost(), websocketURI.getPort() == -1 ? 443 : websocketURI.getPort())
                    : sslContext.newEngine(ch.alloc());
            pipeline.addLast("ssl", new SslHandler(engine));
            pipeline.addLast("http-codec", new HttpClientCodec());
            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
            pipeline.addLast("handler", new WebSocketClientHandler());
        }
    }

    /**
     * Strips the scheme from endpoints passed as a URL (e.g. "wss://host:443") instead of "host:port".
     */
    protected static String stripScheme(String endpoint) {
        int idx = endpoint.indexOf("://");
        if (idx != -1) {
            return endpoint.substring(idx + 3);
        }

        return endpoint;
    }

    protected static String stripPort80(String endpoint) {
        if (endpoint.endsWith(":80")) {
            return endpoint.substring(0, endpoint.length() - 3);
        }

        return endpoint;
    }
}
