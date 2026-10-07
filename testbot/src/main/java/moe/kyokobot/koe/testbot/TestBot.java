package moe.kyokobot.koe.testbot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeEventListener;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.codec.OpusCodecInfo;
import moe.kyokobot.koe.experimental.KoeExperimental;
import moe.kyokobot.koe.experimental.KoeOptionsBuilderExperimental;
import moe.kyokobot.koe.experimental.KoeOptionsExperimental;
import moe.kyokobot.koe.media.AudioFrameProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.DISCORD_OPUS;

/**
 * An example of bot that uses Koe to play music using LavaPlayer.
 * <p>
 * Supposed to be extended by extension examples, that's why create* methods are present,
 * they're meant to be overridden with code that creates an instance of specific thing with
 * different configuration.
 */
public class TestBot extends ListenerAdapter implements VoiceDispatchInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TestBot.class);
    private final String token;

    private final NettyLeakDetect leakDetect;
    private JDA jda;
    private KoeExperimental koe;
    private KoeClient koeClient;
    private AudioPlayerManager playerManager;
    private final Map<Guild, AudioPlayer> playerMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> vsuChannelMap = new ConcurrentHashMap<>();

    public TestBot(String token) {
        this.leakDetect = new NettyLeakDetect();
        this.token = token;
    }

    public void start() {
        this.jda = createJDA();
        var options = configureKoe((KoeOptionsBuilderExperimental) KoeOptionsExperimental.builder()
                .setByteBufAllocator(this.leakDetect.getAllocator())
                .setEnableDAVELogSink(true)
        );
        this.koe = KoeExperimental.koe(options);
        this.playerManager = createAudioPlayerManager();
    }

    public void stop() {
        try {
            logger.info("Shutting down...");
            koeClient.close();
            this.leakDetect.printAllocStats();
            Thread.sleep(250);
            jda.shutdownNow();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public KoeOptionsExperimental configureKoe(KoeOptionsBuilderExperimental builder) {
        return builder.create();
    }

    public JDA createJDA() {
        return JDABuilder
                .createDefault(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES))
                .addEventListeners(this)
                .setVoiceDispatchInterceptor(this)
                .build();
    }

    public AudioPlayerManager createAudioPlayerManager() {
        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager(new Web(), new WebEmbedded(), new Music(), new AndroidVr(), new Ios()));
        manager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        manager.registerSourceManager(new HttpAudioSourceManager());
        return manager;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        koeClient = koe.newClient(jda.getSelfUser().getIdLong());
    }

    @Override
    public void onVoiceServerUpdate(VoiceServerUpdate voiceServerUpdate) {
        var conn = koeClient.getConnection(voiceServerUpdate.getGuildIdLong());
        if (conn != null) {
            var info = VoiceServerInfo.builder()
                    .setSessionId(voiceServerUpdate.getSessionId())
                    .setEndpoint(voiceServerUpdate.getEndpoint())
                    .setToken(voiceServerUpdate.getToken())
                    .setChannelId(vsuChannelMap.getOrDefault(voiceServerUpdate.getGuildIdLong(), 0L))
                    .build();
            conn.connect(info).thenAccept(avoid -> {
                logger.info("Koe connection succeeded!");
                this.leakDetect.printAllocStats();
                // requestToSpeakIfSuppressed(voiceServerUpdate.getGuild());
            });
        }
    }

    private void requestToSpeakIfSuppressed(Guild guild) {
        // Bots join stages as suppressed audience members, so nobody hears them until they become a speaker.
        // Becoming a speaker while the voice connection is still being set up doesn't always take effect,
        // so the request is only sent once Koe is connected.
        var voiceState = guild.getSelfMember().getVoiceState();
        if (voiceState == null || !voiceState.isSuppressed() || !(voiceState.getChannel() instanceof StageChannel)) {
            return;
        }

        var stage = (StageChannel) voiceState.getChannel();
        stage.requestToSpeak().queue(
                v -> logger.info("Requested to speak in {}", stage),
                e -> logger.warn("Failed to request to speak in {}", stage, e));
    }

    @Override
    public boolean onVoiceStateUpdate(VoiceStateUpdate voiceStateUpdate) {
        if (voiceStateUpdate.getVoiceState().getIdLong() == jda.getSelfUser().getIdLong()) {
            logger.info("VSU {} {} suppressed={}", voiceStateUpdate.getGuild(), voiceStateUpdate.getChannel(),
                    voiceStateUpdate.getVoiceState().isSuppressed());

            if (voiceStateUpdate.getChannel() == null) {
                koeClient.destroyConnection(voiceStateUpdate.getGuildIdLong());
                logger.info("Destroyed connection for guild {}", voiceStateUpdate.getGuildIdLong());
                vsuChannelMap.remove(voiceStateUpdate.getGuildIdLong());
                return true;
            } else {
                vsuChannelMap.put(voiceStateUpdate.getGuildIdLong(), voiceStateUpdate.getChannel().getIdLong());
            }
        }
        return true;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        var content = event.getMessage().getContentRaw();

        if (content.equals("!help")) {
            event.getChannel().sendMessage("**Commands:**\n"
                    + "`!help` - shows this message\n"
                    + "`!ping` - replies with Pong!\n"
                    + "`!join` - joins your voice or stage channel\n"
                    + "`!play <url or search>` - joins your channel and plays a track\n"
                    + "`!stop` - stops the current track\n"
                    + "`!disconnect` - disconnects from the voice channel\n"
                    + "`!gcpress` - toggles the GC pressure generator").queue();
            return;
        }

        if (content.equals("!ping")) {
            event.getChannel().sendMessage("Pong!").queue();
            return;
        }

        var isPlay = content.startsWith("!play ");
        if (isPlay || content.startsWith("!join")) {
            if (event.getMember() == null) return;
            var voiceState = event.getMember().getVoiceState();
            if (voiceState == null || voiceState.getChannel() == null) {
                event.getChannel().sendMessage("You need to be in a voice channel!").queue();
                return;
            }

            var channel = voiceState.getChannel();

            if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) {
                event.getChannel().sendMessage("I don't have permissions to join your voice channel!").queue();
                return;
            }

            if (koeClient.getConnection(voiceState.getGuild().getIdLong()) == null) {
                var conn = koeClient.createConnection(voiceState.getGuild().getIdLong());
                var player = playerMap.computeIfAbsent(event.getGuild(), n -> playerManager.createPlayer());
                conn.setAudioSender(new OpusProvider(player));
                conn.registerListener(new ExampleListener());

                if (channel instanceof StageChannel) {
                    requestToSpeakIfSuppressed(event.getGuild());
                }

                connect(channel);
                event.getChannel().sendMessage("Joined channel `" + channel.getName() + "`!").queue();
            }

            if (isPlay) {
                resolve(event.getGuild(), event.getChannel().asGuildMessageChannel(), content.substring(6));
            }
            return;
        }

        if (content.equals("!stop")) {
            var player = playerMap.get(event.getGuild());
            if (player != null) {
                player.stopTrack();
                event.getChannel().sendMessage("Stopped the current track!").queue();
            } else {
                event.getChannel().sendMessage("No track is currently playing!").queue();
            }
            return;
        }

        if (content.startsWith("!disconnect")) {
            jda.getDirectAudioController().disconnect(event.getGuild());
            event.getChannel().sendMessage("Disconnected from voice channel!").queue();
            return;
        }

        if (content.startsWith("!gcpress")) {
            event.getChannel().sendMessage("GC pressure generator enabled = " + GCPressureGenerator.toggle()).queue();
        }
    }

    private void connect(AudioChannel channel) {
        jda.getDirectAudioController().connect(channel);
    }

    private void resolve(Guild guild, GuildMessageChannel channel, String args) {
        var player = playerMap.computeIfAbsent(guild, n -> playerManager.createPlayer());

        playerManager.loadItem(args, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                channel.sendMessage("**Now playing:** " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                var track = playlist.getTracks().get(0);
                player.playTrack(track);
                channel.sendMessage("**Now playing:** " + track.getInfo().title).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage("**Error:** No matches found!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("**Error:** " + exception.getMessage()).queue();
            }
        });
    }

    private static class OpusProvider implements AudioFrameProvider {
        private final AudioPlayer player;
        private final MutableAudioFrame frame;
        private final ByteBuffer frameBuffer;
        private boolean isOpus;

        OpusProvider(AudioPlayer player) {
            this.player = player;
            this.frame = new MutableAudioFrame();
            this.frameBuffer = ByteBuffer.allocate(DISCORD_OPUS.maximumChunkSize());
            frame.setBuffer(frameBuffer);
            frame.setFormat(DISCORD_OPUS);
        }

        @Override
        public void onCodecChanged(@NotNull CodecInstance codec) {
            this.isOpus = OpusCodecInfo.isInstanceOf(codec);
        }

        @Override
        public boolean canProvide() {
            if (!isOpus) return false;
            return player.provide(frame);
        }

        @Override
        public boolean provideFrame(ByteBuf targetBuffer) {
            targetBuffer.writeBytes(frameBuffer.array(), 0, frame.getDataLength());
            return true;
        }

        @Override
        public void dispose() {

        }
    }

    private static class ExampleListener implements KoeEventListener {
        @Override
        public void userStreamsChanged(String id, int audioSSRC, int videoSSRC, int rtxSSRC) {
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
