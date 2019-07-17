package moe.kyokobot.koe.testbot;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.channel.VoiceChannel;
import com.mewna.catnip.entity.util.Permission;
import com.mewna.catnip.shard.DiscordEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import io.netty.buffer.ByteBuf;
import io.reactivex.Observable;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.audio.AudioSender;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.DISCORD_OPUS;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static Catnip catnip;
    private static Koe koe;
    private static KoeClient koeClient;
    private static AudioPlayerManager playerManager;

    public static void main(String... args) {
        var token = System.getenv("TOKEN");
        catnip = Catnip.catnip(token);
        koe = new Koe(KoeOptions.defaultOptions());
        playerManager = new DefaultAudioPlayerManager();
        var listenMoeTrack = (AudioTrack) new HttpAudioSourceManager()
                .loadItem(null, new AudioReference("https://listen.moe/opus", "Listen.moe"));

        catnip.observe(DiscordEvent.READY)
                .subscribe(ready -> koeClient = koe.newClient(catnip.selfUser().idAsLong()));

        catnip.observe(DiscordEvent.VOICE_STATE_UPDATE)
                .filter(state -> state.userIdAsLong() == catnip.selfUser().idAsLong() && state.channelIdAsLong() == 0)
                .subscribe(leave -> {
                    var conn = koeClient.getConnection(leave.guildIdAsLong());
                    if (conn != null) {
                        conn.disconnect();
                    }
                });

        catnip.observe(DiscordEvent.MESSAGE_CREATE)
                .filter(message -> message.guildIdAsLong() != 0
                        && !message.author().bot()
                        && message.content().equals("!join"))
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

                    var conn = koeClient.createConnection(voiceState.guildIdAsLong());
                    message.channel().sendMessage("Joined channel `" + voiceState.channel().name() + "`!");
                    connect(voiceState.channel());

                    var player = playerManager.createPlayer();
                    player.playTrack(listenMoeTrack.makeClone());
                    conn.setSender(new ListenMoeSender(player));
                });

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

    private static class ListenMoeSender implements AudioSender {
        private final AudioPlayer player;
        private final MutableAudioFrame frame;
        private final ByteBuffer frameBuffer;

        ListenMoeSender(AudioPlayer player) {
            this.player = player;
            this.frame = new MutableAudioFrame();
            this.frameBuffer = ByteBuffer.allocate(DISCORD_OPUS.maximumChunkSize());
            frame.setBuffer(frameBuffer);
            frame.setFormat(DISCORD_OPUS);
        }


        @Override
        public boolean canSendFrame() {
            return player.provide(frame);
        }

        @Override
        public void retrieve(ByteBuf buf) {
            buf.writeBytes(frameBuffer.array());
        }
    }
}
