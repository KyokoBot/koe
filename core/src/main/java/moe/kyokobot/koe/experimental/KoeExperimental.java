package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.internal.KoeClientImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KoeExperimental {
    private final KoeOptionsExperimental options;

    private KoeExperimental(@NotNull KoeOptionsExperimental options) {
        if (!options.isExperimental()) {
            throw new IllegalArgumentException("Provided options are not marked as experimental. Please use KoeOptionsExperimental.builder() to create options for KoeExperimental.");
        }

        this.options = Objects.requireNonNull(options);
    }

    /**
     * @param clientId the ID of the user or bot which will connect to Discord voice servers.
     * @return a new Koe client
     */
    @NotNull
    public KoeClientExperimental newClient(long clientId) {
        return new KoeClientImpl(clientId, options);
    }

    /**
     * @return Options of current Koe instance
     */
    @NotNull
    public KoeOptionsExperimental getOptions() {
        return options;
    }

    /**
     * Create a new Koe instance with given options.
     *
     * @param options Options used by new Koe instance.
     * @return A new Koe instance.
     */
    @NotNull
    public static KoeExperimental koe(@NotNull KoeOptionsExperimental options) {
        return new KoeExperimental(options);
    }

    /**
     * Create a new Koe instance with default options.
     *
     * @return A new Koe instance.
     */
    @NotNull
    public static KoeExperimental koe() {
        return new KoeExperimental(KoeOptionsExperimental.builder().create());
    }
}
