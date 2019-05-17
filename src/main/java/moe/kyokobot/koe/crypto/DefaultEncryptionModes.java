package moe.kyokobot.koe.crypto;

import java.util.Map;

class DefaultEncryptionModes {
    private DefaultEncryptionModes() {
        //
    }

    static final Map<String, EncryptionMode> encryptionModes;

    static {
        encryptionModes = Map.of(
                "xsalsa20_poly1305", new XS20P1305EncryptionMode(),
                "xsalsa20_poly1305_suffix", new XS20P1305SuffixEncryptionMode()
        );
    }
}
