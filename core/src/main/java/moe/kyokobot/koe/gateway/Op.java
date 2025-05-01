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
    public static final int CLIENT_CONNECT = 11;
    public static final int VIDEO = 12;
    public static final int CLIENT_DISCONNECT = 13;
    public static final int CODECS = 14;
    public static final int MEDIA_SINK_WANTS = 15;
    /**
     * @deprecated Use {@link #MEDIA_SINK_WANTS} instead.
     */
    public static final int VIDEO_SINK_WANTS = 15;
    public static final int VOICE_BACKEND_VERSION = 16;
    public static final int CHANNEL_OPTIONS_UPDATE = 17;
    public static final int CLIENT_FLAGS = 18;
    public static final int SPEED_TEST = 19;
    public static final int PLATFORM = 20;
    public static final int SECURE_FRAMES_PREPARE_PROTOCOL_TRANSITION = 21;
    public static final int SECURE_FRAMES_EXECUTE_TRANSITION = 22;
    public static final int SECURE_FRAMES_READY_FOR_TRANSITION = 23;
    public static final int SECURE_FRAMES_PREPARE_EPOCH = 24;
    public static final int MLS_EXTERNAL_SENDER_PACKAGE = 25;
    public static final int MLS_KEY_PACKAGE = 26;
    public static final int MLS_PROPOSALS = 27;
    public static final int MLS_COMMIT_WELCOME = 28;
    public static final int MLS_PREPARE_COMMIT_TRANSITION = 29;
    public static final int MLS_WELCOME = 30;
}
