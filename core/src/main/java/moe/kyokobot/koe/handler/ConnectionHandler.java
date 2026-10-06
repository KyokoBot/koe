package moe.kyokobot.koe.handler;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.libdave.MediaType;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletionStage;

/**
 * This interface specifies Discord voice connection handler, allowing to implement other methods of establishing voice
 * connections/transmitting audio packets eg. TCP or browser/WebRTC way via ICE instead of their minimalistic custom
 * discovery protocol.
 *
 * @param <R> type of the result returned if connection succeeds
 */
@ApiStatus.NonExtendable
public interface ConnectionHandler<R> {
    void close();

    void handleSessionDescription(JsonObject object);

    default void sendFrame(CodecInstance codec, int timestamp, ByteBuf data, int start) {
        sendFrame(codec.getType(), codec.getPayloadType(), timestamp, data, start, false);
    }

    CompletionStage<R> connect();

    /**
     * @param extension true if {@code data} starts with an RTP header extension, including its 4 byte preamble. The
     *                  extension is sent as part of the RTP header and is not end-to-end encrypted.
     */
    void sendFrame(CodecType codecType, byte payloadType, int timestamp, ByteBuf data, int start, boolean extension);
}
