package moe.kyokobot.koe.internal;

import moe.kyokobot.koe.KoeClient;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.audio.AudioFrameProvider;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.codec.FramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.gateway.VoiceGatewayConnection;
import moe.kyokobot.koe.handler.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class VoiceConnectionImpl implements VoiceConnection {
    private static final Logger logger = LoggerFactory.getLogger(VoiceConnection.class);

    private final KoeClientImpl client;
    private final long guildId;

    private VoiceGatewayConnection gatewayConnection;
    private ConnectionHandler connectionHandler;
    private VoiceServerInfo info;
    private Codec audioCodec;
    private FramePoller poller;
    private AudioFrameProvider sender;

    public VoiceConnectionImpl(@NotNull KoeClientImpl client, long guildId) {
        this.client = Objects.requireNonNull(client);
        this.guildId = guildId;
        this.audioCodec = OpusCodec.INSTANCE;
        this.poller = audioCodec.createFramePoller(this);
    }

    @Override
    public CompletionStage<Void> connect(VoiceServerInfo info) {
        disconnect();
        var conn = client.getGatewayVersion().createConnection(this, info);

        return conn.start().thenAccept(nothing -> {
            VoiceConnectionImpl.this.info = info;
            VoiceConnectionImpl.this.gatewayConnection = conn;
        });
    }

    @Override
    public void disconnect() {
        logger.debug("Disconnecting...");
        stopFramePolling();

        if (gatewayConnection != null && gatewayConnection.isOpen()) {
            gatewayConnection.close();
            gatewayConnection = null;
        }

        if (connectionHandler != null) {
            connectionHandler.close();
            connectionHandler = null;
        }
    }

    @Override
    @NotNull
    public KoeClient getClient() {
        return client;
    }

    @Override
    @NotNull
    public KoeOptions getOptions() {
        return client.getOptions();
    }

    @Override
    @Nullable
    public AudioFrameProvider getSender() {
        return sender;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    @Nullable
    public VoiceGatewayConnection getGatewayConnection() {
        return gatewayConnection;
    }

    @Override
    @Nullable
    public VoiceServerInfo getVoiceServerInfo() {
        return info;
    }

    @Override
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    public void setAudioSender(@Nullable AudioFrameProvider sender) {
        this.sender = sender;
    }

    @Override
    public void setAudioCodec(@NotNull Codec audioCodec) {
        if (Objects.requireNonNull(audioCodec).getType() != CodecType.AUDIO) {
            throw new IllegalArgumentException("Specified codec must be an audio codec!");
        }

        boolean wasPolling = poller != null && poller.isPolling();
        stopFramePolling();

        this.audioCodec = audioCodec;
        this.poller = audioCodec.createFramePoller(this);

        if (wasPolling) {
            startFramePolling();
        }
    }

    public void setConnectionHandler(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void startFramePolling() {
        if (poller == null || poller.isPolling()) {
            return;
        }

        poller.start();
    }

    @Override
    public void stopFramePolling() {
        if (poller == null || !poller.isPolling()) {
            return;
        }

        poller.stop();
    }

    @Override
    public void close() {
        disconnect();
        client.removeConnection(guildId);
    }
}
