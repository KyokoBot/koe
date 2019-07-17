package moe.kyokobot.koe.crypto;

import moe.kyokobot.koe.internal.crypto.XS20P1305EncryptionMode;
import moe.kyokobot.koe.internal.crypto.XS20P1305SuffixEncryptionMode;

import java.util.Map;

class DefaultEncryptionModes {
    private DefaultEncryptionModes() {
        //
    }

    static final Map<String, EncryptionMode> encryptionModes;

    static {
        encryptionModes = Map.of( // sorted by priority
                "xsalsa20_poly1305_suffix", new XS20P1305SuffixEncryptionMode(),
                "xsalsa20_poly1305", new XS20P1305EncryptionMode()
        );
    }
}
