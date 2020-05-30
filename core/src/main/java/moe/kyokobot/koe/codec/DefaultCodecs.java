package moe.kyokobot.koe.codec;

import java.util.Map;

// todo: migrate to codec registry or something
public class DefaultCodecs {
    private DefaultCodecs() {
        //
    }

    public static final Map<String, Codec> audioCodecs;
    public static final Map<String, Codec> videoCodecs;

    static {
        audioCodecs = Map.of(
                "opus", OpusCodec.INSTANCE
        );

        videoCodecs = Map.of(
                "H264", H264Codec.INSTANCE,
                "VP8", VP8Codec.INSTANCE,
                "VP9", VP9Codec.INSTANCE
        );
    }
}
