package moe.kyokobot.koe.internal.crypto;

import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.experimental.KoeOptionsExperimental;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;

import java.util.List;
import java.util.Set;

public final class CipherPolicies {
    static final String AES_GCM = "aead_aes256_gcm_rtpsize";
    static final String XCHACHA20_POLY1305 = "aead_xchacha20_poly1305_rtpsize";

    private CipherPolicies() {
        //
    }

    public static CipherPreferencePolicy forOptions(KoeOptions options) {
        return options instanceof KoeOptionsExperimental
                ? ((KoeOptionsExperimental) options).getCipherPreferencePolicy()
                : CipherPreferencePolicy.heuristic();
    }

    public static String firstSupported(List<String> offered, Set<String> supported) {
        for (var mode : offered) {
            if (supported.contains(mode)) {
                return mode;
            }
        }
        return null;
    }

    public static CipherPreferencePolicy preferring(List<String> preference) {
        var order = List.copyOf(preference);
        return (offered, supported) -> {
            for (var mode : order) {
                if (offered.contains(mode) && supported.contains(mode)) {
                    return mode;
                }
            }
            return firstSupported(offered, supported);
        };
    }

    public static CipherPreferencePolicy heuristic(boolean aesAccelerated) {
        return aesAccelerated
                ? preferring(List.of(AES_GCM, XCHACHA20_POLY1305))
                : preferring(List.of(XCHACHA20_POLY1305, AES_GCM));
    }
}
