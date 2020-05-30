package moe.kyokobot.koe.codec;

public class VP8Codec extends Codec {
    public static final byte PAYLOAD_TYPE = (byte) 103;
    public static final byte RTX_PAYLOAD_TYPE = (byte) 104;
    public static final VP8Codec INSTANCE = new VP8Codec();

    public VP8Codec() {
        super("VP8", PAYLOAD_TYPE, RTX_PAYLOAD_TYPE, 2000, CodecType.VIDEO);
    }
}
