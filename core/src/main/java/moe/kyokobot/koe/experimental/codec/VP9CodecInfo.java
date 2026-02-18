package moe.kyokobot.koe.experimental.codec;

import moe.kyokobot.koe.codec.CodecInfo;
import moe.kyokobot.koe.codec.CodecType;

/**
 * VP9 video codec information.
 */
public class VP9CodecInfo extends CodecInfo {
    public static final VP9CodecInfo INSTANCE = new VP9CodecInfo();
    public static final byte DEFAULT_PAYLOAD_TYPE = (byte) 105;
    public static final byte DEFAULT_RTX_PAYLOAD_TYPE = (byte) 106;

    private VP9CodecInfo() {
        super("VP9", CodecType.VIDEO, DEFAULT_PAYLOAD_TYPE, DEFAULT_RTX_PAYLOAD_TYPE, 3000);
    }
}
