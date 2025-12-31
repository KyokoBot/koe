package moe.kyokobot.koe.internal;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.libdave.KeyRatchet;
import moe.kyokobot.libdave.MediaType;
import moe.kyokobot.libdave.Session;
import moe.kyokobot.libdave.netty.NettyDaveFactory;
import moe.kyokobot.libdave.netty.NettyEncryptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DAVEManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DAVEManager.class);

    private static final String MLS_NEW_GROUP_EPOCH = "1";
    private static final int INIT_TRANSITION_ID = 0;

    private final MediaConnectionImpl connection;
    private final NettyDaveFactory factory;
    private final Session daveSession;
    private final Set<String> recognizedUserIds = new HashSet<>();
    private final Map<Integer, Integer> pendingTransitions = new HashMap<>();
    private final int maxProtocolVersion;

    private NettyEncryptor selfEncryptor;
    private @Nullable KeyRatchet selfKeyRatchet = null;
    private String selfUserIdString;
    private long mlsGroupId = 0;

    private int currentProtocolVersion = 0;

    DAVEManager(@NotNull MediaConnectionImpl connection, @NotNull NettyDaveFactory factory) {
        this.connection = connection;
        this.factory = factory;
        this.daveSession = factory.createSession("", "", this::mlsFailureCallback);
        this.selfEncryptor = factory.fromEncryptor(factory.createEncryptor());
        this.maxProtocolVersion = factory.maxSupportedProtocolVersion();
        this.selfUserIdString = String.valueOf(connection.getClient().getClientId());
    }

    public int getMaxDAVEProtocolVersion() {
        return maxProtocolVersion;
    }

    public void addUser(String userId) {
        recognizedUserIds.add(userId);
        setupKeyRatchetForUser(userId, currentProtocolVersion);
    }

    public void removeUser(String userId) {
        recognizedUserIds.remove(userId);
    }

    public ByteBuf encrypt(MediaType mediaType, int ssrc, ByteBuf output, ByteBuf input, int size) {
        if (mediaType == MediaType.AUDIO && size == 3) {
            input.markReaderIndex();
            var b1 = input.readByte() == OpusCodec.SILENCE_FRAME[0];
            var b2 = input.readByte() == OpusCodec.SILENCE_FRAME[1];
            var b3 = input.readByte() == OpusCodec.SILENCE_FRAME[2];
            var isSilence = b1 && b2 && b3;
            input.resetReaderIndex();

            if (isSilence) {
                return input;
            }
        }

        output.ensureWritable(this.selfEncryptor.getMaxCiphertextByteSize(mediaType, size));
        var _result = this.selfEncryptor.encrypt(mediaType, ssrc, input, output);
        return output;
    }

    public void handleSessionDescription(@NotNull JsonObject session, long mlsGroupId) {
        int protocolVersion = session.getInt("dave_protocol_version", 0);
        this.mlsGroupId = mlsGroupId;
        daveProtocolInit(protocolVersion);
    }

    public void handleSecureFramesPrepareProtocolTransition(int transitionId, int newProtocolVersion) {
        prepareRatchets(transitionId, newProtocolVersion);
        if (transitionId != 0) {
            sendSecureFramesReadyForTransition(transitionId);
        }
    }

    public void handleSecureFramesExecuteTransition(int transitionId) {
        executeTransition(transitionId);
    }

    public void handleSecureFramesPrepareEpoch(String epoch, int protocolVersion) {
        prepareEpoch(epoch, protocolVersion);

        if (MLS_NEW_GROUP_EPOCH.equals(epoch)) {
            sendMLSKeyPackage();
        }
    }

    public void handleMLSExternalSender(byte[] payload) {
        daveSession.setExternalSender(payload);
    }

    public void handleMLSProposals(byte[] payload) {
        var userIds = recognizedUserIdArray();

        var commitWelcome = daveSession.processProposals(payload, userIds);
        if (commitWelcome != null) {
            sendMLSCommitWelcome(commitWelcome);
        }
    }

    public void handleMLSPrepareCommitTransition(int transitionId, byte[] commit) {
        var result = daveSession.processCommit(commit);
        if (result.isIgnored()) return;


        if (result.isFailed()) {
            sendMLSInvalidCommitWelcome(transitionId);
            daveProtocolInit(daveSession.getProtocolVersion());
            return;
        }

        prepareRatchets(transitionId, daveSession.getProtocolVersion());
        if (transitionId != 0) {
            sendSecureFramesReadyForTransition(transitionId);
        }
    }

    public void handleMLSWelcome(int transitionId, byte[] welcome) {
        var roster = daveSession.processWelcome(welcome, recognizedUserIdArray());

        if (roster == null) {
            sendMLSInvalidCommitWelcome(transitionId);
            sendMLSKeyPackage();
        }

        prepareRatchets(transitionId, daveSession.getProtocolVersion());
        if (transitionId != 0) {
            sendSecureFramesReadyForTransition(transitionId);
        }
    }

    private void daveProtocolInit(int protocolVersion) {
        logger.debug("DAVE Init - Protocol version={}, MLS Group ID={}", protocolVersion, mlsGroupId);
        if (protocolVersion > 0) {
            prepareEpoch(MLS_NEW_GROUP_EPOCH, protocolVersion);
            sendMLSKeyPackage();
        } else {
            prepareRatchets(INIT_TRANSITION_ID, protocolVersion);
            executeTransition(INIT_TRANSITION_ID);
        }
    }


    private void prepareEpoch(String epoch, int protocolVersion) {
        if (MLS_NEW_GROUP_EPOCH.equals(epoch)) {
            daveSession.init(protocolVersion, mlsGroupId, selfUserIdString);
        }
    }

    private void setupKeyRatchetForUser(String uid, int protocolVersion) {
        var keyRatchet = makeKeyRatchetForUser(uid, protocolVersion);
        setSelfKeyRatchet(keyRatchet);
    }

    @Nullable
    private KeyRatchet makeKeyRatchetForUser(String uid, int protocolVersion) {
        if (protocolVersion == 0) {
            return null;
        }

        return daveSession.getKeyRatchet(uid);
    }

    private void prepareRatchets(int transitionId, int protocolVersion) {
        for (var uid : recognizedUserIds) {
            if (selfUserIdString.equals(uid)) {
                continue;
            }

            setupKeyRatchetForUser(uid, protocolVersion);
        }

        if (transitionId == INIT_TRANSITION_ID) {
            setupKeyRatchetForUser(selfUserIdString, protocolVersion);
        } else {
            pendingTransitions.put(transitionId, protocolVersion);
        }

        currentProtocolVersion = protocolVersion;
    }

    private void executeTransition(int transitionId) {
        var protocolVersion = pendingTransitions.remove(transitionId);
        if (protocolVersion == null) {
            return;
        }

        if (protocolVersion == 0) {
            daveSession.reset();
        }

        setupKeyRatchetForUser(selfUserIdString, protocolVersion);
        logger.debug("Transition executed: ID={}, Protocol version={}", transitionId, protocolVersion);
    }

    private void sendMLSKeyPackage() {
        var gateway = connection.getGatewayConnection();
        if (gateway != null) {
            var keyPackage = daveSession.getMarshalledKeyPackage();
            gateway.sendMLSKeyPackage(keyPackage);
        }
    }

    private void sendMLSCommitWelcome(byte[] commitWelcome) {
        var gateway = connection.getGatewayConnection();
        if (gateway != null) {
            gateway.sendMLSCommitWelcome(commitWelcome);
        }
    }

    private void sendMLSInvalidCommitWelcome(int transitionId) {
        var gateway = connection.getGatewayConnection();
        if (gateway != null) {
            gateway.sendMLSInvalidCommitWelcome(transitionId);
        }
    }

    private void sendSecureFramesReadyForTransition(int transitionId) {
        var gateway = connection.getGatewayConnection();
        if (gateway != null) {
            gateway.sendSecureFramesReadyForTransition(transitionId);
        }
    }

    private void mlsFailureCallback(String source, String reason) {
        logger.warn("MLS Failure - Source: {}, Reason: {}", source, reason);
    }

    @Override
    public void close() throws Exception {
        daveSession.close();
        selfEncryptor.close();
    }

    private String[] recognizedUserIdArray() {
        var userIds = new String[recognizedUserIds.size() + 1];
        int i = 0;
        for (var uid : recognizedUserIds) {
            userIds[i++] = uid;
        }
        userIds[i] = selfUserIdString;

        return userIds;
    }

    public void setSelfKeyRatchet(KeyRatchet selfKeyRatchet) {
        if (this.selfKeyRatchet != null) {
            this.selfKeyRatchet.close();
        }

        this.selfKeyRatchet = selfKeyRatchet;

        if (this.selfKeyRatchet != null) {
            this.reinitSelfEncryptor();
            this.selfEncryptor.setKeyRatchet(selfKeyRatchet);
        }
    }

    private void reinitSelfEncryptor() {
        if (this.selfEncryptor != null) {
            this.selfEncryptor.close();
        }

        this.selfEncryptor = factory.fromEncryptor(factory.createEncryptor());
    }
}
