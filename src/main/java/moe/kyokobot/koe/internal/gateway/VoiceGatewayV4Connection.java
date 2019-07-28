package moe.kyokobot.koe.internal.gateway;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.internal.udp.RTPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VoiceGatewayV4Connection extends AbstractVoiceGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(VoiceGatewayV4Connection.class);
    private static final JsonArray SUPPORTED_CODECS;

    static {
        SUPPORTED_CODECS = new JsonArray();
        SUPPORTED_CODECS.add(new JsonObject().add("name", "opus").add("type", "audio").add("priority", 1000).add("payload_type", 120));
    }

    private final VoiceConnection connection;
    private final VoiceServerInfo voiceServerInfo;

    private volatile short ssrc;
    private volatile SocketAddress address;
    private volatile List<String> encryptionModes;
    private volatile EncryptionMode mode;
    private volatile RTPConnection rtpConnection;
    private ScheduledFuture heartbeatFuture;

    public VoiceGatewayV4Connection(VoiceConnection connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 4);

        this.connection = connection;
        this.voiceServerInfo = voiceServerInfo;
    }

    @Override
    protected void handlePayload(JsonObject object) {
        var op = object.getInt("op");

        switch (op) {
            case Op.HELLO: {
                var data = object.getObject("d");
                int interval = data.getInt("heartbeat_interval");

                logger.debug("Received HELLO, heartbeat interval: {}", interval);
                setupHeartbeats(interval);

                logger.debug("Identifying...");
                sendPayload(Op.IDENTIFY, new JsonObject()
                        .addAsString("server_id", connection.getGuildId())
                        .addAsString("user_id", connection.getClient().getClientId())
                        .add("session_id", voiceServerInfo.getSessionId())
                        .add("token", voiceServerInfo.getToken()));
                break;
            }
            case Op.READY: {
                var data = object.getObject("d");
                var port = data.getInt("port");
                var ip = data.getString("ip");
                ssrc = (short) data.getInt("ssrc");
                encryptionModes = data.getArray("modes")
                        .stream()
                        .map(o -> (String) o)
                        .collect(Collectors.toList());
                address = new InetSocketAddress(ip, port);
                logger.debug("Voice READY, ssrc: {}", ssrc);
                setupUDPConnection();
                break;
            }
            case Op.SESSION_DESCRIPTION: {
                var data = object.getObject("d");
                logger.debug("Got session description: {}", data);
            }
        }
    }

    @Override
    protected void handleClose(CloseWebSocketFrame frame) {
        if (this.heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
    }

    private void setupHeartbeats(int interval) {
        if (eventExecutor != null) {
            heartbeatFuture = eventExecutor.scheduleAtFixedRate(this::heartbeat, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    private void heartbeat() {
        sendPayload(Op.HEARTBEAT, System.currentTimeMillis());
    }

    private void setupUDPConnection() {
        mode = EncryptionMode.select(encryptionModes);
        logger.debug("Selected preferred encryption mode: {}", mode);

        rtpConnection = new RTPConnection(connection, mode, address, ssrc);
        rtpConnection.connect().thenAccept(ourAddress -> {
            logger.debug("Connected, our external address is: {}", ourAddress);
            // select protocol

            sendPayload(Op.SELECT_PROTOCOL, new JsonObject()
                    .add("protocol", "udp") // ["udp", "webrtc"]
                    .add("port", ourAddress.getPort())
                    .add("mode", mode.getName())
                    .add("data", new JsonObject()
                            .add("address", ourAddress.getAddress().getHostAddress())
                            .add("port", ourAddress.getPort())
                            .add("mode", mode.getName())));
        });
    }

    @Override
    public void close() {
        if (rtpConnection != null) {
            rtpConnection.close();
        }

        super.close();
    }
}
