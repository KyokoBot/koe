package moe.kyokobot.koe.codec;

public class H264Codec extends Codec {
    public static final byte PAYLOAD_TYPE = (byte) 101;
    public static final byte RTX_PAYLOAD_TYPE = (byte) 102;
    public static final H264Codec INSTANCE = new H264Codec();

    public H264Codec() {
        super("H264", PAYLOAD_TYPE, RTX_PAYLOAD_TYPE, 1000, CodecType.VIDEO);
    }
}
