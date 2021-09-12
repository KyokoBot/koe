package moe.kyokobot.koe.handler;

import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.internal.dto.data.SessionDescription;

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

    void handleSessionDescription(SessionDescription object);

    default void sendFrame(Codec codec, int timestamp, int ssrc, ByteBuf data, int start) {
        sendFrame(codec.getPayloadType(), timestamp, ssrc, data, start, false);
    }

    CompletionStage<R> connect();

    void sendFrame(byte payloadType, int timestamp, int ssrc, ByteBuf data, int start, boolean extension);
}
