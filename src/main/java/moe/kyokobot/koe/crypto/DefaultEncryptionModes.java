package moe.kyokobot.koe.crypto;

import moe.kyokobot.koe.internal.crypto.XSalsa20Poly1305EncryptionMode;
import moe.kyokobot.koe.internal.crypto.XSalsa20Poly1305SuffixEncryptionMode;

import java.util.Map;

class DefaultEncryptionModes {
    private DefaultEncryptionModes() {
        //
    }

    static final Map<String, EncryptionMode> encryptionModes;

    static {
        encryptionModes = Map.of( // sorted by priority
                "xsalsa20_poly1305_suffix", new XSalsa20Poly1305SuffixEncryptionMode(),
                "xsalsa20_poly1305", new XSalsa20Poly1305EncryptionMode()
        );
    }
}
