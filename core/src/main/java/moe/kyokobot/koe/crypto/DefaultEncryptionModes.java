package moe.kyokobot.koe.crypto;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class DefaultEncryptionModes {
    private DefaultEncryptionModes() {
        //
    }

    static final Map<String, Supplier<EncryptionMode>> encryptionModes;

    static {
        encryptionModes = new HashMap<>();
        encryptionModes.put("xsalsa20_poly1305_lite", XSalsa20Poly1305LiteEncryptionMode::new);
        encryptionModes.put("xsalsa20_poly1305_suffix", XSalsa20Poly1305SuffixEncryptionMode::new);
        encryptionModes.put("xsalsa20_poly1305", XSalsa20Poly1305EncryptionMode::new);
        encryptionModes.put("plain", PlainEncryptionMode::new);
    }
}
