package moe.kyokobot.koe.gateway;

/**
 * Voice gateway opcodes. Opcodes marked as undocumented are missing from Discord's documentation.
 */
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
    /** Undocumented. */
    public static final int ICECANDIDATES = 10;
    public static final int CLIENT_CONNECT = 11;
    /** Undocumented, sent by the client and the server. The SSRCs of a user's audio and video streams. */
    public static final int VIDEO = 12;
    public static final int CLIENT_DISCONNECT = 13;
    /** Undocumented. */
    public static final int CODECS = 14;
    /** Undocumented, sent by the client and the server. Which incoming streams the client wants, by SSRC. */
    public static final int MEDIA_SINK_WANTS = 15;
    /** Undocumented. The client sends it with empty data, the server replies with its voice and RTC worker versions. */
    public static final int VOICE_BACKEND_VERSION = 16;
    /** Undocumented. */
    public static final int CHANNEL_OPTIONS_UPDATE = 17;
    /** Undocumented, sent by the server. Flags of a user in the channel. */
    public static final int CLIENT_FLAGS = 18;
    /** Undocumented. */
    public static final int SPEED_TEST = 19;
    /** Undocumented, sent by the server. Platform of a user in the channel. */
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
    public static final int MLS_INVALID_COMMIT_WELCOME = 31;
    /** Undocumented. */
    public static final int NO_ROUTE = 32;
}
