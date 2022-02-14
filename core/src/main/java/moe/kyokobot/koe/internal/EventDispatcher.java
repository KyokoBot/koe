package moe.kyokobot.koe.internal;

import moe.kyokobot.koe.KoeEventListener;
import moe.kyokobot.koe.internal.json.JsonObject;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EventDispatcher implements KoeEventListener {
    private final Set<KoeEventListener> listeners;

    EventDispatcher() {
        this.listeners = new HashSet<>();
    }

    void register(KoeEventListener listener) {
        if (Objects.requireNonNull(listener) == this) {
            throw new IllegalArgumentException("Are you trying to register the dispatcher, rly?");
        }
        
        listeners.add(listener);
    }

    void unregister(KoeEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void gatewayError(Throwable cause) {
        for (var listener : listeners) {
            listener.gatewayError(cause);
        }
    }

    @Override
    public void gatewayReady(InetSocketAddress target, int ssrc) {
        for (var listener : listeners) {
            listener.gatewayReady(target, ssrc);
        }
    }

    @Override
    public void gatewayClosed(int code, String reason, boolean byRemote) {
        for (var listener : listeners) {
            listener.gatewayClosed(code, reason, byRemote);
        }
    }

    @Override
    public void userConnected(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
        for (var listener : listeners) {
            listener.userConnected(id, audioSSRC, videoSSRC, rtxSSRC);
        }
    }

    @Override
    public void userDisconnected(String id) {
        for (var listener : listeners) {
            listener.userDisconnected(id);
        }
    }

    @Override
    public void externalIPDiscovered(InetSocketAddress address) {
        for (var listener : listeners) {
            listener.externalIPDiscovered(address);
        }
    }

    @Override
    public void sessionDescription(JsonObject session) {
        for (var listener : listeners) {
            listener.sessionDescription(session);
        }
    }
}
