package moe.kyokobot.koe.gateway;

public class Op {
    private Op() {
        //
    }

    public static final int IDENTIFY = 0;
    public static final int SELECT_PROTOCOL = 1;
    public static final int READY = 2;
    public static final int HEARTBEAT = 3;
    public static final int SESSION_DESCRIPTION = 4;
    public static final int SPEAKING = 5;
    public static final int HEARTBEAT_ACK = 6;
    public static final int RESUME = 7;
    public static final int HELLO = 8;
    public static final int RESUMED = 9;
    // public static final int DUNNO = 10;
    // public static final int DUNNO = 11;
    public static final int CLIENT_CONNECT = 12; // thx b1nzy
    public static final int CLIENT_DISCONNECT = 13;
    public static final int CODECS = 14;
    public static final int VIDEO_SINK_WANTS = 15;
}
