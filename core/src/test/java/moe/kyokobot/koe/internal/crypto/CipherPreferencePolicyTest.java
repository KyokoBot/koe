package moe.kyokobot.koe.internal.crypto;

import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.experimental.KoeOptionsExperimental;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;
import moe.kyokobot.koe.internal.util.AesAcceleration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static moe.kyokobot.koe.internal.crypto.CipherPolicies.AES_GCM;
import static moe.kyokobot.koe.internal.crypto.CipherPolicies.XCHACHA20_POLY1305;
import static org.junit.jupiter.api.Assertions.*;

class CipherPreferencePolicyTest {
    private static final String XSALSA20_LITE = "xsalsa20_poly1305_lite";

    @Test
    void serverOrderTakesTheFirstSupportedMode() {
        assertEquals(XCHACHA20_POLY1305, select(CipherPreferencePolicy.serverOrder(), "made_up_mode", XCHACHA20_POLY1305, AES_GCM));
    }

    @Test
    void preferringTakesTheFirstOfferedPreference() {
        var policy = CipherPreferencePolicy.preferring(List.of(AES_GCM, XCHACHA20_POLY1305));

        assertEquals(AES_GCM, select(policy, XCHACHA20_POLY1305, AES_GCM));
        assertEquals(XCHACHA20_POLY1305, select(policy, XSALSA20_LITE, XCHACHA20_POLY1305));
    }

    @Test
    void preferringFallsBackToServerOrder() {
        var policy = CipherPreferencePolicy.preferring(List.of("made_up_mode"));

        assertEquals(XSALSA20_LITE, select(policy, XSALSA20_LITE, XCHACHA20_POLY1305));
    }

    @Test
    void heuristicPrefersAesOnlyWhenAccelerated() {
        assertEquals(AES_GCM, select(CipherPolicies.heuristic(true), XCHACHA20_POLY1305, AES_GCM));
        assertEquals(XCHACHA20_POLY1305, select(CipherPolicies.heuristic(false), AES_GCM, XCHACHA20_POLY1305));
        assertEquals(XSALSA20_LITE, select(CipherPolicies.heuristic(false), "made_up_mode", XSALSA20_LITE));
    }

    @Test
    void selectRejectsWhatThePolicyCantUse() {
        var offered = List.of(AES_GCM, XCHACHA20_POLY1305);

        assertThrows(UnsupportedEncryptionModeException.class,
                () -> EncryptionMode.select(List.of("made_up_mode"), CipherPreferencePolicy.serverOrder()));
        assertThrows(UnsupportedEncryptionModeException.class,
                () -> EncryptionMode.select(offered, (o, s) -> "made_up_mode"));
        assertThrows(UnsupportedEncryptionModeException.class,
                () -> EncryptionMode.select(offered, (o, s) -> "plain"), "supported, but not offered");
        assertThrows(UnsupportedEncryptionModeException.class,
                () -> EncryptionMode.select(offered, (o, s) -> null));
        assertNull(EncryptionMode.get("made_up_mode"));
    }

    @Test
    void policyGetsTheServerOrderAndCantModifyIt() {
        var offered = List.of(XSALSA20_LITE, AES_GCM);

        assertEquals(AES_GCM, EncryptionMode.select(offered, (o, supported) -> {
            assertEquals(offered, o);
            assertThrows(UnsupportedOperationException.class, o::clear);
            assertTrue(supported.containsAll(offered));
            return o.get(1);
        }));
    }

    @Test
    void optionsDefaultToTheHeuristicAndCanBeOverridden() {
        var defaults = (KoeOptionsExperimental) KoeOptions.builder().create();
        var policy = CipherPreferencePolicy.serverOrder();
        var custom = KoeOptionsExperimental.builder().setCipherPreferencePolicy(policy).create();
        try {
            var expected = AesAcceleration.isLikelyAccelerated() ? AES_GCM : XCHACHA20_POLY1305;
            assertEquals(expected, select(defaults.getCipherPreferencePolicy(), XCHACHA20_POLY1305, AES_GCM));
            assertSame(policy, custom.getCipherPreferencePolicy());
        } finally {
            defaults.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS);
            custom.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS);
        }
    }

    private static String select(CipherPreferencePolicy policy, String... offered) {
        return EncryptionMode.select(List.of(offered), policy);
    }
}
