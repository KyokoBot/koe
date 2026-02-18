package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of CodecRegistry with built-in codecs pre-registered.
 */
public class DefaultCodecRegistry implements CodecRegistry {
    protected final ConcurrentHashMap<String, CodecInfo> codecsByName = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Byte, CodecInfo> codecsByPayloadType = new ConcurrentHashMap<>();

    public DefaultCodecRegistry(boolean registerBuiltins) {
        if (registerBuiltins) {
            registerBuiltInCodecs();
        }
    }

    /**
     * Creates a registry with all built-in codecs pre-registered.
     */
    public DefaultCodecRegistry() {
        this(true);
    }

    /**
     * Creates an empty registry with no codecs registered.
     * Use this if you want to register only specific codecs.
     *
     * @return an empty CodecRegistry
     */
    public static DefaultCodecRegistry empty() {
        return new DefaultCodecRegistry(false);
    }

    /**
     * Registers all built-in codecs. Can be overridden to skip registration.
     */
    protected void registerBuiltInCodecs() {
        register(OpusCodecInfo.INSTANCE);
    }

    @Override
    public void register(@NotNull CodecInfo codec) {
        String name = codec.getName().toLowerCase();

        if (codecsByName.containsKey(name)) {
            throw new IllegalArgumentException("Codec already registered: " + name);
        }

        codecsByName.put(name, codec);
        codecsByPayloadType.put(codec.getDefaultPayloadType(), codec);

        if (codec.getDefaultRtxPayloadType() != 0) {
            codecsByPayloadType.put(codec.getDefaultRtxPayloadType(), codec);
        }
    }

    @Override
    public boolean unregister(@NotNull String codecName) {
        CodecInfo removed = codecsByName.remove(codecName.toLowerCase());
        if (removed != null) {
            codecsByPayloadType.remove(removed.getDefaultPayloadType());
            if (removed.getDefaultRtxPayloadType() != 0) {
                codecsByPayloadType.remove(removed.getDefaultRtxPayloadType());
            }
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public CodecInfo getByName(@NotNull String name) {
        return codecsByName.get(name.toLowerCase());
    }

    @Override
    @NotNull
    public Collection<CodecInfo> getAudioCodecs() {
        return codecsByName.values().stream()
                .filter(c -> c.getType() == CodecType.AUDIO)
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public Collection<CodecInfo> getVideoCodecs() {
        return codecsByName.values().stream()
                .filter(c -> c.getType() == CodecType.VIDEO)
                .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public Collection<CodecInfo> getAllCodecs() {
        return new ArrayList<>(codecsByName.values());
    }
}
