package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.internal.udp.RTPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.stream.Collectors;

public class VoiceGatewayV4Connection extends AbstractVoiceGatewayConnection {
    private static final Logger logger = LoggerFactory.getLogger(VoiceGatewayV4Connection.class);

    private final VoiceConnection connection;
    private final VoiceServerInfo voiceServerInfo;

    private volatile short ssrc;
    private volatile SocketAddress address;
    private volatile List<String> encryptionModes;
    private volatile EncryptionMode mode;
    private volatile RTPConnection rtpConnection;

    public VoiceGatewayV4Connection(VoiceConnection connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 4);

        this.connection = connection;
        this.voiceServerInfo = voiceServerInfo;
    }

    @Override
    protected void handlePayload(JsonObject object) {
        logger.trace("-> {}", object);

        var op = object.getInt("op");
        var data = object.getObject("d");

        switch (op) {
            case Op.HELLO: {
                int interval = data.getInt("heartbeat_interval");
                logger.debug("Received HELLO, heartbeat interval: {}", interval);

                logger.debug("Identifying...");
                sendPayload(Op.IDENTIFY, new JsonObject()
                        .addAsString("server_id", connection.getGuildId())
                        .addAsString("user_id", connection.getClient().getClientId())
                        .add("session_id", voiceServerInfo.getSessionId())
                        .add("token", voiceServerInfo.getToken()));
                break;
            }
            case Op.READY: {
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
        }
    }

    private void setupUDPConnection() {
        mode = EncryptionMode.select(encryptionModes);
        rtpConnection = new RTPConnection(connection, mode, address, ssrc);
        rtpConnection.connect().thenAccept(ourAddress -> {
            logger.debug("Connected, our external address is: {}", ourAddress);
            // select protocol
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
