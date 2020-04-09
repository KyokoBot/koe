package moe.kyokobot.koe.codec;

public class OpusCodec extends Codec {
    public static final byte PAYLOAD_TYPE = (byte) 120;
    public static final OpusCodec INSTANCE = new OpusCodec();

    public OpusCodec() {
        super("opus", PAYLOAD_TYPE, 100, CodecType.AUDIO);
    }
}
