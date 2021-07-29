package moe.kyokobot.koe.testbot;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.catnip.entity.channel.TextChannel;
import com.mewna.catnip.entity.channel.VoiceChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.util.Permission;
import com.mewna.catnip.extension.AbstractExtension;
import com.mewna.catnip.extension.hook.CatnipHook;
import com.mewna.catnip.rest.ratelimit.DefaultRateLimiter;
import com.mewna.catnip.shard.DiscordEvent;
import com.mewna.catnip.shard.ShardInfo;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeEventAdapter;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.H264Codec;
import moe.kyokobot.koe.testbot.util.GCPressureGenerator;
import moe.kyokobot.koe.testbot.util.UserBotBurstRequester;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * An example of bot that uses Koe to play music using LavaPlayer.
 * <p>
 * Supposed to be extended by extension examples, that's why create* methods are present,
 * they're meant to be overridden with code that creates an instance of specific thing with
 * different configuration.
 */
public class TestBot {
    private static final Logger logger = LoggerFactory.getLogger(TestBot.class);
    private final String token;

    private Catnip catnip;
    private Koe koe;
    private KoeClient koeClient;
    private AudioPlayerManager playerManager;
    private Map<Guild, AudioPlayer> playerMap = new ConcurrentHashMap<>();
    private boolean vidyaEnabled = false;

    public TestBot(String token) {
        this(token, false);
    }

    public TestBot(String token, boolean vidya) {
        this.token = token;
        this.vidyaEnabled = vidya;
    }

    public void start() {
        this.catnip = createCatnip();
        this.koe = createKoe();
        this.playerManager = createAudioPlayerManager();

        catnip.observable(DiscordEvent.READY)
                .subscribe(ready -> {
                    koeClient = koe.newClient(catnip.selfUser().idAsLong());
                });

        catnip.observable(DiscordEvent.VOICE_STATE_UPDATE)
                .filter(state -> state.userIdAsLong() == catnip.selfUser().idAsLong() && state.channelIdAsLong() == 0)
                .subscribe(leave -> koeClient.destroyConnection(leave.guildIdAsLong()));

        catnip.observable(DiscordEvent.MESSAGE_CREATE)
                .filter(message -> message.guildIdAsLong() != 0
                        && !message.author().bot()
                        && message.content().equals("!ping"))
                .subscribe(message -> message.channel().sendMessage("Pong!"));

        catnip.observable(DiscordEvent.MESSAGE_CREATE)
                .filter(message -> message.guildIdAsLong() != 0
                        && !message.author().bot()
                        && message.content().startsWith("!play "))
                .subscribe(message -> {
                    if (koeClient == null) return;

                    var voiceState = message.guild().voiceStates().getById(message.author().idAsLong());
                    if (voiceState == null) {
                        message.channel().sendMessage("You need to be in a voice channel!");
                        return;
                    }

                    if (!message.guild().selfMember().hasPermissions(voiceState.channel(), Permission.CONNECT)) {
                        message.channel().sendMessage("I don't have permissions to join your voice channel!");
                        return;
                    }

                    playSong(message.channel().asTextChannel(), voiceState.channel(), message.guild(), message.content().substring(6));
                });


        catnip.observable(DiscordEvent.MESSAGE_CREATE)
                .filter(message -> message.guildIdAsLong() != 0
                        && !message.author().bot()
                        && message.content().startsWith("!gcpress"))
                .subscribe(message -> message.channel()
                        .sendMessage("GC pressure generator enabled = " + GCPressureGenerator.toggle()));

        catnip.connect();
    }

