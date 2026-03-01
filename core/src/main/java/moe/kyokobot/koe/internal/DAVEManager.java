package moe.kyokobot.koe.internal;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.libdave.*;
import moe.kyokobot.libdave.netty.NettyDaveFactory;
import moe.kyokobot.libdave.netty.NettyEncryptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

public class DAVEManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DAVEManager.class);

    private static final String MLS_NEW_GROUP_EPOCH = "1";
    private static final int INIT_TRANSITION_ID = 0;

    private final MediaConnectionImpl connection;
    private final NettyDaveFactory factory;
    private final Session daveSession;
    private final Set<String> recognizedUserIds = ConcurrentHashMap.newKeySet();
    private final Map<String, byte[]> activeE2EEUsers = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> pendingTransitions = new ConcurrentHashMap<>();
    private final int maxProtocolVersion;

    private final StampedLock sessionLock = new StampedLock();
    private volatile boolean closed = false;

    private final NettyEncryptor selfEncryptor;
    private @Nullable KeyRatchet selfKeyRatchet = null;
    private final String selfUserIdString;
    private long mlsGroupId = 0;

    private int currentProtocolVersion = 0;

    DAVEManager(@NotNull MediaConnectionImpl connection, @NotNull NettyDaveFactory factory) {
        this.connection = connection;
        this.factory = factory;
        this.daveSession = factory.createSession("", "", this::mlsFailureCallback);
        this.selfEncryptor = factory.fromEncryptor(factory.createEncryptor());
        this.selfEncryptor.setPassthroughMode(true);
        this.maxProtocolVersion = factory.maxSupportedProtocolVersion();
        this.selfUserIdString = String.valueOf(connection.getClient().getClientId());
    }

    public int getMaxDAVEProtocolVersion() {
        return maxProtocolVersion;
    }

    public int getCurrentProtocolVersion() {
        return currentProtocolVersion;
    }

    public int getMaxCiphertextByteSize(MediaType mediaType, int frameSize) {
        long stamp = sessionLock.readLock();
        try {
            return selfEncryptor.getMaxCiphertextByteSize(mediaType, frameSize);
        } finally {
            sessionLock.unlockRead(stamp);
        }
    }

    public void addUsers(Iterable<String> userIds) {
        if (closed) return;
        long stamp = sessionLock.writeLock();
        try {
            for (var uid : userIds) {
                addUserLocked(uid);
            }
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void addUser(String userId) {
        if (closed) return;
        long stamp = sessionLock.writeLock();
        try {
            addUserLocked(userId);
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    private void addUserLocked(String userId) {
        recognizedUserIds.add(userId);
        // TODO: Setup decryption
    }

    public void removeUser(String userId) {
        if (closed) return;
        long stamp = sessionLock.writeLock();
        try {
            removeUserLocked(userId);
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    private void removeUserLocked(String userId) {
        recognizedUserIds.remove(userId);
        activeE2EEUsers.remove(userId);
        // TODO: Cleanup decryption
    }

    public int encrypt(MediaType mediaType, int ssrc, ByteBuf output, ByteBuf input, int size) {
        if (closed) {
            return -EncryptorResultCode.ENCRYPTION_FAILURE.getValue();
        }

        if (mediaType == MediaType.AUDIO && size == 3) {
            input.markReaderIndex();
            var b1 = input.readByte() == OpusCodecInfo.SILENCE_FRAME[0];
            var b2 = input.readByte() == OpusCodecInfo.SILENCE_FRAME[1];
            var b3 = input.readByte() == OpusCodecInfo.SILENCE_FRAME[2];
            var isSilence = b1 && b2 && b3;
            input.resetReaderIndex();

            if (isSilence) {
                output.writeBytes(OpusCodecInfo.SILENCE_FRAME);
                return EncryptorResultCode.SUCCESS.getValue();
            }
        }

        long stamp = sessionLock.readLock();
        try {
            output.ensureWritable(this.selfEncryptor.getMaxCiphertextByteSize(mediaType, size));
            return this.selfEncryptor.encrypt(mediaType, ssrc, input, output);
        } finally {
            sessionLock.unlockRead(stamp);
        }
    }

    public void handleSessionDescription(@NotNull JsonObject session, long mlsGroupId) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            int protocolVersion = session.getInt("dave_protocol_version", 0);
            this.mlsGroupId = mlsGroupId;
            daveProtocolInit(protocolVersion);
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleSecureFramesPrepareProtocolTransition(int transitionId, int newProtocolVersion) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            prepareRatchets(transitionId, newProtocolVersion);
            if (transitionId != 0) {
                sendSecureFramesReadyForTransition(transitionId);
            }
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleSecureFramesExecuteTransition(int transitionId) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            executeTransition(transitionId);
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleSecureFramesPrepareEpoch(String epoch, int protocolVersion) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            prepareEpoch(epoch, protocolVersion);

            if (MLS_NEW_GROUP_EPOCH.equals(epoch)) {
                sendMLSKeyPackage();
            }
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleMLSExternalSender(byte[] payload) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            daveSession.setExternalSender(payload);
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleMLSProposals(byte[] payload) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            var userIds = recognizedUserIdArray();

            var commitWelcome = daveSession.processProposals(payload, userIds);
            if (commitWelcome != null) {
                sendMLSCommitWelcome(commitWelcome);
            }
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleMLSPrepareCommitTransition(int transitionId, byte[] commit) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            var result = daveSession.processCommit(commit);
            if (result.isIgnored()) return;

            if (result.isFailed()) {
                sendMLSInvalidCommitWelcome(transitionId);
                daveProtocolInit(daveSession.getProtocolVersion());
                return;
            }

            updateActiveUsers(result.getRosterMap());

            prepareRatchets(transitionId, daveSession.getProtocolVersion());
            if (transitionId != 0) {
                sendSecureFramesReadyForTransition(transitionId);
            }
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    public void handleMLSWelcome(int transitionId, byte[] welcome) {
        if (closed) return;

        long stamp = sessionLock.writeLock();
        try {
            var roster = daveSession.processWelcome(welcome, recognizedUserIdArray());

            if (roster == null) {
                sendMLSInvalidCommitWelcome(transitionId);
                sendMLSKeyPackage();
                return;
            }

            updateActiveUsers(roster);

            prepareRatchets(transitionId, daveSession.getProtocolVersion());
            if (transitionId != 0) {
                sendSecureFramesReadyForTransition(transitionId);
            }
        } finally {
            sessionLock.unlockWrite(stamp);
        }
    }

    private void daveProtocolInit(int protocolVersion) {
        logger.debug("DAVE Init - Protocol version={}, MLS Group ID={}", protocolVersion, mlsGroupId);
        if (protocolVersion > 0) {
            prepareEpoch(MLS_NEW_GROUP_EPOCH, protocolVersion);
            sendMLSKeyPackage();
        } else {
            activeE2EEUsers.clear();
            prepareRatchets(INIT_TRANSITION_ID, protocolVersion);
            executeTransition(INIT_TRANSITION_ID);
        }
    }

    private void updateActiveUsers(RosterMap roster) {
        for (var entry : roster.entrySet()) {
            String userId = String.valueOf(entry.getKey());
            byte[] key = entry.getValue();
            if (key == null || key.length == 0) {
                activeE2EEUsers.remove(userId);
            } else {
                activeE2EEUsers.put(userId, key);
            }
        }
    }

    private void prepareEpoch(String epoch, int protocolVersion) {
        if (closed) return;
        if (MLS_NEW_GROUP_EPOCH.equals(epoch)) {
            daveSession.init(protocolVersion, mlsGroupId, selfUserIdString);
        }
    }

    private void setupKeyRatchetForUser(String uid, int protocolVersion) {
        var keyRatchet = makeKeyRatchetForUser(uid, protocolVersion);
        if (selfUserIdString.equals(uid)) {
            setSelfKeyRatchet(keyRatchet);
        } else if (keyRatchet != null) {
            keyRatchet.close();
        }
    }

    @Nullable
    private KeyRatchet makeKeyRatchetForUser(String uid, int protocolVersion) {
        if (protocolVersion == 0) {
            return null;
        }

        return daveSession.getKeyRatchet(uid);
    }

    private void prepareRatchets(int transitionId, int protocolVersion) {
        if (protocolVersion == 0) {
            for (var uid : recognizedUserIds) {
                setupKeyRatchetForUser(uid, 0);
            }
        } else {
            for (var uid : activeE2EEUsers.keySet()) {
                if (selfUserIdString.equals(uid)) {
                    continue;
                }

                setupKeyRatchetForUser(uid, protocolVersion);
            }
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
            activeE2EEUsers.clear();
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
        closed = true;
        long stamp = sessionLock.writeLock();
        try {
            daveSession.close();
            if (selfKeyRatchet != null) {
                selfKeyRatchet.close();
                selfKeyRatchet = null;
            }
            selfEncryptor.close();
        } finally {
            sessionLock.unlockWrite(stamp);
        }
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

    private void setSelfKeyRatchet(KeyRatchet selfKeyRatchet) {
        if (this.selfKeyRatchet != null) {
            this.selfKeyRatchet.close();
        }

        this.selfKeyRatchet = selfKeyRatchet;

        if (this.selfKeyRatchet == null) {
            this.selfEncryptor.setPassthroughMode(true);
        } else {
            this.selfEncryptor.setKeyRatchet(selfKeyRatchet);
            this.selfEncryptor.setPassthroughMode(false);
        }
    }
}
