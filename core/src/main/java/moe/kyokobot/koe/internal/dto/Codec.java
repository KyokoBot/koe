package moe.kyokobot.koe.internal.dto;

import org.jetbrains.annotations.Nullable;

public class Codec {
    public String name;
    public int payloadType;
    public int priority;
    public String type;
    public @Nullable Integer rtxPayloadType;

    public Codec(String name, int payloadType, int priority, String type) {
        this.name = name;
        this.payloadType = payloadType;
        this.priority = priority;
        this.type = type;
    }
}
