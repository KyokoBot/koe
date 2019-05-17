package moe.kyokobot.koe;

public class KoeConnection {
    private final long guildId;

    public KoeConnection(long guildId) {
        this.guildId = guildId;
    }

    public long getGuildId() {
        return guildId;
    }
}
