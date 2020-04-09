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
import moe.kyokobot.koe.audio.AudioFrameProvider;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.OpusCodec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.DISCORD_OPUS;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static Catnip catnip;
    private static Koe koe;
    private static KoeClient koeClient;
    private static AudioPlayerManager playerManager;
    private static Map<Guild, AudioPlayer> playerMap = new ConcurrentHashMap<>();

    public static void main(String... args) {
        var token = System.getenv("TOKEN");
        catnip = Catnip.catnip(token);
        koe = Koe.koe(KoeOptions.defaultOptions());
        playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new HttpAudioSourceManager());

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
                        conn.setAudioSender(new AudioSender(player));
                        conn.registerListener(new ExampleListener());
                        connect(channel);
                        message.channel().sendMessage("Joined channel `" + channel.name() + "`!");
                    } else {
                        catnip.openVoiceConnection(channel.guildIdAsLong(), channel.idAsLong());
                    }

                    resolve(message.guild(), message.channel().asTextChannel(), message.content().substring(6));
                });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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
        }));

        catnip.connect();
    }

    private static void connect(VoiceChannel channel) {
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

    private static void resolve(Guild guild, TextChannel channel, String args) {
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

    private static class AudioSender implements AudioFrameProvider {
        private final AudioPlayer player;
        private final MutableAudioFrame frame;
        private final ByteBuffer frameBuffer;

        AudioSender(AudioPlayer player) {
            this.player = player;
            this.frame = new MutableAudioFrame();
            this.frameBuffer = ByteBuffer.allocateDirect(DISCORD_OPUS.maximumChunkSize());
            frame.setBuffer(frameBuffer);
            frame.setFormat(DISCORD_OPUS);
        }

        @Override
        public boolean canSendFrame() {
            return player.provide(frame);
        }

        @Override
        public void retrieve(Codec codec, ByteBuf buf) {
            if (codec.getPayloadType() == OpusCodec.PAYLOAD_TYPE) {
                buf.writeBytes(frame.getData());
            }
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
        public void gatewayClosed(int code, String reason) {
            logger.info("Voice gateway closed with code {}: {}", code, reason);
        }
    }
}
