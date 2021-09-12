package moe.kyokobot.koe.internal.dto.data;

import moe.kyokobot.koe.internal.dto.Data;

public class Ready implements Data {
    public int port;
    public String ip;
    public int ssrc;
    public String[] modes;
}
