package moe.kyokobot.koe.dave.mls;

import moe.kyokobot.koe.dave.KeyRatchet;
import moe.kyokobot.koe.mls.HashRatchet;
import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;
import moe.kyokobot.koe.mls.crypto.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MLSKeyRatchet implements KeyRatchet {
    private static final Logger logger = LoggerFactory.getLogger(MLSKeyRatchet.class);

    private final HashRatchet hashRatchet;

    public MLSKeyRatchet(MlsCipherSuite suite, byte[] baseSecret) {
        hashRatchet = new HashRatchet(suite, new Secret(baseSecret));
    }

    @Override
    public byte[] getKey(int keyGeneration) {
        logger.info("Retrieving key for generation {} from HashRatchet", keyGeneration);

        try {
            var keyAndNonce = hashRatchet.get(keyGeneration);
            return keyAndNonce.key;
        } catch (Exception e) {
            logger.error("Failed to retrieve key for generation {}: {}", keyGeneration, e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteKey(int keyGeneration) {
        hashRatchet.erase(keyGeneration);
    }
}
