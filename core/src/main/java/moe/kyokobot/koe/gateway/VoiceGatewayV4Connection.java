package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.VoiceConnectionImpl;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import moe.kyokobot.koe.internal.json.JsonArray;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VoiceGatewayV4Connection extends AbstractVoiceGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(VoiceGatewayV4Connection.class);
    private static final JsonArray SUPPORTED_CODECS;

    static {
        SUPPORTED_CODECS = new JsonArray();
        SUPPORTED_CODECS.add(new JsonObject()
                .add("name", "opus")
                .add("type", "audio")
                .add("priority", 1000)
                .add("payload_type", 120));
    }

    private final VoiceConnectionImpl connection;
    private final VoiceServerInfo voiceServerInfo;

    private int ssrc;
    private SocketAddress address;
    private List<String> encryptionModes;
    private UUID rtcConnectionId;
    private ScheduledFuture heartbeatFuture;

    public VoiceGatewayV4Connection(VoiceConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 4);

        this.connection = (VoiceConnectionImpl) connection;
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
                ssrc = data.getInt("ssrc");
                encryptionModes = data.getArray("modes")
                        .stream()
                        .map(o -> (String) o)
                        .collect(Collectors.toList());
                address = new InetSocketAddress(ip, port);

                connection.getDispatcher().gatewayReady((InetSocketAddress) address, ssrc);
                logger.debug("Voice READY, ssrc: {}", ssrc);
                selectProtocol("udp");
                break;
            }
            case Op.SESSION_DESCRIPTION: {
                var data = object.getObject("d");
                logger.debug("Got session description: {}", data);

                if (connection.getConnectionHandler() == null) {
                    logger.warn("Received session description before protocol selection? (connection id = {})",
                            this.rtcConnectionId);
                    break;
                }

                sendSpeaking(false);
                sendSpeaking(true);

                connection.getDispatcher().sessionDescription(data);
                connection.getConnectionHandler().handleSessionDescription(data);
                break;
            }
            case Op.CLIENT_CONNECT : {
                var data = object.getObject("d");
                var user = data.getString("user_id");
                var audioSsrc = data.getInt("audio_ssrc", 0);
                var videoSsrc = data.getInt("video_ssrc", 0);
                connection.getDispatcher().userConnected(user, audioSsrc, videoSsrc);
                break;
            }
            case Op.CLIENT_DISCONNECT : {
                var data = object.getObject("d");
                var user = data.getString("user_id");
                connection.getDispatcher().userDisconnected(user);
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onClose(int code, @Nullable String reason, boolean remote) {
        super.onClose(code, reason, remote);
        if (this.heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
    }

    @Override
    public void sendSpeaking(boolean state) {
        sendPayload(Op.SPEAKING, new JsonObject()
                .add("speaking", state)
                .add("delay", 0)
                .add("ssrc", ssrc));
    }

    private void setupHeartbeats(int interval) {
        if (eventExecutor != null) {
            heartbeatFuture = eventExecutor.scheduleAtFixedRate(this::heartbeat, interval, interval,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void heartbeat() {
        sendPayload(Op.HEARTBEAT, System.currentTimeMillis());
    }

    private void selectProtocol(String protocol) {
        var mode = EncryptionMode.select(encryptionModes);
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

                sendPayload(Op.SELECT_PROTOCOL, new JsonObject()
                        .add("protocol", "udp")
                        .add("codecs", SUPPORTED_CODECS)
                        .add("rtc_connection_id", rtcConnectionId.toString())
                        .add("data", udpInfo)
                        .combine(udpInfo));

                sendPayload(Op.CLIENT_CONNECT, new JsonObject()
                        .add("audio_ssrc", ssrc)
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
}
