package moe.kyokobot.koe;

import moe.kyokobot.koe.internal.json.JsonObject;

import java.net.InetSocketAddress;

public class KoeEventAdapter implements KoeEventListener {
    @Override
    public void gatewayError(Throwable cause) {
        //
    }

    @Override
    public void gatewayReady(InetSocketAddress target, int ssrc) {
        //
    }

    @Override
    public void gatewayClosed(int code, String reason, boolean byRemote) {
        //
    }

    @Override
    public void userConnected(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
        //
    }

    @Override
    public void userDisconnected(String id) {
        //
    }

    @Override
    public void externalIPDiscovered(InetSocketAddress address) {
        //
    }

    @Override
    public void sessionDescription(JsonObject session) {
        //
    }
}