    public void stop() {
        try {
            logger.info("Shutting down...");
            koeClient.getConnections().forEach((guild, conn) -> catnip.closeVoiceConnection(guild));
            koeClient.close();
            Thread.sleep(250);
            catnip.shutdown();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Koe createKoe() {
        return Koe.koe();
    }

    public Catnip createCatnip() {
        var apiHost = System.getenv("API_ENDPOINT");
        if (apiHost == null) {
            apiHost = "https://discord.com";
        }

        var options = new CatnipOptions(token)
                .apiHost(apiHost)
                .validateToken(false);

        return Catnip.catnip(options);
    }

    public AudioPlayerManager createAudioPlayerManager() {
        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager());
        manager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        manager.registerSourceManager(new HttpAudioSourceManager());
        return manager;
    }

    private void playSong(TextChannel textChannel, VoiceChannel channel, Guild guild, String query) {
        if (koeClient.getConnection(channel.guildIdAsLong()) == null) {
            var conn = koeClient.createConnection(guild.idAsLong());
            var player = playerMap.computeIfAbsent(guild, n -> playerManager.createPlayer());
            conn.setAudioSender(new Senders.AudioSender(player, conn));

            if (this.vidyaEnabled) {
                conn.setVideoCodec(H264Codec.INSTANCE);
                conn.setVideoSender(new Senders.VideoSender(conn));
            }

            conn.registerListener(new ExampleListener());
            connect(channel);

            if (textChannel != null)
                textChannel.sendMessage("Joined channel `" + channel.name() + "`!");
        }

        resolve(guild, textChannel, query);
    }

    private void connect(VoiceChannel channel) {
        Observable.combineLatest(
                catnip.observable(DiscordEvent.VOICE_STATE_UPDATE)
                        .filter(update -> update.userIdAsLong() == koeClient.getClientId()
                                && update.guildIdAsLong() == channel.guildIdAsLong()),
                catnip.observable(DiscordEvent.VOICE_SERVER_UPDATE)
                        .filter(update -> update.guildIdAsLong() == channel.guildIdAsLong())
                        .debounce(1, TimeUnit.SECONDS),
                Pair::of
        ).subscribe(pair -> {
            var stateUpdate = pair.getLeft();
            var serverUpdate = pair.getRight();
            var conn = koeClient.getConnection(serverUpdate.guildIdAsLong());
            if (conn != null) {
                var info = new VoiceServerInfo(
                        stateUpdate.sessionId(),
                        serverUpdate.endpoint(),
                        serverUpdate.token());
                conn.connect(info).thenAccept(avoid -> {
                    logger.info("Koe connection succeeded!");
                });
            }
        });

        catnip.openVoiceConnection(channel.guildIdAsLong(), channel.idAsLong());
    }

    private void resolve(Guild guild, TextChannel channel, String args) {
        var player = playerMap.computeIfAbsent(guild, n -> playerManager.createPlayer());

        playerManager.loadItem(args, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                if (channel != null) channel.sendMessage("**Now playing:** " + track.getInfo().title);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                var track = playlist.getTracks().get(0);
                player.playTrack(track);
                if (channel != null) channel.sendMessage("**Now playing:** " + track.getInfo().title);
            }

            @Override
            public void noMatches() {
                if (channel != null) channel.sendMessage("**Error:** No matches found!");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (channel != null) channel.sendMessage("**Error:** " + exception.getMessage());
            }
        });
    }


    private static class ExampleListener extends KoeEventAdapter {
        @Override
        public void userConnected(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
            logger.info("An user with id {} joined the channel!", id);
        }

        @Override
        public void userDisconnected(String id) {
            logger.info("An user with id {} left the channel!", id);
        }

        @Override
        public void gatewayClosed(int code, String reason, boolean byRemote) {
            logger.info("Voice gateway closed with code {}: {}", code, reason);
        }
    }

    private static class UserExtension extends AbstractExtension {
        private final CatnipHook hook = new CatnipHook() {
            @Override
            public JsonObject rawGatewayReceiveHook(@Nonnull ShardInfo shardInfo, @Nonnull JsonObject json) {
                if (json.isString("t") && json.getString("t").equals("READY")) {
                    var d = json.getObject("d");
                    if (!d.has("shard")) {
                        var arr = new JsonArray();
                        arr.add(0);
                        arr.add(1);
                        d.put("shard", arr);
                    }
                }

                return json;
            }
        };

        public UserExtension() {
            super("user-ext");
        }

        @Override
        public Completable onLoaded() {
            registerHook(hook);

            return null;
        }

        @Override
        public Completable onUnloaded() {
            unregisterHook(hook);
            return null;
        }
    }
}
