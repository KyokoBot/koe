package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.CodecInfo;
import moe.kyokobot.koe.gateway.MediaValve;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Adds the video sink wants opcode, used by the {@link MediaValve}. Koe also announces video codecs from this version
 * on.
 */
public class MediaGatewayV5Connection extends AbstractMediaGatewayConnection {
    private final MediaValve mediaValve = new MediaValveImpl(this);

    public MediaGatewayV5Connection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        this(connection, voiceServerInfo, 5);
    }

    protected MediaGatewayV5Connection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo, int version) {
        super(connection, voiceServerInfo, version);
    }

    @Nullable
    @Override
    public MediaValve getValve() {
        return this.mediaValve;
    }

    // With the `video` flag the server sends MEDIA_SINK_WANTS. At time of writing this comment Discord forces it to
    // false on bots (so.. user bot time? /s) due to voice server bug that broke clients or something.
    // After receiving this opcode client can send op 12 with ssrcs for video (audio + 1)
    // and retransmission (audio + 2, not required but results in graphical issues if user joins a VC
    // or even resizes the window) and start sending video data according to received quality hint -
    // so if (d.any < 100) in this payload, the client should send video data with lowered resolution
    // and bitrate.
    @Override
    protected JsonObject identifyPayload() {
        return super.identifyPayload().add("video", true);
    }

    @Override
    protected JsonObject resumePayload() {
        return super.resumePayload().add("video", true);
    }

    @Override
    protected Collection<CodecInfo> codecs() {
        return connection.getOptions().getCodecRegistry().getAllCodecs();
    }

    @Override
    protected void onReady() {
        mediaValve.sendToGateway();
    }

    @Override
    protected void onStreamsChanged(JsonObject payload) {
        mediaValve.handleEvent(payload);
    }

    @Override
    protected void onUserDisconnected(String userId) {
        mediaValve.removeUser(userId);
    }

    @Override
    protected void onProtocolSelected() {
        updateSpeaking(0);
    }
}
