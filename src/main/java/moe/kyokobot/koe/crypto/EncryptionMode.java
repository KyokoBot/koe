package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;

import java.util.List;

public interface EncryptionMode {
    void box(ByteBuf opus, ByteBuf output);

    static EncryptionMode select(List<String> modes) {
        for (String mode : modes) {
            var impl = DefaultEncryptionModes.encryptionModes.get(mode);

            if (impl != null) {
                return impl;
            }
        }

        throw new UnsupportedEncryptionModeException("Cannot find a suitable encryption mode for this connection!");
    }
}
