package moe.kyokobot.koe.dave.mls;

import moe.kyokobot.koe.dave.util.MLSUtil;
import moe.kyokobot.koe.mls.codec.Credential;
import moe.kyokobot.koe.mls.codec.CredentialType;

public class UserCredential {
    public static Credential createUserCredential(String userId, @SuppressWarnings("unused") int protocolVersion) {
        // convert the string user ID to a big endian uint64_t
        var userID = Long.parseUnsignedLong(userId);
        var credentialBytes = MLSUtil.bigEndianBytesFrom(userID);

        return Credential.forBasic(credentialBytes);
    }

    public static String userCredentialToString(Credential cred, @SuppressWarnings("unused") int protocolVersion) {
        if (cred.getCredentialType() != CredentialType.basic) {
            return "";
        }

        var uidVal = MLSUtil.fromBigEndianBytes(cred.getIdentity());

        return Long.toUnsignedString(uidVal);
    }
}
