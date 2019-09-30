package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.internal.json.JsonObject;

public abstract class AbstractCodec implements Codec {
    private final String name;
    private final byte payloadType;
    private final int priority;
    private final CodecType type;
    private final JsonObject jsonDescription;

    protected AbstractCodec(String name, byte payloadType, int priority, CodecType type) {
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte getPayloadType() {
        return payloadType;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public CodecType getType() {
        return type;
    }

    @Override
    public JsonObject getJsonDescription() {
        return jsonDescription;
    }
}
