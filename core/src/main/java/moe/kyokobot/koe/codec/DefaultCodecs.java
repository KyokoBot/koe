package moe.kyokobot.koe.codec;

import java.util.Map;

class DefaultCodecs {
    private DefaultCodecs() {
        //
    }

    static final Map<String, Codec> audioCodecs;
    static final Map<String, Codec> videoCodecs;

    static {
        audioCodecs = Map.of("opus", new OpusCodec());

        videoCodecs = Map.of();
    }
}
