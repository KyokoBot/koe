package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;

/**
 * Opus audio codec information.
 */
public class OpusCodecInfo extends CodecInfo {
    public static final OpusCodecInfo INSTANCE = new OpusCodecInfo();
    public static final byte DEFAULT_PAYLOAD_TYPE = (byte) 120;
    public static final int FRAME_DURATION = 20;
    public static final byte[] SILENCE_FRAME = new byte[] {(byte)0xF8, (byte)0xFF, (byte)0xFE};

    private OpusCodecInfo() {
        super("opus", CodecType.AUDIO, DEFAULT_PAYLOAD_TYPE, (byte) 0, 1000);
    }
}
