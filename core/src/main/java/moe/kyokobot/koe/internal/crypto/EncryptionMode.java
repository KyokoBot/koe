package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

public interface EncryptionMode {
    SecureRandom SECURE_RANDOM = new SecureRandom();

    int TAG_BYTES_LENGTH = 16;
    int SECRET_KEY_LENGTH = 32;

    /**
     * Checks the key once per session, {@link #box} assumes it was validated.
     *
     * @throws IllegalArgumentException if the key can't be used with this mode
     */
    default void validateKey(byte[] secretKey) {
        if (secretKey == null || secretKey.length != SECRET_KEY_LENGTH) {
            throw new IllegalArgumentException(getName() + " requires a " + SECRET_KEY_LENGTH + " byte key, got "
                    + (secretKey == null ? "none" : secretKey.length + " bytes"));
        }
    }

    boolean box(ByteBuf plain, int start, ByteBuf output, byte[] secretKey);

    String getName();

    /**
     * @param offered modes offered by the voice server
     * @return the mode picked by the policy
     * @throws UnsupportedEncryptionModeException if the policy picked nothing, or a mode that isn't both offered and
     *                                            supported
     */
    static String select(List<String> offered, CipherPreferencePolicy policy) throws UnsupportedEncryptionModeException {
        var supported = supportedModes();
        var mode = policy.select(List.copyOf(offered), supported);

        if (mode == null) {
            throw new UnsupportedEncryptionModeException("Cannot find a suitable encryption mode for this connection! "
                    + "Offered by the server: " + offered);
        }
        if (!offered.contains(mode) || !supported.contains(mode)) {
            throw new UnsupportedEncryptionModeException("Encryption mode " + mode + " picked by " + policy
                    + " is not both offered by the server " + offered + " and supported " + supported);
        }
        return mode;
    }

    static Set<String> supportedModes() {
        return DefaultEncryptionModes.encryptionModes.keySet();
    }

    static EncryptionMode get(String mode) {
        var factory = DefaultEncryptionModes.encryptionModes.get(mode);
        return factory != null ? factory.get() : null;
    }
}
