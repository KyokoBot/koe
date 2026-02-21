package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.gateway.MediaValve;
import moe.kyokobot.koe.gateway.Op;
import moe.kyokobot.koe.internal.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the media streams of each user connected to the Voice channel and disables their incoming packets.
 */
public class MediaValveImpl implements MediaValve {
    private static final Logger LOG = LoggerFactory.getLogger(MediaValve.class.getName());

    private final AbstractMediaGatewayConnection gatewayConnection;

    /**
     * A map of `user_id->streams[*].ssrc`
     */
    private final Map<String, int[]> unwantedStreams = new HashMap<>();

    private boolean deafen = false;

    public MediaValveImpl(AbstractMediaGatewayConnection gatewayConnection) {
        this.gatewayConnection = gatewayConnection;
    }

    @Override public boolean isDeafened() {
        return deafen;
    }

    @Override public void setDeafen(boolean deafen) {
        this.deafen = deafen;
    }

    @Override public synchronized void sendToGateway() {
        JsonObject d = new JsonObject();

        // disable all incoming audio streams.
        d.add("any", deafen ? 0 : 100);

        // add any unwanted streams.
        for (int[] streams : unwantedStreams.values()) {
            for (int ssrc : streams) d.add(Integer.toString(ssrc), 0);
        }

        this.gatewayConnection.sendInternalPayload(Op.MEDIA_SINK_WANTS, d);
    }

    @Override public synchronized void handleEvent(JsonObject obj) {
        int op = obj.getInt("op");
        if (op == Op.VIDEO) {
            JsonObject d = obj.getObject("d");
            String userId = d.getString("user_id");

            // if `video_ssrc` is 0, it indicates that the user is not showing their camera.
            if (d.getInt("video_ssrc") == 0) {
                this.removeUser(userId);
                return;
            }

            // we can skip `audio_ssrc` since "any":0 covers audio.
            // instead, all ssrcs listed in "streams".
            int[] ssrcs = d.getArray("streams").stream()
                    .filter(s -> s instanceof JsonObject)
                    .mapToInt((stream) -> ((JsonObject) stream).getInt("ssrc"))
                    .toArray();

            LOG.debug("Received streams for user {}: {}", d, Arrays.toString(ssrcs));

            this.unwantedStreams.put(userId, ssrcs);
            this.sendToGateway();
        } else if (op == Op.CLIENT_DISCONNECT) {
            this.removeUser(obj.getObject("d").getString("user_id"));
        }
    }

    @Override public void removeUser(String userId) {
        LOG.debug("Removing streams for user {}", userId);
        this.unwantedStreams.remove(userId);
        this.sendToGateway();
    }
}
