package moe.kyokobot.koe.internal;

import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeImpl implements Koe {
    private final KoeOptions options;

    public KoeImpl(@NotNull KoeOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    @NotNull
    @Override
    public KoeClient newClient(long clientId) {
        return new KoeClientImpl(clientId, options);
    }

    @NotNull
    @Override
    public KoeOptions getOptions() {
        return options;
    }
}
