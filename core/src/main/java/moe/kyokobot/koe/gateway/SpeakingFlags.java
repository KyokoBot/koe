package moe.kyokobot.koe.gateway;

public class SpeakingFlags {
    private SpeakingFlags() {
        //
    }

    public static final int NORMAL = 1;
    public static final int SOUND_SHARE = 1 << 1;
    public static final int PRIORITY = 1 << 2;
}
