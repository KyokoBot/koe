package moe.kyokobot.koe.codec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Returns whether the given codec instance is Opus.
     *
     * @param instance the codec instance to check, may be null
     * @return true if instance is non-null and its codec is Opus
     */
    public static boolean isInstanceOf(@Nullable CodecInstance instance) {
        return instance != null && INSTANCE.equals(instance.getInfo());
    }
}
