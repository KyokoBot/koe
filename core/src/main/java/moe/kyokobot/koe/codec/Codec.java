package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class Codec {
    private final String name;
    private final byte payloadType;
    private final int priority;
    private final CodecType type;
    private final JsonObject jsonDescription;

    protected Codec(String name, byte payloadType, int priority, CodecType type) {
        this.name = name;
        this.payloadType = payloadType;
        this.priority = priority;
        this.type = type;

        this.jsonDescription = new JsonObject()
                .add("name", name)
                .add("payload_type", payloadType)
                .add("priority", priority)
                .add("type", type.name().toLowerCase());
    }

    public String getName() {
        return name;
    }

    public byte getPayloadType() {
        return payloadType;
    }

    public int getPriority() {
        return priority;
    }

    public CodecType getType() {
        return type;
    }

    public JsonObject getJsonDescription() {
        return jsonDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Codec that = (Codec) o;
        return payloadType == that.payloadType &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadType, type);
    }

    /**
     * Gets audio codec description by name.
     * @param name the codec name
     * @return Codec instance or null if the codec is not found/supported by Koe.
     */
    @Nullable
    public static Codec getAudio(String name) {
        return DefaultCodecs.audioCodecs.get(name);
    }

    /**
     * Gets video codec description by name.
     * @param name the codec name
     * @return Codec instance or null if the codec is not found/supported by Koe.
     */
    @Nullable
    public static Codec getVideo(String name) {
        return DefaultCodecs.audioCodecs.get(name);
    }

    /**
     * Gets audio or video codec by payload type.
     * @param payloadType the payload type
     * @return Codec instance or null if the codec is not found/supported by Koe.
     */
    @Nullable
    public static Codec getByPayload(byte payloadType) {
        for (var codec : DefaultCodecs.audioCodecs.values()) {
            if (codec.getPayloadType() == payloadType) {
                return codec;
            }
        }

        for (var codec : DefaultCodecs.videoCodecs.values()) {
            if (codec.getPayloadType() == payloadType) {
                return codec;
            }
        }

        return null;
    }
}
