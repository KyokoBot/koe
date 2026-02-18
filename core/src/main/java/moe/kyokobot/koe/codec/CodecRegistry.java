package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Registry for managing available codec capabilities.
 * Allows users to register custom codecs.
 */
public interface CodecRegistry {
    /**
     * Registers a codec capability.
     *
     * @param codec the codec info to register
     * @throws IllegalArgumentException if codec with same name already exists
     */
    void register(@NotNull CodecInfo codec);

    /**
     * Unregisters a codec by name.
     *
     * @param codecName the name of the codec to unregister
     * @return true if codec was registered, false if not found
     */
    boolean unregister(@NotNull String codecName);

    /**
     * Gets codec info by name.
     *
     * @param name the codec name (case-insensitive)
     * @return CodecInfo or null if not found
     */
    @Nullable
    CodecInfo getByName(@NotNull String name);

    /**
     * Gets all registered audio codecs.
     *
     * @return collection of audio codec infos
     */
    @NotNull
    Collection<CodecInfo> getAudioCodecs();

    /**
     * Gets all registered video codecs.
     *
     * @return collection of video codec infos
     */
    @NotNull
    Collection<CodecInfo> getVideoCodecs();

    /**
     * Gets all registered codecs (audio + video).
     *
     * @return collection of all codec infos
     */
    @NotNull
    Collection<CodecInfo> getAllCodecs();
}
