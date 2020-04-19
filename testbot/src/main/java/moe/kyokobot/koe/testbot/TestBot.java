package moe.kyokobot.koe.testbot;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.channel.TextChannel;
import com.mewna.catnip.entity.channel.VoiceChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.util.Permission;
import com.mewna.catnip.shard.DiscordEvent;
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
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import io.netty.buffer.ByteBuf;
import io.reactivex.Observable;
import moe.kyokobot.koe.*;
import moe.kyokobot.koe.media.OpusAudioFrameProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.DISCORD_OPUS;

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

    public TestBot(String token) {
        this.token = token;
    }

    public void start() {
        this.catnip = createCatnip();
        this.koe = createKoe();
        this.playerManager = createAudioPlayerManager();

        catnip.observe(DiscordEvent.READY)
                .subscribe(ready -> koeClient = koe.newClient(catnip.selfUser().idAsLong()));

        catnip.observe(DiscordEvent.VOICE_STATE_UPDATE)
                .filter(state -> state.userIdAsLong() == catnip.selfUser().idAsLong() && state.channelIdAsLong() == 0)
                .subscribe(leave -> koeClient.destroyConnection(leave.guildIdAsLong()));

        catnip.observe(DiscordEvent.MESSAGE_CREATE)
                .filter(message -> message.guildIdAsLong() != 0
                        && !message.author().bot()
                        && message.content().equals("!ping"))
                .subscribe(message -> message.channel().sendMessage("Pong!"));

        catnip.observe(DiscordEvent.MESSAGE_CREATE)
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

                    var channel = Objects.requireNonNull(voiceState.channel());

                    if (koeClient.getConnection(voiceState.guildIdAsLong()) == null) {
                        var conn = koeClient.createConnection(voiceState.guildIdAsLong());
                        var player = playerMap.computeIfAbsent(message.guild(), n -> playerManager.createPlayer());
                        conn.setAudioSender(new AudioSender(player, conn));
                        conn.registerListener(new ExampleListener());
                        connect(channel);
                        message.channel().sendMessage("Joined channel `" + channel.name() + "`!");
                    } else {
                        catnip.openVoiceConnection(channel.guildIdAsLong(), channel.idAsLong());
                    }

                    resolve(message.guild(), message.channel().asTextChannel(), message.content().substring(6));
                });


        catnip.observe(DiscordEvent.MESSAGE_CREATE)
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
            catnip.shutdown(true);
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Koe createKoe() {
        return Koe.koe();
    }

    public Catnip createCatnip() {
        return Catnip.catnip(token);
    }

    public AudioPlayerManager createAudioPlayerManager() {
        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager());
        manager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        manager.registerSourceManager(new HttpAudioSourceManager());
        return manager;
    }

    private void connect(VoiceChannel channel) {
        Observable.combineLatest(
                catnip.observe(DiscordEvent.VOICE_STATE_UPDATE)
                        .filter(update -> update.userIdAsLong() == koeClient.getClientId()
                                && update.guildIdAsLong() == channel.guildIdAsLong()),
                catnip.observe(DiscordEvent.VOICE_SERVER_UPDATE)
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
                conn.connect(info).thenAccept(avoid -> logger.info("Koe connection succeeded!"));
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
                channel.sendMessage("**Now playing:** " + track.getInfo().title);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                var track = playlist.getTracks().get(0);
                player.playTrack(track);
                channel.sendMessage("**Now playing:** " + track.getInfo().title);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("**Error:** No matches found!");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("**Error:** " + exception.getMessage());
            }
        });
    }

    private static class AudioSender extends OpusAudioFrameProvider {
        private final AudioPlayer player;
        private final MutableAudioFrame frame;
        private final ByteBuffer frameBuffer;

        AudioSender(AudioPlayer player, VoiceConnection connection) {
            super(connection);
            this.player = player;
            this.frame = new MutableAudioFrame();
            this.frameBuffer = ByteBuffer.allocate(DISCORD_OPUS.maximumChunkSize());
            frame.setBuffer(frameBuffer);
            frame.setFormat(DISCORD_OPUS);
        }

        @Override
        public boolean canProvide() {
            return player.provide(frame);
        }

        @Override
        public void retrieveOpusFrame(ByteBuf targetBuffer) {
            targetBuffer.writeBytes(frameBuffer.array(), 0, frame.getDataLength());
        }
    }

    private static class ExampleListener extends KoeEventAdapter {
        @Override
        public void userConnected(String id, int audioSSRC, int videoSSRC) {
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
}
