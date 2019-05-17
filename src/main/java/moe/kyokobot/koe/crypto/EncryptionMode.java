package moe.kyokobot.koe.crypto;

import io.netty.buffer.ByteBuf;

public interface EncryptionMode {
    void box(ByteBuf opus, ByteBuf output);

    static EncryptionMode getByName(String name) {
        var mode = DefaultEncryptionModes.encryptionModes.get(name);
        if (mode == null) {
            throw new UnsupportedEncryptionModeException(name);
        } else {
            return mode;
        }
    }
}
