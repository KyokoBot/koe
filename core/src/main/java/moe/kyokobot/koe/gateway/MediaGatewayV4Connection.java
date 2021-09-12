package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.dto.Codec;
import moe.kyokobot.koe.internal.dto.operation.OperationData;
import moe.kyokobot.koe.internal.dto.data.*;
import moe.kyokobot.koe.internal.dto.operation.OperationHeartbeat;
import moe.kyokobot.koe.internal.handler.DiscordUDPConnection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MediaGatewayV4Connection extends AbstractMediaGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(MediaGatewayV4Connection.class);
    private static final Codec[] SUPPORTED_CODECS;

    static {
        SUPPORTED_CODECS = new Codec[]{OpusCodec.INSTANCE.getJsonDescription()};
    }

    private int ssrc;
    private SocketAddress address;
    private String[] encryptionModes;
    private UUID rtcConnectionId;
    private ScheduledFuture<?> heartbeatFuture;

    public MediaGatewayV4Connection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 4);
    }

    @Override
    protected void identify() {
        logger.debug("Identifying...");
        sendInternalPayload(
                new OperationData(
                        Op.IDENTIFY,
                        new Identify(
                                connection.getGuildId(),
                                connection.getClient().getClientId(),
                                voiceServerInfo.getSessionId(),
                                voiceServerInfo.getToken()
                        )
                )
        );
    }

    @Override
    protected void handlePayload(OperationData op) {
        switch (op.op) {
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
                logger.debug("Got READY, ssrc: {}", ssrc);
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
                new OperationData(Op.SPEAKING,
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
        return 0;
    }

    @Override
    public int getRetransmissionSSRC() {
        return 0;
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

                sendInternalPayload(
                        new OperationData(
                                Op.CLIENT_CONNECT,
                                new ClientConnect(
                                        ssrc,
                                        0,
                                        0
                                )
                        )
                );
            });

            connection.setConnectionHandler(conn);
            logger.debug("Waiting for session description...");
        } else if (protocol.equals("webrtc")) {
            // do ICE and then generate SDP with info like above?
            throw new IllegalArgumentException("WebRTC protocol is not supported yet!");
        }
    }
}
