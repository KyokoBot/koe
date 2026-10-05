package moe.kyokobot.koe.internal.crypto;

import io.netty.buffer.ByteBuf;

import java.security.SecureRandom;
import java.util.List;

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

    static String select(List<String> modes) throws UnsupportedEncryptionModeException {
        for (String mode : modes) {
            var impl = DefaultEncryptionModes.encryptionModes.get(mode);

            if (impl != null) {
                return mode;
            }
        }

        throw new UnsupportedEncryptionModeException("Cannot find a suitable encryption mode for this connection!");
    }

    static EncryptionMode get(String mode) {
        var factory = DefaultEncryptionModes.encryptionModes.get(mode);
        return factory != null ? factory.get() : null;
    }
}
