package moe.kyokobot.koe.codec;

public class OpusCodec extends Codec {
    public static final byte PAYLOAD_TYPE = (byte) 120;
    public static final int FRAME_DURATION = 20;
    public static final OpusCodec INSTANCE = new OpusCodec();
    public static final byte[] SILENCE_FRAME = new byte[] {(byte)0xF8, (byte)0xFF, (byte)0xFE};

    public OpusCodec() {
        super("opus", PAYLOAD_TYPE, 1000, CodecType.AUDIO);
    }
}
