package moe.kyokobot.koe.testbot;

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
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeEventAdapter;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.H264Codec;
import moe.kyokobot.koe.testbot.util.GCPressureGenerator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private JDA jda;
    private Koe koe;
    private KoeClient koeClient;
    private AudioPlayerManager playerManager;
    private Map<Guild, AudioPlayer> playerMap = new ConcurrentHashMap<>();
    private boolean vidyaEnabled;

    public TestBot(String token) {
        this(token, false);
    }

    public TestBot(String token, boolean vidya) {
        this.token = token;
        this.vidyaEnabled = vidya;
    }

    public void start() {
        this.jda = createJDA();
        this.koe = createKoe();
        this.playerManager = createAudioPlayerManager();

        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(@NotNull ReadyEvent event) {
                koeClient = koe.newClient(jda.getSelfUser().getIdLong());
            }

            @Override
            public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
                if (event.getMember().getIdLong() == jda.getSelfUser().getIdLong()) {
                    koeClient.destroyConnection(event.getGuild().getIdLong());
                }
            }

            @Override
            public void onRawGateway(@NotNull RawGatewayEvent event) {
                if (event.getType().equals("VOICE_SERVER_UPDATE")) {
                    logger.debug("got VSU {}", event.getPayload());

                    var guildId = event.getPayload().getString("guild_id");
                    var endpoint = event.getPayload().getString("endpoint");
                    var token = event.getPayload().getString("token");

                    var guild = jda.getGuildById(guildId);
                    if (guild == null) return;

                    var voiceState = guild.getVoiceStates().stream()
                            .filter(s -> s.getMember().getIdLong() == jda.getSelfUser().getIdLong()).findFirst();

                    if (voiceState.isEmpty()) return;

                    var conn = koeClient.getConnection(guild.getIdLong());
                    if (conn != null) {
                        var info = new VoiceServerInfo(
                                voiceState.get().getSessionId(),
                                endpoint,
                                token);
                        conn.connect(info).thenAccept(avoid -> {
                            logger.info("Koe connection succeeded!");
                        });
                    }
                }
            }

            @Override
            public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
                if (event.getAuthor().isBot()) return;

                var message = event.getMessage();

                if (message.getContentRaw().equals("!ping")) {
                    message.reply("Pong!").queue();
                } else if (message.getContentRaw().equals("!gcpress")) {
                    message.reply("GC pressure generator enabled = " + GCPressureGenerator.toggle());
                } else if (message.getContentRaw().startsWith("!play ")) {
                    if (koeClient == null) return;

                    var voiceState = message.getGuild().getVoiceStates().stream()
                            .filter(s -> s.getMember().getIdLong() == message.getAuthor().getIdLong()).findFirst();
                    if (voiceState.isEmpty()) {
                        message.reply("You need to be in a voice channel!").queue();
                        return;
                    }

                    if (!message.getGuild().getSelfMember().hasPermission(voiceState.get().getChannel(), Permission.VOICE_CONNECT)) {
                        message.reply("I don't have permissions to join your voice channel!").queue();
                        return;
                    }

                    playSong(message.getTextChannel(), voiceState.get().getChannel(), message.getGuild(), message.getContentRaw().substring(6));
                }
            }
        });
    }

    public void stop() {
        try {
            logger.info("Shutting down...");

            var ac = jda.getDirectAudioController();

            koeClient.getConnections().forEach((guild, conn) -> {
                var g = jda.getGuildById(guild);
                if (g != null) ac.disconnect(g);
            });
            koeClient.close();

            Thread.sleep(250);
            jda.shutdown();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Koe createKoe() {
        return Koe.koe();
    }

    public JDA createJDA() {
        try {
            return JDABuilder.createDefault(token)
                    .setVoiceDispatchInterceptor(new VoiceDispatchInterceptor() {
                        @Override
                        public void onVoiceServerUpdate(@NotNull VoiceDispatchInterceptor.VoiceServerUpdate update) {
                            var voiceState = update.getGuild().getVoiceStates().stream()
                                    .filter(s -> s.getMember().getIdLong() == jda.getSelfUser().getIdLong()).findFirst();

                            if (voiceState.isEmpty()) return;

                            var conn = koeClient.getConnection(update.getGuildIdLong());
                            if (conn != null) {
                                var info = new VoiceServerInfo(
                                        voiceState.get().getSessionId(),
                                        update.getEndpoint(),
                                        update.getToken());
                                conn.connect(info).thenAccept(avoid -> {
                                    logger.info("Koe connection succeeded!");
                                });
                            }
                        }

                        @Override
                        public boolean onVoiceStateUpdate(@NotNull VoiceDispatchInterceptor.VoiceStateUpdate update) {
                            return true;
                        }
                    }).build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    public AudioPlayerManager createAudioPlayerManager() {
        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager());
        manager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        manager.registerSourceManager(new HttpAudioSourceManager());
        return manager;
    }

    private void playSong(TextChannel textChannel, VoiceChannel channel, Guild guild, String query) {
        if (koeClient.getConnection(channel.getIdLong()) == null) {
            var conn = koeClient.createConnection(guild.getIdLong());
            var player = playerMap.computeIfAbsent(guild, n -> playerManager.createPlayer());
            conn.setAudioSender(new Senders.AudioSender(player, conn));

            if (this.vidyaEnabled) {
                conn.setVideoCodec(H264Codec.INSTANCE);
                conn.setVideoSender(new Senders.VideoSender(conn));
            }

            conn.registerListener(new ExampleListener());
            connect(channel);

            if (textChannel != null)
                textChannel.sendMessage("Joined channel `" + channel.getName() + "`!").queue();
        }

        resolve(guild, textChannel, query);
    }

    private void connect(VoiceChannel channel) {
        jda.getDirectAudioController().connect(channel);
    }

    private void resolve(Guild guild, TextChannel channel, String args) {
        var player = playerMap.computeIfAbsent(guild, n -> playerManager.createPlayer());

        playerManager.loadItem(args, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                if (channel != null) channel.sendMessage("**Now playing:** " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                var track = playlist.getTracks().get(0);
                player.playTrack(track);
                if (channel != null) channel.sendMessage("**Now playing:** " + track.getInfo().title).queue();
            }

            @Override
            public void noMatches() {
                if (channel != null) channel.sendMessage("**Error:** No matches found!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (channel != null) channel.sendMessage("**Error:** " + exception.getMessage()).queue();
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
}
