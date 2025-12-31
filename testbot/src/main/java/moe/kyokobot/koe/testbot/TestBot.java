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
import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.*;
import moe.kyokobot.koe.media.OpusAudioFrameProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.GatewayIntent;
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
    private Koe koe;
    private KoeClient koeClient;
    private AudioPlayerManager playerManager;
    private Map<Guild, AudioPlayer> playerMap = new ConcurrentHashMap<>();
    private Map<Long, Long> vsuChannelMap = new ConcurrentHashMap<>();

    public TestBot(String token) {
        this.leakDetect = new NettyLeakDetect();
        this.token = token;
    }

    public void start() {
        this.jda = createJDA();
        this.koe = createKoe();
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

    public Koe createKoe() {
        return Koe.koe(KoeOptions.builder()
                .setByteBufAllocator(this.leakDetect.getAllocator())
                .create());
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
        manager.registerSourceManager(new YoutubeAudioSourceManager());
        manager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        manager.registerSourceManager(new HttpAudioSourceManager());
        return manager;
    }

    @Override
    public void onReady(ReadyEvent event) {
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
            });
        }
    }

    @Override
    public boolean onVoiceStateUpdate(VoiceStateUpdate voiceStateUpdate) {
        if (voiceStateUpdate.getVoiceState().getIdLong() == jda.getSelfUser().getIdLong()) {
            logger.info("VSU {} {}", voiceStateUpdate.getGuild(), voiceStateUpdate.getChannel());

            if (voiceStateUpdate.getChannel() == null) {
                koeClient.destroyConnection(voiceStateUpdate.getGuildIdLong());
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
                conn.setAudioSender(new AudioSender(player, conn));
                conn.registerListener(new ExampleListener());
                connect(channel);
                event.getChannel().sendMessage("Joined channel `" + channel.getName() + "`!").queue();
            }

            if (isPlay) {
                resolve(event.getGuild(), event.getChannel().asGuildMessageChannel(), content.substring(6));
            }
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

    private static class AudioSender extends OpusAudioFrameProvider {
        private final AudioPlayer player;
        private final MutableAudioFrame frame;
        private final ByteBuffer frameBuffer;

        AudioSender(AudioPlayer player, MediaConnection connection) {
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
}
