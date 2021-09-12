package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.DefaultCodecs;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.dto.Codec;
import moe.kyokobot.koe.internal.dto.StreamInfo;
import moe.kyokobot.koe.internal.dto.data.*;
import moe.kyokobot.koe.internal.dto.operation.OperationData;
import moe.kyokobot.koe.internal.dto.operation.OperationHeartbeat;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MediaGatewayV5Connection extends AbstractMediaGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(MediaGatewayV5Connection.class);

    private static final Codec[] SUPPORTED_CODECS;

    static {
        List<Codec> codecs = new ArrayList<>();
        Stream.concat(DefaultCodecs.audioCodecs.values().stream(), DefaultCodecs.videoCodecs.values().stream())
                .map(moe.kyokobot.koe.codec.Codec::getJsonDescription)
                .forEach(codecs::add);
        SUPPORTED_CODECS = (Codec[]) codecs.toArray();
    }

    private int ssrc;
    private SocketAddress address;
    private String[] encryptionModes;
    private UUID rtcConnectionId;
    private ScheduledFuture<?> heartbeatFuture;

    public MediaGatewayV5Connection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 5);
    }

    @Override
    protected void identify() {
        logger.debug("Identifying...");

        Identify.Stream[] streams = new Identify.Stream[]{
                new Identify.Stream(
                        "video",
                        "100",
                        100
                )
        };

        sendInternalPayload(
                new OperationData(
                        Op.IDENTIFY,
                        new Identify(
                                streams,
                                connection.getGuildId(),
                                connection.getClient().getClientId(),
                                voiceServerInfo.getSessionId(),
                                voiceServerInfo.getToken(),
                                true
                        )
                )
        );
    }

    @Override
    protected void handlePayload(OperationData op) {
        switch (op.opCode) {
            case Op.HELLO: {
                Hello data = (Hello) op.data;
                int interval = data.heartbeatInterval;

                logger.debug("Received HELLO, heartbeat interval: {}", interval);
                setupHeartbeats(interval);
                break;
            }
            case Op.READY: {
                Ready data = (Ready) op.data;
                int port = data.port;
                String ip = data.ip;
                ssrc = data.ssrc;
                encryptionModes = data.modes;
                address = new InetSocketAddress(ip, port);

                connection.getDispatcher().gatewayReady((InetSocketAddress) address, ssrc);
                logger.debug("Voice READY, ssrc: {}", ssrc);
                selectProtocol("udp");
                break;
            }
            case Op.SESSION_DESCRIPTION: {
                SessionDescription data = (SessionDescription) op.data;
                logger.debug("Got session description: {}", data);

                if (connection.getConnectionHandler() == null) {
                    logger.warn("Received session description before protocol selection? (connection id = {})",
                            this.rtcConnectionId);
                    break;
                }

                connection.getDispatcher().sessionDescription(data);
                connection.getConnectionHandler().handleSessionDescription(data);
                break;
            }
            case Op.CLIENT_CONNECT: {
                ClientConnect data = (ClientConnect) op.data;
                String user = data.userId;
                int audioSsrc = data.audioSsrc;
                int videoSsrc = data.videoSsrc;
                int rtxSsrc = data.rtxSsrc;
                connection.getDispatcher().userConnected(user, audioSsrc, videoSsrc, rtxSsrc);
                break;
            }
            case Op.CLIENT_DISCONNECT: {
                ClientDisconnect data = (ClientDisconnect) op.data;
                String user = data.userId;
                connection.getDispatcher().userDisconnected(user);
                break;
            }
            case Op.VIDEO_SINK_WANTS: {
                // Sent only if `video` flag was true while identifying. At time of writing this comment Discord forces
                // it to false on bots (so.. user bot time? /s) due to voice server bug that broke clients or something.
                // After receiving this opcode client can send op 12 with ssrcs for video (audio + 1)
                // and retransmission (audio + 2, not required but results in graphical issues if user joins a VC
                // or even resizes the window) and start sending video data according to received quality hint -
                // so if (d.any < 100) in this payload, the client should send video data with lowered resolution
                // and bitrate.

                sendClientConnect(true, true);
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
    public void updateSpeaking(int mask) {
        sendInternalPayload(
                new OperationData(
                        Op.SPEAKING,
                        new Speaking(
                                mask,
                                0,
                                ssrc
                        )
                )
        );
    }

    @Override
    public int getAudioSSRC() {
        return ssrc;
    }

    @Override
    public int getVideoSSRC() {
        return ssrc + 1;
    }

    @Override
    public int getRetransmissionSSRC() {
        return ssrc + 2;
    }

    private void setupHeartbeats(int interval) {
        if (eventExecutor != null) {
            heartbeatFuture = eventExecutor.scheduleAtFixedRate(this::heartbeat, interval, interval,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void heartbeat() {
        sendInternalPayload(new OperationHeartbeat());
    }

    private void sendClientConnect(boolean enableAudio, boolean enableVideo) {
        StreamInfo.Resolution resolution = new StreamInfo.Resolution();
        resolution.type = "fixed";
        resolution.width = 1280;
        resolution.height = 720;
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.type = "video";
        streamInfo.rid = "100";
        streamInfo.ssrc = ssrc + 1;
        streamInfo.rtxSsrc = ssrc + 2;
        streamInfo.active = true;
        streamInfo.quality = 100;
        streamInfo.maxBitrate = 2500000;
        streamInfo.maxFramerate = 30;
        streamInfo.maxResolution = resolution;
        StreamInfo[] streams = new StreamInfo[]{
                streamInfo
        };

        sendInternalPayload(
                new OperationData(
                        Op.CLIENT_CONNECT,
                        new ClientConnect(
                                enableAudio ? ssrc : 0,
                                enableVideo ? (ssrc + 1) : 0,
                                enableVideo ? (ssrc + 2) : 0,
                                streams
                        )
                )
        );
    }

    private void selectProtocol(String protocol) {
        String mode = EncryptionMode.select(encryptionModes);
        logger.debug("Selected preferred encryption mode: {}", mode);

        rtcConnectionId = UUID.randomUUID();
        logger.debug("Generated new connection id: {}", rtcConnectionId);

        // known values: ["udp", "webrtc"]
        if (protocol.equals("udp")) {
            DiscordUDPConnection conn = new DiscordUDPConnection(connection, address, ssrc);
            conn.connect().thenAccept(ourAddress -> {
                logger.debug("Connected, our external address is: {}", ourAddress);
                connection.getDispatcher().externalIPDiscovered(ourAddress);

                SelectProtocol.UdpInfo udpInfo = new SelectProtocol.UdpInfo(
                        ourAddress.getAddress().getHostAddress(),
                        ourAddress.getPort(),
                        mode
                );

                sendInternalPayload(
                        new OperationData(
                                Op.SELECT_PROTOCOL,
                                new SelectProtocol(
                                        "udp",
                                        SUPPORTED_CODECS,
                                        rtcConnectionId.toString(),
                                        udpInfo
                                )
                        )
                );

                this.updateSpeaking(0);

                sendClientConnect(true, false);
            });

            connection.setConnectionHandler(conn);
            logger.debug("Waiting for session description...");
        } else if (protocol.equals("webrtc")) {
            // do ICE and then generate SDP with info like above?
            throw new IllegalArgumentException("WebRTC protocol is not supported yet!");
        }
    }
}
