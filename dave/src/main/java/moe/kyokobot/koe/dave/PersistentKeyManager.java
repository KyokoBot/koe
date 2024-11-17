package moe.kyokobot.koe.dave;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

public interface PersistentKeyManager {
    AsymmetricCipherKeyPair getKeyPair(String signingKeyId, int protocolVersion);
}
