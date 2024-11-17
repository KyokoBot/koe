package moe.kyokobot.koe.dave.mls;

import moe.kyokobot.koe.dave.DAVEException;
import moe.kyokobot.koe.dave.KeyRatchet;
import moe.kyokobot.koe.dave.PersistentKeyManager;
import moe.kyokobot.koe.dave.util.MLSUtil;
import moe.kyokobot.koe.mls.TreeKEM.LeafNode;
import moe.kyokobot.koe.mls.TreeKEM.LifeTime;
import moe.kyokobot.koe.mls.codec.*;
import moe.kyokobot.koe.mls.crypto.Secret;
import moe.kyokobot.koe.mls.protocol.Group;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;

public class MLSSession {
    private static final Logger logger = LoggerFactory.getLogger(MLSSession.class);

    @Nullable
    private final PersistentKeyManager persistentKeyManager;
    private int protocolVersion;
    private byte[] groupId = new byte[0];
    private final String signingKeyId;
    private String selfUserId = "";
    private LeafNode selfLeafNode;
    private AsymmetricCipherKeyPair selfSigPrivateKey;
    private AsymmetricCipherKeyPair selfHPKEPrivateKey;

    private AsymmetricCipherKeyPair joinInitPrivateKey;
    private KeyPackage joinKeyPackage;

    private ExternalSender externalSender;

    private Group pendingGroupState;
    private MLSMessage pendingGroupCommit;

    private Group outboundCachedGroupState;

    private Group currentState;
    private Map<Long, byte[]> roster;

    private Group stateWithProposals;
    private final Deque<QueuedProposal> proposalQueue = new LinkedList<>();

    /**
     * Creates a new MLS session.
     *
     * @param signingKeyId         ID of a persistent key to use for this session.
     * @param persistentKeyManager Optional instance of {@link PersistentKeyManager} if you want to use persistent keys.
     */
    public MLSSession(@Nullable String signingKeyId, @Nullable PersistentKeyManager persistentKeyManager) {
        this.persistentKeyManager = persistentKeyManager;
        this.signingKeyId = signingKeyId == null ? "" : signingKeyId;
    }

    public void init(int protocolVersion, long groupId, String selfUserId, AsymmetricCipherKeyPair transientKey) throws DAVEException {
        this.selfUserId = selfUserId;
        this.protocolVersion = protocolVersion;
        this.groupId = MLSUtil.bigEndianBytesFrom(groupId);

        logger.info("Initializing MLS session with protocol version {} and group ID {}", protocolVersion, groupId);

        initLeafNode(selfUserId, transientKey);
        createPendingGroup();
    }

    public void reset() {
        logger.info("Resetting MLS session");

        clearPendingState();

        currentState = null;
        outboundCachedGroupState = null;

        protocolVersion = 0;
        groupId = null;
    }

