package moe.kyokobot.koe;

import moe.kyokobot.koe.internal.json.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

public interface KoeEventListener {
    void gatewayError(Throwable cause);

    void gatewayReady(InetSocketAddress target, int ssrc);

    void gatewayClosed(int code, @Nullable String reason, boolean byRemote);

    void userConnected(String id, int audioSSRC, int videoSSRC, int rtxSSRC);

    void userDisconnected(String id);

    void externalIPDiscovered(InetSocketAddress address);

    void sessionDescription(JsonObject session);
}
