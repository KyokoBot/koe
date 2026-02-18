package moe.kyokobot.koe.experimental;

import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.MediaConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface KoeClientExperimental extends KoeClient {
    @NotNull
    MediaConnectionExperimental createConnection(long guildId);

    @Nullable
    MediaConnectionExperimental getConnection(long guildId);

    static MediaConnectionExperimental asExperimental(MediaConnection connection) {
        if (connection instanceof MediaConnectionExperimental) {
            return (MediaConnectionExperimental) connection;
        } else {
            throw new IllegalArgumentException("MediaConnection is not an instance of MediaConnectionExperimental");
        }
    }
}
