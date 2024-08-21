package moe.kyokobot.koe.crypto;

import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class DefaultEncryptionModes {

    private static final String AES_GCM_NO_PADDING = "AES_256/GCM/NOPADDING";

    private DefaultEncryptionModes() {
        //
    }

    static final Map<String, Supplier<EncryptionMode>> encryptionModes;

    static {
        // sorted by priority
        var modes = new HashMap<String, Supplier<EncryptionMode>>();

        // the jvm may not support this algorithm, so we need to check first if it is available
        if (Security.getAlgorithms("Cipher").contains(AES_GCM_NO_PADDING)) {
            modes.put("aead_aes256_gcm_rtpsize", AEADAES256GCMRTPSizeEncryptionMode::new); // recommended by Discord when available)
        }

        modes.put("aead_xchacha20_poly1305_rtpsize", AEADXChaCha20Poly1305RTPSizeEncryptionMode::new); // required by Discord
        modes.put("xsalsa20_poly1305_lite", XSalsa20Poly1305LiteEncryptionMode::new); // deprecated and discontinued by Discord as of 18th of November 2024
        modes.put("xsalsa20_poly1305_suffix", XSalsa20Poly1305SuffixEncryptionMode::new); // deprecated and discontinued by Discord as of 18th of November 2024
        modes.put("xsalsa20_poly1305", XSalsa20Poly1305EncryptionMode::new); // deprecated and discontinued by Discord as of 18th of November 2024
        modes.put("plain", PlainEncryptionMode::new); // not supported by Discord anymore, implemented for testing.

        encryptionModes = Collections.unmodifiableMap(modes);
    }
}
