package moe.kyokobot.koe.internal.dto.data;

import moe.kyokobot.koe.internal.dto.Data;

public class SessionDescription implements Data {
    public String mode;
    public String audioCodec;
    public int[] secretKey;
}
