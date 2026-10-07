package moe.kyokobot.koe.internal.gateway;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.gateway.Op;
import moe.kyokobot.koe.internal.DAVEManager;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Adds message buffering, the server re-delivers messages after the last acknowledged sequence number on resume.
 * Koe implements DAVE only on this version.
 */
public class MediaGatewayV8Connection extends MediaGatewayV5Connection {
    private static final Logger logger = LoggerFactory.getLogger(MediaGatewayV8Connection.class);

    private final DAVEManager daveManager;
    private int sequence = -1; // -1 means we haven't received any sequence yet

    public MediaGatewayV8Connection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 8);
        this.daveManager = connection.createDAVEManager();
    }

    @Nullable
    public DAVEManager getDAVEManager() {
        return this.daveManager;
    }

    @Override
    protected JsonObject identifyPayload() {
        int maxDAVEVersion = 0;
        if (daveManager != null) {
            maxDAVEVersion = daveManager.getMaxDAVEProtocolVersion();
        }

        logger.debug("Max DAVE Protocol Version: {}", maxDAVEVersion);

        return super.identifyPayload()
                .addAsString("channel_id", voiceServerInfo.getChannelId())
                .add("max_dave_protocol_version", maxDAVEVersion);
    }

    @Override
    protected JsonObject resumePayload() {
        return super.resumePayload().add("seq_ack", sequence);
    }

    @Override
    protected Object heartbeatPayload() {
        return new JsonObject()
                .add("t", System.currentTimeMillis())
                .add("seq_ack", sequence);
    }

    @Override
    protected void handlePayload(JsonObject object) {
        var op = object.getInt("op");

        if (object.has("seq")) {
            sequence = object.getInt("seq");
        }

        switch (op) {
            case Op.SECURE_FRAMES_PREPARE_PROTOCOL_TRANSITION: {
                var data = object.getObject("d");
                logger.debug("Secure frames prepare protocol transition: {}", data);
                var transitionId = data.getInt("transition_id");
                var protocolVersion = data.getInt("protocol_version");

                if (daveManager != null) {
                    daveManager.handleSecureFramesPrepareProtocolTransition(transitionId, protocolVersion);
                }

                break;
            }
            case Op.SECURE_FRAMES_EXECUTE_TRANSITION: {
                var data = object.getObject("d");
                logger.debug("Secure frames execute transition: {}", data);
                var transitionId = data.getInt("transition_id");

                if (daveManager != null) {
                    daveManager.handleSecureFramesExecuteTransition(transitionId);
                }

                break;
            }
            case Op.SECURE_FRAMES_READY_FOR_TRANSITION: {
                var data = object.getObject("d");
                logger.debug("Secure frames ready for transition: {}", data);

                break;
            }
            case Op.SECURE_FRAMES_PREPARE_EPOCH: {
                var data = object.getObject("d");
                logger.debug("Secure frames prepare epoch: {}", data);
                var epoch = data.getInt("epoch");
                var protocolVersion = data.getInt("protocol_version");

                if (daveManager != null) {
                    daveManager.handleSecureFramesPrepareEpoch(Integer.toString(epoch), protocolVersion);
                }

                break;
            }
            default:
                super.handlePayload(object);
                break;
        }
    }

    @Override
    protected void handlePayload(ByteBuf byteBuf) {
        var seq = byteBuf.readShort();
        var op = byteBuf.readByte();

        sequence = seq;
        if (daveManager == null) {
            return;
        }

        switch (op) {
            case Op.MLS_WELCOME: {
                var transId = byteBuf.readUnsignedShort();
                var payload = new byte[byteBuf.readableBytes()];
                logger.debug("MLS welcome, transId: {} payload: <{} bytes>", transId, payload.length);
                byteBuf.readBytes(payload);
                daveManager.handleMLSWelcome(transId, payload);

                break;
            }
            case Op.MLS_EXTERNAL_SENDER_PACKAGE: {
                logger.debug("MLS external sender package");

                var payload = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(payload);
                daveManager.handleMLSExternalSender(payload);

                break;
            }
//            case Op.MLS_KEY_PACKAGE: {
//                logger.debug("MLS key package");
//                break;
//            }
            case Op.MLS_PROPOSALS: {
                logger.debug("MLS proposals");

                var payload = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(payload);
                daveManager.handleMLSProposals(payload);
                break;
            }
//            case Op.MLS_COMMIT_WELCOME: {
//                logger.debug("MLS commit welcome");
//                break;
//            }
            case Op.MLS_PREPARE_COMMIT_TRANSITION: {
                var transId = byteBuf.readUnsignedShort();
                var payload = new byte[byteBuf.readableBytes()];
                logger.debug("MLS prepare commit transition, transId: {} payload: <{} bytes>", transId, payload.length);

                byteBuf.readBytes(payload);
                daveManager.handleMLSPrepareCommitTransition(transId, payload);
                break;
            }
            default:
                logger.debug("Received unknown binary payload OP: {}", op);
                break;
        }
    }

    @Override
    protected void onSessionDescription(JsonObject data) {
        if (daveManager != null) {
            daveManager.handleSessionDescription(data, voiceServerInfo.getChannelId());
        }
    }

    @Override
    protected void onUsersConnected(List<String> userIds) {
        if (daveManager != null) {
            daveManager.addUsers(userIds);
        }
    }

    @Override
    protected void onUserDisconnected(String userId) {
        super.onUserDisconnected(userId);
        if (daveManager != null) {
            daveManager.removeUser(userId);
        }
    }

    @Override
    public void sendMLSKeyPackage(byte[] keyPackage) {
        sendInternalBinPayload(Op.MLS_KEY_PACKAGE, keyPackage);
    }

    @Override
    public void sendMLSCommitWelcome(byte[] commitWelcome) {
        sendInternalBinPayload(Op.MLS_COMMIT_WELCOME, commitWelcome);
    }

    @Override
    public void sendMLSInvalidCommitWelcome(int transitionId) {
        sendInternalPayload(Op.MLS_INVALID_COMMIT_WELCOME, new JsonObject()
                .add("transition_id", transitionId));
    }

    @Override
    public void sendSecureFramesReadyForTransition(int transitionId) {
        sendInternalPayload(Op.SECURE_FRAMES_READY_FOR_TRANSITION, new JsonObject()
                .add("transition_id", transitionId));
    }
}
