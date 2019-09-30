package moe.kyokobot.koe;

import moe.kyokobot.koe.internal.json.JsonObject;

import java.net.InetSocketAddress;

public class KoeEventAdapter implements KoeEventListener {
    @Override
    public void gatewayReady(InetSocketAddress target, int ssrc) {
        //
    }

    @Override
    public void gatewayClosed(int code, String reason) {
        //
    }

    @Override
    public void userConnected(String id, int audioSSRC, int videoSSRC) {
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
