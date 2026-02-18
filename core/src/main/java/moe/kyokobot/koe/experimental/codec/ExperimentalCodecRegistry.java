package moe.kyokobot.koe.experimental.codec;

import moe.kyokobot.koe.codec.DefaultCodecRegistry;

public class ExperimentalCodecRegistry extends DefaultCodecRegistry {
    @Override
    protected void registerBuiltInCodecs() {
        super.registerBuiltInCodecs();

        register(H264CodecInfo.INSTANCE);
        register(VP8CodecInfo.INSTANCE);
        register(VP9CodecInfo.INSTANCE);
    }
}
