package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.KoeOptions;

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
}
