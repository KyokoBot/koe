package moe.kyokobot.koe.mls;

import moe.kyokobot.koe.mls.crypto.MlsCipherSuite;
import moe.kyokobot.koe.mls.crypto.Secret;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class HashRatchet {
    final MlsCipherSuite suite;
    final int keySize;
    final int nonceSize;
    final int secretSize;
    Secret nextSecret;
    int nextGeneration;
    Map<Integer, KeyGeneration> cache;

    public HashRatchet(MlsCipherSuite suite, Secret baseSecret) {
        this.suite = suite;
        keySize = suite.getAEAD().getKeySize();
        nonceSize = suite.getAEAD().getNonceSize();
        secretSize = suite.getKDF().getHashLength();
        nextGeneration = 0;
        nextSecret = baseSecret;
        cache = new HashMap<>();
    }

    public KeyGeneration next() throws IOException {
        Secret key = nextSecret.deriveTreeSecret(suite, "key", nextGeneration, keySize);
        Secret nonce = nextSecret.deriveTreeSecret(suite, "nonce", nextGeneration, nonceSize);
        Secret secret = nextSecret.deriveTreeSecret(suite, "secret", nextGeneration, secretSize);

        KeyGeneration generation = new KeyGeneration(nextGeneration, key, nonce);

        nextGeneration += 1;
        nextSecret.consume();
        nextSecret = secret;

        cache.put(generation.generation, generation);
        return generation;
    }

    public KeyGeneration get(int generation)
            throws IOException, IllegalAccessException {
        if (cache.containsKey(generation)) {
            return cache.get(generation);
        }

        if (nextGeneration > generation) {
            throw new InvalidParameterException("Request for expired key");
        }

        while (nextGeneration < generation) {
            next();
        }

        return next();
    }

    public void erase(int generation) {
        if (cache.containsKey(generation)) {
            cache.get(generation).consume();
            cache.remove(generation);
        }
    }
}
