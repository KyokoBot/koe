package moe.kyokobot.koe;

import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;

public interface KoeEventListener {
    default void gatewayError(Throwable cause) {
        //
    }

    default void gatewayReady(InetSocketAddress target, int ssrc) {
        //
    }

    /**
     * Called when the gateway connection is closed.
     *
     * @param code     the WebSocket close code.
     * @param reason   the close reason if present, null otherwise.
     * @param byRemote true if the connection was closed by the gateway, false if it was closed by Koe.
     */
    default void gatewayClosed(int code, @Nullable String reason, boolean byRemote) {
        //
    }

    /**
     * Called after {@link #gatewayClosed} when the voice server no longer knows the session and Koe couldn't start a
     * new one. Koe can't recover from this on its own, the bot has to rejoin the voice channel through the main
     * gateway (opcode 4) to get a new session and pass the new server info to {@link MediaConnection#connect}.
     *
     * @param code   the WebSocket close code.
     * @param reason the close reason if present, null otherwise.
     */
    default void sessionLost(int code, @Nullable String reason) {
        //
    }

    /**
     * Called when the stream information for a user has changed. Not state tracked by Koe, provides data from {@link moe.kyokobot.koe.gateway.Op#USER_SPEAKING} as-is.
     *
     * @param id        the user ID of the user whose stream information has changed.
     * @param audioSSRC the SSRC of the user's audio stream, or 0 if not present or just disabled.
     * @param videoSSRC the SSRC of the user's video stream, or 0 if not present or just disabled.
     * @param rtxSSRC   the SSRC of the user's retransmission stream, or 0 if not present or just disabled.
     */
    default void userStreamsChanged(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
        //
    }

    /**
     * Called when one or more users have connected to the voice channel.
     * Not state tracked by Koe, provides data from {@link moe.kyokobot.koe.gateway.Op#CLIENT_CONNECT} as-is.
     *
     * @param userIds the list of user IDs that have connected, as provided by the gateway.
     */
    default void usersConnected(List<String> userIds) {
        //
    }

    /**
     * Called when one or more users have disconnected from the voice channel.
     * Not state tracked by Koe, provides data from {@link moe.kyokobot.koe.gateway.Op#CLIENT_DISCONNECT} as-is.
     *
     * @param userIds the list of user IDs that have disconnected, as provided by the gateway.
     */
    default void userDisconnected(String id) {
        //
    }

    default void externalIPDiscovered(InetSocketAddress address) {
        //
    }

    default void sessionDescription(JsonObject session) {
        //
    }
}
