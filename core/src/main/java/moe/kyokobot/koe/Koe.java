package moe.kyokobot.koe;

import moe.kyokobot.koe.internal.KoeImpl;
import org.jetbrains.annotations.NotNull;

public interface Koe {
    /**
     * @param clientId the ID of the user or bot which will connect to Discord voice servers.
     * @return a new Koe client
     */
    @NotNull
    KoeClient newClient(long clientId);

    /**
     * @return Options of current Koe instance
     */
    @NotNull
    KoeOptions getOptions();

    /**
     * Create a new Koe instance with given options.
     *
     * @param options Options used by new Koe instance.
     * @return A new Koe instance.
     */
    @NotNull
    static Koe koe(@NotNull KoeOptions options) {
        return new KoeImpl(options);
    }

    /**
     * Create a new Koe instance with default options.
     *
     * @return A new Koe instance.
     */
    @NotNull
    static Koe koe() {
        return new KoeImpl(KoeOptions.defaultOptions());
    }
}
