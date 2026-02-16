package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;

/**
 * VP8 video codec information.
 */
public class VP8CodecInfo extends CodecInfo {
    public static final VP8CodecInfo INSTANCE = new VP8CodecInfo();
    public static final byte DEFAULT_PAYLOAD_TYPE = (byte) 103;
    public static final byte DEFAULT_RTX_PAYLOAD_TYPE = (byte) 104;

    private VP8CodecInfo() {
        super("VP8", CodecType.VIDEO, DEFAULT_PAYLOAD_TYPE, DEFAULT_RTX_PAYLOAD_TYPE, 2000);
    }
}
