package moe.kyokobot.koe.crypto;

import java.util.Map;
import java.util.function.Supplier;

class DefaultEncryptionModes {
    private DefaultEncryptionModes() {
        //
    }

    static final Map<String, Supplier<EncryptionMode>> encryptionModes;

    static {
        encryptionModes = Map.of( // sorted by priority
                "xsalsa20_poly1305_lite", XSalsa20Poly1305LiteEncryptionMode::new,
                "xsalsa20_poly1305_suffix", XSalsa20Poly1305SuffixEncryptionMode::new,
                "xsalsa20_poly1305", XSalsa20Poly1305EncryptionMode::new,
                "plain", PlainEncryptionMode::new // not supported by Discord anymore, implemented for testing.
        );
    }
}
