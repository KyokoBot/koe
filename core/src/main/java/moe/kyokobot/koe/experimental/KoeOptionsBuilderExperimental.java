package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.KoeOptionsBuilder;
import moe.kyokobot.koe.experimental.codec.ExperimentalCodecRegistry;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;
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

    public KoeOptionsExperimental create() {
        return (KoeOptionsExperimental) super.create();
    }
}
