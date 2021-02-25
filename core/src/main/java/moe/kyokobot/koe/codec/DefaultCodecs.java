package moe.kyokobot.koe.codec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// todo: migrate to codec registry or something
public class DefaultCodecs {
    private DefaultCodecs() {
        //
    }

    public static final Map<String, Codec> audioCodecs;
    public static final Map<String, Codec> videoCodecs;

    static {
        audioCodecs = new HashMap<>();
        audioCodecs.put("opus", OpusCodec.INSTANCE);

        videoCodecs = new HashMap<>();
        videoCodecs.put("H264", H264Codec.INSTANCE);
        videoCodecs.put("VP8", VP8Codec.INSTANCE);
        videoCodecs.put("VP9", VP9Codec.INSTANCE);
    }
}
