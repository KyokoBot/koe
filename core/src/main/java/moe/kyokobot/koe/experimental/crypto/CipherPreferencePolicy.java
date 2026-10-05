package moe.kyokobot.koe.experimental.crypto;

import moe.kyokobot.koe.internal.crypto.CipherPolicies;
import moe.kyokobot.koe.internal.util.AesAcceleration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Picks the transport encryption mode of a voice session out of the ones offered by the voice server.
 *
 * @see moe.kyokobot.koe.experimental.KoeOptionsBuilderExperimental#setCipherPreferencePolicy(CipherPreferencePolicy)
 */
@FunctionalInterface
public interface CipherPreferencePolicy {
    /**
     * @param offered   encryption modes offered by the voice server, in the order it sent them
     * @param supported encryption modes implemented by Koe
     * @return a mode that is both offered and supported, or null if none of them is acceptable, which fails the
     * connection
     */
    @Nullable
    String select(@NotNull List<String> offered, @NotNull Set<String> supported);

    /**
     * @return a policy which takes the first supported mode in the order the voice server sent them
     */
    @NotNull
    static CipherPreferencePolicy serverOrder() {
        return CipherPolicies::firstSupported;
    }

    /**
     * @param preference encryption mode names, most preferred first
     * @return a policy which takes the first mode from {@code preference} that the voice server offers, and falls
     * back to {@link #serverOrder()} if it offers none of them
     */
    @NotNull
    static CipherPreferencePolicy preferring(@NotNull List<String> preference) {
        return CipherPolicies.preferring(preference);
    }

    /**
     * The default policy. Prefers AES-GCM if the JVM reports using hardware AES instructions (HotSpot), and
     * XChaCha20-Poly1305 otherwise, which is several times faster than AES-GCM implemented in software. Never runs a
     * benchmark, see {@link #benchmark()} for that.
     *
     * @return a policy based on the current JVM and CPU
     */
    @NotNull
    static CipherPreferencePolicy heuristic() {
        return CipherPolicies.heuristic(AesAcceleration.isLikelyAccelerated());
    }

    /**
     * Measures every supported encryption mode on this JVM and prefers the fastest ones. Blocks the calling thread
     * for about half a second, so call it once at startup, never on an event loop thread. Use
     * {@link CipherBenchmark} directly to configure how long it runs or to store the results.
     *
     * @return a policy preferring modes from the fastest to the slowest
     * @see CipherBenchmark#run()
     */
    @NotNull
    static CipherPreferencePolicy benchmark() {
        return CipherBenchmark.run().toPolicy();
    }
}
