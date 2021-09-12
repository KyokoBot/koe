package moe.kyokobot.koe.internal.dto.data;

import moe.kyokobot.koe.internal.dto.Data;
import moe.kyokobot.koe.internal.dto.StreamInfo;
import org.jetbrains.annotations.Nullable;

public class ClientConnect implements Data {
    public @Nullable String userId;
    public int audioSsrc;
    public int videoSsrc;
    public int rtxSsrc;
    public @Nullable StreamInfo[] streams;

    public ClientConnect() {
        this.audioSsrc = 0;
        this.videoSsrc = 0;
        this.rtxSsrc = 0;
    }

    public ClientConnect(int audioSsrc, int videoSsrc, int rtxSsrc) {
        this.audioSsrc = audioSsrc;
        this.videoSsrc = videoSsrc;
        this.rtxSsrc = rtxSsrc;
    }

    public ClientConnect(int audioSsrc,
                         int videoSsrc,
                         int rtxSsrc,
                         @Nullable StreamInfo[] streams) {
        this.audioSsrc = audioSsrc;
        this.videoSsrc = videoSsrc;
        this.rtxSsrc = rtxSsrc;
        this.streams = streams;
    }
}
