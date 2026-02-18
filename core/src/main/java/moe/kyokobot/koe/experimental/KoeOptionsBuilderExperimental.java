package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.KoeOptionsBuilder;
import moe.kyokobot.koe.experimental.codec.ExperimentalCodecRegistry;

public class KoeOptionsBuilderExperimental extends KoeOptionsBuilder {
    public KoeOptionsBuilderExperimental() {
        super();
        this.codecRegistry = new ExperimentalCodecRegistry();
        this.experimental = true;
    }

    public KoeOptionsExperimental create() {
        return (KoeOptionsExperimental) super.create();
    }
}
