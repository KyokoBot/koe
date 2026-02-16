package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;

/**
 * H.264 video codec information.
 */
public class H264CodecInfo extends CodecInfo {
    public static final H264CodecInfo INSTANCE = new H264CodecInfo();
    public static final byte DEFAULT_PAYLOAD_TYPE = (byte) 101;
    public static final byte DEFAULT_RTX_PAYLOAD_TYPE = (byte) 102;

    private H264CodecInfo() {
        super("H264", CodecType.VIDEO, DEFAULT_PAYLOAD_TYPE, DEFAULT_RTX_PAYLOAD_TYPE, 1000);
    }
}
