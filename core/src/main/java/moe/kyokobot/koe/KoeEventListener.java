package moe.kyokobot.koe;

import moe.kyokobot.koe.internal.json.JsonObject;

import java.net.InetSocketAddress;

public interface KoeEventListener {
    void gatewayReady(InetSocketAddress target, int ssrc);

    void gatewayClosed(int code, String reason);

    void userConnected(String id, int audioSSRC, int videoSSRC);

    void userDisconnected(String id);

    void externalIPDiscovered(InetSocketAddress address);

    void sessionDescription(JsonObject session);
}
