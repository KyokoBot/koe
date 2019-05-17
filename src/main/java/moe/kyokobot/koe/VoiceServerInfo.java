package moe.kyokobot.koe;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VoiceServerInfo {
    private final String sessionId;
    private final String endpoint;
    private final String token;

    public VoiceServerInfo(@NotNull String sessionId,
                           @NotNull String endpoint,
                           @NotNull String token) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.endpoint = Objects.requireNonNull(endpoint);
        this.token = Objects.requireNonNull(token);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getToken() {
        return token;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionId;
        private String endpoint;
        private String token;

        public Builder setSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public VoiceServerInfo build() {
            return new VoiceServerInfo(sessionId, endpoint.replace(":80", ""), token);
        }
    }
}
