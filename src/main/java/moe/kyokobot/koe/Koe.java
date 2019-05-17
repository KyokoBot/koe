package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Koe {
    private final KoeOptions options;

    public Koe(@NotNull KoeOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    /**
     * @param clientId the ID of the user or bot which will connect to Discord voice servers.
     * @return a new Koe client
     */
    @NotNull
    public KoeClient newClient(long clientId) {
        return new KoeClient(clientId);
    }

    /**
     * @return Options of current Koe instance
     */
    @NotNull
    public KoeOptions getOptions() {
        return options;
    }

    /**
     * Create a new Koe instance with given options.
     *
     * @param options Options used by new Koe instance.
     * @return A new Koe instance.
     */
    @NotNull
    public static Koe koe(@NotNull KoeOptions options) {
        return new Koe(options);
    }

    /**
     * Create a new Koe instance with default options.
     *
     * @return A new Koe instance.
     */
    @NotNull
    public static Koe koe() {
        return new Koe(KoeOptions.defaultOptions());
    }
}
