package moe.kyokobot.koe.handler;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.internal.json.JsonObject;

import java.util.concurrent.CompletionStage;

/**
 * This interface specifies Discord voice connection handler, allowing to implement other methods of establishing voice
 * connections/transmitting audio packets eg. TCP or browser/WebRTC way via ICE instead of their minimalistic custom
 * discovery protocol.
 *
 * @param <R> type of the result returned if connection succeeds
 */
public interface ConnectionHandler<R> {
    void close();

    void handleSessionDescription(JsonObject object);

    default void sendFrame(Codec codec, int timestamp, ByteBuf data, int start) {
        sendFrame(codec.getPayloadType(), timestamp, data, start, false);
    }

    CompletionStage<R> connect();

    void sendFrame(byte payloadType, int timestamp, ByteBuf data, int start, boolean extension);
}
