package moe.kyokobot.koe.codec;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;

public interface Codec {
    String getName();

    byte getPayloadType();

    int getPriority();

    CodecType getType();

    JsonObject getJsonDescription();

    FramePoller createFramePoller(VoiceConnection connection);

    /**
     * Gets audio codec description by name.
     * @param name the codec name
     * @return Codec instance or null if the codec is not found/supported by Koe.
     */
    @Nullable
    static Codec getAudio(String name) {
        return DefaultCodecs.audioCodecs.get(name);
    }

    /**
     * Gets video codec description by name.
     * @param name the codec name
     * @return Codec instance or null if the codec is not found/supported by Koe.
     */
    @Nullable
    static Codec getVideo(String name) {
        return DefaultCodecs.audioCodecs.get(name);
    }

    /**
     * Gets audio or video codec by payload type.
     * @param payloadType the payload type
     * @return Codec instance or null if the codec is not found/supported by Koe.
     */
    @Nullable
    static Codec getByPayload(byte payloadType) {
        for (var codec : DefaultCodecs.audioCodecs.values()) {
            if (codec.getPayloadType() == payloadType) {
                return codec;
            }
        }

        for (var codec : DefaultCodecs.videoCodecs.values()) {
            if (codec.getPayloadType() == payloadType) {
                return codec;
            }
        }

        return null;
    }
}
