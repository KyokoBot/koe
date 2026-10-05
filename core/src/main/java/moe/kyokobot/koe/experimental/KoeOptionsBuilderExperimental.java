package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.KoeOptionsBuilder;
import moe.kyokobot.koe.experimental.codec.ExperimentalCodecRegistry;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;
import moe.kyokobot.libdave.netty.NettyDaveFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeOptionsBuilderExperimental extends KoeOptionsBuilder {
    public KoeOptionsBuilderExperimental() {
        super();
        this.codecRegistry = new ExperimentalCodecRegistry();
        this.experimental = true;
    }

    /**
     * Sets the policy which picks the transport encryption mode of voice sessions out of the ones offered by the voice
     * server. Defaults to {@link CipherPreferencePolicy#heuristic()}.
     *
     * @param cipherPreferencePolicy the policy to use
     * @see CipherPreferencePolicy#benchmark()
     */
    public KoeOptionsBuilderExperimental setCipherPreferencePolicy(@NotNull CipherPreferencePolicy cipherPreferencePolicy) {
        this.cipherPreferencePolicy = Objects.requireNonNull(cipherPreferencePolicy);
        return this;
    }

    /**
     * Sets a custom {@link NettyDaveFactory} provider function. This function will be called with a boolean indicating
     * whether the DAVE log sink is enabled, and should return a new instance of {@link NettyDaveFactory}.
     * If this is not set, the default factory will be used.
     *
     * @param daveFactoryProvider the custom factory provider function
     */
    public KoeOptionsBuilderExperimental setDaveFactoryProvider(@NotNull DaveFactoryProvider daveFactoryProvider) {
        this.daveFactoryProvider = Objects.requireNonNull(daveFactoryProvider);
        return this;
    }

    public KoeOptionsExperimental create() {
        return (KoeOptionsExperimental) super.create();
    }
}
