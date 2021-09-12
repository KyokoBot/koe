package moe.kyokobot.koe.internal.dto;

public class StreamInfo {
    public String type;
    public String rid;
    public int ssrc;
    public int rtxSsrc;
    public boolean active;
    public int quality;
    public int maxBitrate;
    public int maxFramerate;
    public Resolution maxResolution;

    public static class Resolution {
        public String type;
        public int width;
        public int height;
    }
}