package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;

import java.util.List;

public interface EncryptionMode {
    int ZERO_BYTES_LENGTH = 32; // For XSalsa20Poly1305

    boolean box(ByteBuf opus, int start, ByteBuf output, byte[] secretKey);

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
