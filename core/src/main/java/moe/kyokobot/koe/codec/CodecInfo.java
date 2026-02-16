package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable codec capabilities and metadata.
 * Represents what a codec CAN do, not what it IS doing in a session.
 */
public abstract class CodecInfo {
    protected final String name;
    protected final CodecType type;
    protected final int defaultPriority;
    protected final byte defaultPayloadType;
    protected final byte defaultRtxPayloadType;

    protected CodecInfo(@NotNull String name, @NotNull CodecType type, byte defaultPayloadType,
                       byte defaultRtxPayloadType, int defaultPriority) {
        this.name = name;
        this.type = type;
        this.defaultPayloadType = defaultPayloadType;
        this.defaultRtxPayloadType = defaultRtxPayloadType;
        this.defaultPriority = defaultPriority;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public CodecType getType() {
        return type;
    }

    public byte getDefaultPayloadType() {
        return defaultPayloadType;
    }

    public byte getDefaultRtxPayloadType() {
        return defaultRtxPayloadType;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }

    /**
     * Creates a codec instance with negotiated payload types.
     *
     * @param negotiatedPayloadType the negotiated payload type for this session
     * @param negotiatedRtxPayloadType the negotiated RTX payload type for this session
     * @return a new CodecInstance with the negotiated payload types
     */
    @NotNull
    public CodecInstance instantiate(byte negotiatedPayloadType, byte negotiatedRtxPayloadType) {
        return new CodecInstance(this, negotiatedPayloadType, negotiatedRtxPayloadType);
    }

    /**
     * Creates a codec instance using default payload types (for Discord's simple protocol).
     *
     * @return a new CodecInstance with default payload types
     */
    @NotNull
    public CodecInstance instantiate() {
        return instantiate(defaultPayloadType, defaultRtxPayloadType);
    }

    /**
     * Converts this codec info to JSON format for gateway protocol.
     *
     * @return JSON representation of this codec
     */
    @NotNull
    public JsonObject toJson() {
        return new JsonObject()
                .add("name", name)
                .add("payload_type", defaultPayloadType)
                .add("priority", defaultPriority)
                .add("type", type.name().toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodecInfo codecInfo = (CodecInfo) o;
        return name.equals(codecInfo.name) && type == codecInfo.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
