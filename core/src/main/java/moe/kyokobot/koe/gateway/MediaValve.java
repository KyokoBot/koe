package moe.kyokobot.koe.gateway;

import moe.kyokobot.koe.internal.json.JsonObject;

public interface MediaValve {
    /**
     * Whether we're deafened, i.e., we are not receiving audio from any user.
     */
    boolean isDeafened();

    /**
     * Set whether to deafen ourselves.
     * <p>
     * You must call {@link #sendToGateway()} after calling this method to have any effect.
     */
    void setDeafen(boolean deafen);

    /**
     * Send a {@link Op#MEDIA_SINK_WANTS} payload to the gateway.
     */
    void sendToGateway();

    /**
     * Handle a voice gateway message.
     */
    void handleEvent(JsonObject obj);

    void removeUser(String userId);
}
