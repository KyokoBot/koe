package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents connection information for a Discord voice server.
 * This class stores combination of data from VOICE_STATE_UPDATE and VOICE_SERVER_UPDATE required to establish a voice connection.
 *
 * @see <a href="https://discord.com/developers/docs/events/gateway-events#voice-server-update">Discord documentation on gateway events</a>
 * @see <a href="https://discord.com/developers/docs/topics/voice-connections#connecting-to-voice">Discord documentation on connecting to voice</a>
 */
public class VoiceServerInfo {
    private final String sessionId;
    private final String endpoint;
    private final String token;
    private final long channelId;

    /**
     * @param sessionId Session ID from VOICE_STATE_UPDATE payload.
     * @param endpoint  Voice server endpoint from VOICE_SERVER_UPDATE event (passed as-is in form of "hostname" or "hostname:port").
     * @param token     The authentication token from VOICE_SERVER_UPDATE payload.
     * @see #builder() Recommended way to create an instance of this class.
     */
    private VoiceServerInfo(@NotNull String sessionId,
                            @NotNull String endpoint,
                            @NotNull String token,
                            long channelId) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.endpoint = Objects.requireNonNull(endpoint);
        this.token = Objects.requireNonNull(token);
        this.channelId = channelId;
    }

    @NotNull
    public String getSessionId() {
        return sessionId;
    }

    @NotNull
    public String getEndpoint() {
        return endpoint;
    }

    @NotNull
    public String getToken() {
        return token;
    }

    public long getChannelId() {
        return channelId;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionId;
        private String endpoint;
        private String token;
        private long channelId;

        /**
         * @param sessionId Session ID from VOICE_STATE_UPDATE payload.
         */
        public Builder setSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @param endpoint Voice server endpoint from VOICE_SERVER_UPDATE event (passed as-is in form
         *                 of "hostname" or "hostname:port").
         */
        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @param token The authentication token from VOICE_SERVER_UPDATE payload.
         */
        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        /**
         * @param channelId An ID of the voice channel. Required for establishing DAVE MLS groups.
         */
        public Builder setChannelId(long channelId) {
            this.channelId = channelId;
            return this;
        }

        public VoiceServerInfo build() {
            return new VoiceServerInfo(sessionId, endpoint, token, channelId);
        }
    }
}
