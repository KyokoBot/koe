package moe.kyokobot.koe.internal.dto.data;

import moe.kyokobot.koe.internal.dto.Data;

public class Speaking implements Data {
    public Speaking(int speaking, int delay, int ssrc) {
        this.speaking = speaking;
        this.delay = delay;
        this.ssrc = ssrc;
    }
    public int speaking;
    public int delay;
    public int ssrc;
}