    public void setProtocolVersion(int version) {
        if (protocolVersion != version) {
            // when we need to retain backwards compatibility
            // there may be some changes to the MLS objects required here
            // until then we can just update the stored version
            protocolVersion = version;
        }
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public byte[] getLastEpochAuthenticator() throws DAVEException {
        if (currentState == null) {
            throw new DAVEException("Cannot get epoch authenticator without an established MLS group");
        }

        return currentState.getEpochAuthenticator();
    }

    public void setExternalSender(byte[] marshalledExternalSender) throws DAVEException {
        if (currentState != null) {
            throw new DAVEException("Cannot set external sender after joining/creating an MLS group");
        }

        logger.info("Unmarshalling MLS external sender");
        logger.info("Sender: {}", Hex.encode(marshalledExternalSender));

        try {
            externalSender = new ExternalSender(new MLSInputStream(marshalledExternalSender));

            if (groupId != null && groupId.length > 0) {
                createPendingGroup();
            }
        } catch (Exception e) {
            throw new DAVEException("Failed to unmarshal external sender", e);
        }
    }

    @Nullable
    public byte[] processProposals(byte[] proposals, Set<String> recognizedUserIDs) throws DAVEException {
        try {
            if (pendingGroupState == null && currentState == null) {
                throw new DAVEException("Cannot process proposals without any pending or established MLS group state");
            }

            if (stateWithProposals == null) {
                stateWithProposals = (pendingGroupState != null ? pendingGroupState : currentState);
            }

            logger.info("Processing MLS proposals message of {} bytes", proposals.length);
            logger.info("Proposals: {}", Hex.toHexString(proposals));

            MLSInputStream inStream = new MLSInputStream(proposals);

            boolean isRevoke = (boolean) inStream.read(boolean.class);
            logger.info("Revoking: {}", isRevoke);

            var suite = stateWithProposals.getSuite();

            if (isRevoke) {
                var refs = new ArrayList<byte[]>();
                inStream.readList(refs, byte[].class);

                for (byte[] ref : refs) {
                    boolean found = false;
                    for (var it = proposalQueue.iterator(); it.hasNext(); ) {
                        var prop = it.next();
                        if (Arrays.equals(prop.ref, ref)) {
                            found = true;
                            it.remove();
                            break;
                        }
                    }

                    if (!found) {
                        throw new DAVEException("Cannot revoke unrecognized proposal ref");
                    }
                }

                stateWithProposals = (pendingGroupState != null ? pendingGroupState : currentState);

                for (QueuedProposal prop : proposalQueue) {
                    stateWithProposals.handle(prop.content, null, null);
                }
            } else {
                var messages = new ArrayList<MLSMessage>();
                inStream.readList(messages, MLSMessage.class);

                for (var proposalMessage : messages) {
                    var validatedMessage = stateWithProposals.unwrap(proposalMessage);

                    if (!validateProposalMessage(validatedMessage.getAuthenticatedContent(), stateWithProposals, recognizedUserIDs)) {
                        return null;
                    }

                    stateWithProposals.handle(validatedMessage.getAuthenticatedContent(), null, null);

                    var ref = suite.refHash(MLSOutputStream.encode(validatedMessage.getAuthenticatedContent()), "MLS 1.0 Proposal Reference");

                    proposalQueue.add(new QueuedProposal(validatedMessage.getAuthenticatedContent(), ref));
                }
            }

            // generate a commit
            var commitSecret = new byte[suite.getKDF().getHashLength()];
            SecureRandom random = new SecureRandom();
            random.nextBytes(commitSecret);

            // Commit the Add and PSK proposals
            var commitOpts = new Group.CommitOptions(
                    List.of(), // no extra proposals
                    true, // inline tree in welcome
                    false, // do not force path
                    new Group.LeafNodeOptions()
            );

            var commit = stateWithProposals.commit(new Secret(commitSecret), commitOpts, new Group.MessageOptions(),
                    new Group.CommitParameters(Group.NORMAL_COMMIT_PARAMS));

            logger.info("Prepared commit/welcome/next state for MLS group from received proposals");

            var outStream = new MLSOutputStream();
            outStream.write(commit.message);

            pendingGroupCommit = commit.message;

            if (!commit.message.welcome.getSecrets().isEmpty()) {
                outStream.write(commit.message.welcome);
            }

            outboundCachedGroupState = commit.group;

            logger.info("Output: {}", Hex.toHexString(outStream.toByteArray()));

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new DAVEException("Failed to parse MLS proposals", e);
        }
    }

    private boolean isRecognizedUserID(Credential cred, Set<String> recognizedUserIDs) {
        String uid = UserCredential.userCredentialToString(cred, protocolVersion);
        if (uid.isEmpty()) {
            logger.error("Attempted to verify credential of unexpected type");
            return false;
        }

        if (!recognizedUserIDs.contains(uid)) {
            logger.error("Attempted to verify credential for unrecognized user ID: {}", uid);
            return false;
        }

        return true;
    }

    private boolean validateProposalMessage(AuthenticatedContent message, Group targetGroup, Set<String> recognizedUserIds) {
        if (message.getWireFormat() != WireFormat.mls_private_message) {
            logger.error("MLS proposal message must be PublicMessage");
            return false;
        }

        if (message.getContent().getEpoch() != targetGroup.getEpoch()) {
            logger.error("MLS proposal message must be for current epoch ({}) != {}", message.getContent().getEpoch(), targetGroup.getEpoch());
            return false;
        }

        if (message.getContent().getContentType() != ContentType.PROPOSAL) {
            logger.error("ProcessProposals called with non-proposal message");
            return false;
        }

        if (message.getContent().getSender().getSenderType() != SenderType.EXTERNAL) {
            logger.error("MLS proposal must be from external sender");
            return false;
        }

        var proposal = message.getContent().getProposal();
        switch (proposal.getProposalType()) {
            case ADD:
                var credential = proposal.getAdd().keyPackage.getLeafNode().getCredential();
                if (!isRecognizedUserID(credential, recognizedUserIds)) {
                    logger.error("MLS add proposal must be for recognized user");
                    return false;
                }
                break;
            case REMOVE:
                // Remove proposals are always allowed (mlspp will validate that it's a recognized user)
                break;
            default:
                logger.error("MLS proposal must be add or remove");
                return false;
        }

        return true;
    }

    private boolean canProcessCommit(MLSMessage commit) {
        if (stateWithProposals == null) {
            return false;
        }

        if (!Arrays.equals(commit.getGroupId(), groupId)) {
            logger.error("MLS commit message was for unexpected group");
            return false;
        }

        return true;
    }

    @NotNull
    public RosterVariant processCommit(byte[] commit) {
        try {
            logger.info("Processing commit");
            logger.info("Commit: {}", Hex.toHexString(commit));

            var commitMessage = (MLSMessage) MLSInputStream.decode(commit, MLSMessage.class);

            if (!canProcessCommit(commitMessage)) {
                logger.error("ProcessCommit called with unprocessable MLS commit");
                return new RosterVariant.Ignored();
            }

            Group optionalCachedGroup = null;
            if (outboundCachedGroupState != null) {
                optionalCachedGroup = outboundCachedGroupState;
            }

            var newState = stateWithProposals.handle(stateWithProposals.unwrap(commitMessage).getAuthenticatedContent(),
                    optionalCachedGroup, null);

            if (newState == null) {
                logger.error("MLS commit handling did not produce a new state");
                return new RosterVariant.Failed();
            }

            logger.info("Successfully processed MLS commit, updating state; our leaf index is {}; current epoch is {}",
                    newState.getIndex().value(), newState.getEpoch());

            var ret = replaceState(newState);
            outboundCachedGroupState = null;
            clearPendingState();

            return ret;
        } catch (Exception e) {
            logger.error("Failed to process MLS commit: {}", e.getMessage());
            return new RosterVariant.Failed();
        }
    }

    @Nullable
    public RosterVariant.RosterMap processWelcome(byte[] welcome, Set<String> recognizedUserIDs) {
        try {
            if (!hasCryptographicStateForWelcome()) {
                logger.error("Missing local crypto state necessary to process MLS welcome");
                return null;
            }

            if (externalSender == null) {
                logger.error("Cannot process MLS welcome without an external sender");
                return null;
            }

            if (currentState != null) {
                logger.error("Cannot process MLS welcome after joining/creating an MLS group");
                return null;
            }

            logger.info("Processing welcome: {}", Hex.toHexString(welcome));

            var unmarshalledWelcome = (MLSMessage) MLSInputStream.decode(welcome, MLSMessage.class);
            var suite = unmarshalledWelcome.welcome.getSuite();

            // TODO: BC MLS does redundant serialization ;w;
            var newState = new Group(suite.getHPKE().serializePrivateKey(joinInitPrivateKey.getPrivate()),
                    selfHPKEPrivateKey,
                    suite.getHPKE().serializePrivateKey(selfSigPrivateKey.getPrivate()),
                    joinKeyPackage,
                    unmarshalledWelcome.welcome,
                    null,
                    new HashMap<>(),
                    new HashMap<>());

            if (!verifyWelcomeState(newState, recognizedUserIDs)) {
                logger.error("Group received in MLS welcome is not valid");
                return null;
            }

            logger.info("Successfully welcomed to MLS Group, our leaf index is {}; current epoch is {}",
                    newState.getIndex().value(), newState.getEpoch());

            var ret = replaceState(newState);

            clearPendingState();

            return ret;
        } catch (Exception e) {
            logger.error("Failed to create group state from MLS welcome: {}", e.getMessage());
            return null;
        }
    }

    private RosterVariant.RosterMap replaceState(Group state) {
        var newRoster = new HashMap<Long, byte[]>();
        for (var node : state.roster()) {
            if (node.getCredential().getCredentialType() == CredentialType.basic) {
                newRoster.put(MLSUtil.fromBigEndianBytes(node.getCredential().getIdentity()), node.getSignatureKey());
            }
        }

        var changeMap = new HashMap<Long, byte[]>();

        newRoster.forEach((key, value) -> {
            if (!roster.containsKey(key)) {
                changeMap.put(key, new byte[0]);
            }
        });

        roster.forEach((key, value) -> {
            if (!newRoster.containsKey(key)) {
                changeMap.put(key, new byte[0]);
            }
        });

        roster = newRoster;
        currentState = state;

        return new RosterVariant.RosterMap(changeMap);
    }

    private boolean hasCryptographicStateForWelcome() {
        return joinKeyPackage != null && joinInitPrivateKey != null && selfSigPrivateKey != null && selfHPKEPrivateKey != null;
    }

    private boolean verifyWelcomeState(Group state, Set<String> recognizedUserIDs) {
        if (externalSender == null) {
            logger.error("Cannot verify MLS welcome without an external sender");
            return false;
        }

        var ext = state.getExtensions().stream().filter(extension -> extension.extensionType == ExtensionType.EXTERNAL_SENDERS).findFirst();
        if (ext.isEmpty()) {
            logger.error("MLS welcome missing external senders extension");
            return false;
        }

        List<ExternalSender> senders;
        try {
            senders = ext.get().getSenders();
        } catch (IOException e) {
            logger.error("Failed to read external senders", e);
            return false;
        }

        if (senders.size() != 1) {
            logger.error("MLS welcome lists unexpected number of external senders: {}", senders.size());
            return false;
        }

        if (!senders.get(0).equals(externalSender)) {
            logger.error("MLS welcome lists unexpected external sender");
            return false;
        }

        for (var leaf : state.roster()) {
            if (!isRecognizedUserID(leaf.getCredential(), recognizedUserIDs)) {
                logger.error("MLS welcome lists unrecognized user ID");
                // TRACK_MLS_ERROR("Welcome message lists unrecognized user ID");
                // return false;
            }
        }

        return true;
    }

    private void initLeafNode(String selfUserId, @Nullable AsymmetricCipherKeyPair transientKey) throws DAVEException {
        var cipherSuite = Parameters.ciphersuiteForProtocolVersion(protocolVersion);

        if (transientKey == null) {
            if (signingKeyId.isEmpty()) {
                //Generate a new key pair
                transientKey = cipherSuite.generateSignatureKeyPair();
            } else if (persistentKeyManager != null) {
                transientKey = persistentKeyManager.getKeyPair(signingKeyId, protocolVersion);
            } else {
                throw new DAVEException("Did not receive MLS signature private key!");
            }
        }

        selfSigPrivateKey = transientKey;

        var selfCredential = UserCredential.createUserCredential(selfUserId, protocolVersion);

        selfHPKEPrivateKey = cipherSuite.getHPKE().generatePrivateKey();

        try {
            Objects.requireNonNull(selfSigPrivateKey);
            Objects.requireNonNull(selfHPKEPrivateKey);

            selfLeafNode = new LeafNode(cipherSuite,
                    cipherSuite.getHPKE().serializePublicKey(selfHPKEPrivateKey.getPublic()),
                    cipherSuite.serializeSignaturePublicKey(selfSigPrivateKey.getPublic()),
                    selfCredential,
                    Parameters.leafNodeCapabilitiesForProtocolVersion(protocolVersion),
                    new LifeTime(),
                    Parameters.leafNodeExtensionsForProtocolVersion(protocolVersion),
                    cipherSuite.serializeSignaturePrivateKey(selfSigPrivateKey.getPrivate()));

            logger.info("Created MLS leaf node");
        } catch (Exception e) {
            throw new DAVEException("Failed to initialize MLS leaf node", e);
        }
    }

    private void resetJoinKeyPackage() throws DAVEException {
        try {
            if (selfLeafNode == null) {
                throw new DAVEException("Cannot initialize join key package without a leaf node");
            }

            var cipherSuite = Parameters.ciphersuiteForProtocolVersion(protocolVersion);

            joinInitPrivateKey = cipherSuite.getHPKE().generatePrivateKey();

            joinKeyPackage = new KeyPackage(cipherSuite,
                    cipherSuite.getHPKE().serializePublicKey(joinInitPrivateKey.getPublic()),
                    selfLeafNode,
                    Parameters.leafNodeExtensionsForProtocolVersion(protocolVersion),
                    cipherSuite.serializeSignaturePrivateKey(selfSigPrivateKey.getPrivate()));

            logger.info("Generated key package: {}", Hex.toHexString(MLSOutputStream.encode(joinKeyPackage)));
        } catch (Exception e) {
            throw new DAVEException("Failed to initialize join key package", e);
        }
    }

    private void createPendingGroup() throws DAVEException {
        try {
            if (groupId == null || groupId.length == 0) {
                throw new DAVEException("Cannot create MLS group without a group ID");
            }

            if (externalSender == null) {
                throw new DAVEException("Cannot create MLS group without ExternalSender");
            }

            if (selfLeafNode == null) {
                throw new DAVEException("Cannot create MLS group without self leaf node");
            }

            logger.info("Creating a pending MLS group");

            var cipherSuite = Parameters.ciphersuiteForProtocolVersion(protocolVersion);

            pendingGroupState = new Group(groupId, cipherSuite, selfHPKEPrivateKey,
                    cipherSuite.serializeSignaturePrivateKey(selfSigPrivateKey.getPrivate()),
                    selfLeafNode,
                    Parameters.groupExtensionsForProtocolVersion(protocolVersion, externalSender));

            logger.info("Created a pending MLS group");
        } catch (Exception e) {
            throw new DAVEException("Failed to create MLS group", e);
        }
    }

    public byte[] getMarshalledKeyPackage() throws DAVEException {
        try {
            // key packages are not meant to be re-used
            // so every time the client asks for a key package we create a new one
            resetJoinKeyPackage();

            if (joinKeyPackage == null) {
                throw new DAVEException("Cannot marshal an uninitialized key package");
            }

            return MLSOutputStream.encode(joinKeyPackage);
        } catch (Exception e) {
            throw new DAVEException("Failed to marshal join key package", e);
        }
    }

    public KeyRatchet getKeyRatchet(String userId) throws DAVEException {
        try {
            if (currentState == null) {
                throw new DAVEException("Cannot get key ratchet without an established MLS group");
            }

            // change the string user ID to a little endian 64 bit user ID
            byte[] userIdBytes = MLSUtil.littleEndianBytesFromString(userId);

            // generate the base secret for the hash ratchet
            byte[] baseSecret = currentState.getKeySchedule().MLSExporter("User Media Key Base Label", userIdBytes, 16);

            // this assumes the MLS ciphersuite produces a 16 byte key
            // would need to be updated to a different ciphersuite if there's a future mismatch
            return new MLSKeyRatchet(currentState.getSuite(), baseSecret);
        } catch (Exception e) {
            throw new DAVEException("Failed to get key ratchet", e);
        }
    }

    private static final byte[] SALT = Hex.decode("24cab17a7af8ec2b82b412b92dab192e");

    public void getPairwiseFingerprint(int version, String userId, Consumer<byte[]> callback) throws DAVEException {
        try {
            if (currentState == null || selfSigPrivateKey == null) {
                throw new DAVEException("Cannot get pairwise fingerprint without an established MLS group");
            }

            long u64RemoteUserId = Long.parseLong(userId);
            long u64SelfUserId = Long.parseLong(selfUserId);

            byte[] remoteUserIdBytes = MLSUtil.bigEndianBytesFrom(u64RemoteUserId);
            byte[] selfUserIdBytes = MLSUtil.bigEndianBytesFrom(u64SelfUserId);

            MLSOutputStream toHash1 = new MLSOutputStream();
            MLSOutputStream toHash2 = new MLSOutputStream();

            toHash1.write(version);
            toHash1.write(remoteUserIdBytes);

            toHash2.write(version);
            toHash2.write(currentState.getSuite().serializeSignaturePublicKey(selfSigPrivateKey.getPublic()));
            toHash2.write(selfUserIdBytes);

            var keyData = new ArrayList<byte[]>();
            keyData.add(toHash1.toByteArray());
            keyData.add(toHash2.toByteArray());
            keyData.sort(Arrays::compare);

            var data = new byte[keyData.get(0).length + keyData.get(1).length];
            System.arraycopy(keyData.get(0), 0, data, 0, keyData.get(0).length);
            System.arraycopy(keyData.get(1), 0, data, keyData.get(0).length, keyData.get(1).length);

            int N = 16384, r = 8, p = 2, maxMem = 32 * 1024 * 1024;
            int hashLen = 64;

            var out = SCrypt.generate(data, SALT, N, r, p, hashLen);
            callback.accept(out);
        } catch (Exception e) {
            throw new DAVEException("Failed to generate pairwise fingerprint", e);
        }
    }

    private void clearPendingState() {
        pendingGroupState = null;
        pendingGroupCommit = null;

        joinInitPrivateKey = null;
        joinKeyPackage = null;

        selfHPKEPrivateKey = null;

        selfLeafNode = null;

        stateWithProposals = null;
        proposalQueue.clear();
    }

    static class QueuedProposal {
        private final AuthenticatedContent content;
        private final byte[] ref;

        public QueuedProposal(AuthenticatedContent content, byte[] ref) {
            this.content = content;
            this.ref = ref;
        }

        public AuthenticatedContent getContent() {
            return content;
        }

        public byte[] getRef() {
            return ref;
        }
    }
}
