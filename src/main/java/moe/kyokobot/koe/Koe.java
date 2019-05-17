package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Koe {
    private final KoeOptions options;

    public Koe(@NotNull KoeOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    @NotNull
    public KoeClient newClient(long clientId) {
        return new KoeClient(clientId);
    }

    @NotNull
    public KoeOptions getOptions() {
        return options;
    }

    @NotNull
    public static Koe koe(@NotNull KoeOptions options) {
        return new Koe(options);
    }

    @NotNull
    public static Koe koe() {
        return new Koe(KoeOptions.defaultOptions());
    }
}
