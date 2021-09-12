package moe.kyokobot.koe.internal.dto.data;

import moe.kyokobot.koe.internal.dto.Data;
import org.jetbrains.annotations.Nullable;

public class Identify implements Data {
    public @Nullable Stream[] streams;
    public long serverId;
    public long userId;
    public String sessionId;
    public String token;
    public boolean video;

    public Identify(long serverId,
                    long userId,
                    String sessionId,
                    String token) {
        this.serverId = serverId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.token = token;
    }

    public Identify(@Nullable Stream[] streams,
                    long serverId,
                    long userId,
                    String sessionId,
                    String token,
                    boolean video) {
        this.streams = streams;
        this.serverId = serverId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.token = token;
        this.video = video;
    }

    public static class Stream {
        public Stream(String type, String rid, int quality) {
            this.type = type;
            this.rid = rid;
            this.quality = quality;
        }
        public String type;
        public String rid;
        public int quality;
    }
}
