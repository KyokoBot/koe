package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A codec instance in an active media session.
 * Contains negotiated payload types that may differ from defaults.
 */
public class CodecInstance {
    private final CodecInfo info;
    private final byte payloadType;
    private final byte rtxPayloadType;

    CodecInstance(@NotNull CodecInfo info, byte payloadType, byte rtxPayloadType) {
        this.info = Objects.requireNonNull(info, "info cannot be null");
        this.payloadType = payloadType;
        this.rtxPayloadType = rtxPayloadType;
    }

    @NotNull
    public String getName() {
        return info.getName();
    }

    @NotNull
    public CodecType getType() {
        return info.getType();
    }

    public byte getPayloadType() {
        return payloadType;
    }

    public byte getRetransmissionPayloadType() {
        return rtxPayloadType;
    }

    public int getPriority() {
        return info.getDefaultPriority();
    }

    @NotNull
    public CodecInfo getInfo() {
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodecInstance that = (CodecInstance) o;
        return payloadType == that.payloadType &&
                rtxPayloadType == that.rtxPayloadType &&
                info.equals(that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, payloadType, rtxPayloadType);
    }

    @Override
    public String toString() {
        return "CodecInstance{" +
                "name='" + getName() + '\'' +
                ", type=" + getType() +
                ", payloadType=" + payloadType +
                ", rtxPayloadType=" + rtxPayloadType +
                '}';
    }
}
