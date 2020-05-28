package moe.kyokobot.koe.codec;

public class VP9Codec extends Codec {
    public static final byte PAYLOAD_TYPE = (byte) 105;
    public static final byte RTX_PAYLOAD_TYPE = (byte) 106;
    public static final VP9Codec INSTANCE = new VP9Codec();

    public VP9Codec() {
        super("VP9", PAYLOAD_TYPE, RTX_PAYLOAD_TYPE, 3000, CodecType.VIDEO);
    }
}
