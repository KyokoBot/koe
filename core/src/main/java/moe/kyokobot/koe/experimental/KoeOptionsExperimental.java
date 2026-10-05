package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.experimental.crypto.CipherPreferencePolicy;
import moe.kyokobot.libdave.netty.NettyDaveFactory;
import org.jetbrains.annotations.NotNull;

public interface KoeOptionsExperimental extends KoeOptions {
    /**
     * Creates a new {@link KoeOptionsBuilderExperimental} instance with experimental defaults.
     * Required to use with Koe created through {@link moe.kyokobot.koe.experimental.KoeExperimental} to access experimental features.
     *
     * @return A new {@link KoeOptionsBuilderExperimental} instance.
     */
    static KoeOptionsBuilderExperimental builder() {
        return new KoeOptionsBuilderExperimental();
    }

    /**
     * @return the policy which picks the transport encryption mode of voice sessions
     */
    @NotNull
    CipherPreferencePolicy getCipherPreferencePolicy();

    /**
     * @return a custom {@link NettyDaveFactory} provider function, or null if not set.
     */
    @NotNull DaveFactoryProvider getDaveFactoryProvider();
}
