package moe.kyokobot.koe.internal;

import moe.kyokobot.koe.*;
import moe.kyokobot.koe.codec.Codec;
import moe.kyokobot.koe.codec.CodecType;
import moe.kyokobot.koe.codec.FramePoller;
import moe.kyokobot.koe.codec.OpusCodec;
import moe.kyokobot.koe.gateway.MediaGatewayConnection;
import moe.kyokobot.koe.handler.ConnectionHandler;
import moe.kyokobot.koe.media.MediaFrameProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class MediaConnectionImpl implements MediaConnection {
    private static final Logger logger = LoggerFactory.getLogger(MediaConnectionImpl.class);

    private final KoeClientImpl client;
    private final long guildId;
    private final EventDispatcher dispatcher;

    private MediaGatewayConnection gatewayConnection;
    private ConnectionHandler<?> connectionHandler;
    private VoiceServerInfo info;
    private Codec audioCodec;
    private Codec videoCodec;
    private FramePoller audioPoller;
    private FramePoller videoPoller;
    private MediaFrameProvider audioSender;
    private MediaFrameProvider videoSender;

    public MediaConnectionImpl(@NotNull KoeClientImpl client, long guildId) {
        this.client = Objects.requireNonNull(client);
        this.guildId = guildId;
        this.dispatcher = new EventDispatcher();
        this.audioCodec = OpusCodec.INSTANCE;
        this.audioPoller = client.getOptions().getFramePollerFactory().createFramePoller(this.audioCodec, this);
        this.videoCodec = null;
        this.videoPoller = null;
    }

    @Override
    public CompletionStage<Void> connect(VoiceServerInfo info) {
        this.disconnect();
        var conn = client.getGatewayVersion().createConnection(this, info);

        return conn.start().thenAccept(nothing -> {
            MediaConnectionImpl.this.info = info;
            MediaConnectionImpl.this.gatewayConnection = conn;
        });
    }

    @Override
    public void disconnect() {
        logger.debug("Disconnecting...");
        stopAudioFramePolling();
        stopVideoFramePolling();

        if (gatewayConnection != null && gatewayConnection.isOpen()) {
            gatewayConnection.close(1000, null);
            gatewayConnection = null;
        }

        if (connectionHandler != null) {
            connectionHandler.close();
            connectionHandler = null;
        }
    }

    @Override
    public void reconnect() {
        logger.debug("Reconnecting...");

        if (gatewayConnection != null) {
            gatewayConnection.reconnect();
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
    public MediaFrameProvider getAudioSender() {
        return audioSender;
    }

    @Override
    @Nullable
    public MediaFrameProvider getVideoSender() {
        return videoSender;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    @Nullable
    public MediaGatewayConnection getGatewayConnection() {
        return gatewayConnection;
    }

    @Override
    @Nullable
    public VoiceServerInfo getVoiceServerInfo() {
        return info;
    }

    @Override
    public ConnectionHandler<?> getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    public void setAudioSender(@Nullable MediaFrameProvider sender) {
        if (this.audioSender != null) {
            this.audioSender.dispose();
        }
        this.audioSender = sender;
    }

    @Override
    public void setAudioCodec(@NotNull Codec audioCodec) {
        if (Objects.requireNonNull(audioCodec).getType() != CodecType.AUDIO) {
            throw new IllegalArgumentException("Specified codec must be an audio codec!");
        }

        boolean wasPolling = this.audioPoller != null && this.audioPoller.isPolling();
        this.stopAudioFramePolling();

        this.audioCodec = audioCodec;
        this.audioPoller = client.getOptions().getFramePollerFactory().createFramePoller(audioCodec, this);

        if (wasPolling) {
            this.startAudioFramePolling();
        }
    }

    @Override
    public void startAudioFramePolling() {
        if (this.audioPoller == null || this.audioPoller.isPolling()) {
            return;
        }

        this.audioPoller.start();
    }

    @Override
    public void stopAudioFramePolling() {
        if (this.audioPoller == null || !this.audioPoller.isPolling()) {
            return;
        }

        this.audioPoller.stop();
    }

    @Override
    public void setVideoSender(@Nullable MediaFrameProvider sender) {
        if (this.videoSender != null) {
            this.videoSender.dispose();
        }
        this.videoSender = sender;
    }

    @Override
    public void setVideoCodec(@Nullable Codec videoCodec) {
        if (videoCodec == null) {
            this.stopVideoFramePolling();
            this.videoCodec = null;
            this.videoPoller = null;
            return;
        }

        if (videoCodec.getType() != CodecType.VIDEO) {
            throw new IllegalArgumentException("Specified codec must be an video codec!");
        }

        boolean wasPolling = videoPoller != null && videoPoller.isPolling();
        this.stopVideoFramePolling();

        this.videoCodec = videoCodec;
        this.videoPoller = client.getOptions().getFramePollerFactory().createFramePoller(videoCodec, this);

        if (wasPolling) {
            this.startVideoFramePolling();
        }
    }

    @Override
    public void startVideoFramePolling() {
        if (this.videoPoller == null || this.videoPoller.isPolling()) {
            return;
        }

        this.videoPoller.start();
    }

    @Override
    public void stopVideoFramePolling() {
        if (this.videoPoller == null || !this.videoPoller.isPolling()) {
            return;
        }

        this.videoPoller.stop();
    }

    @Override
    public void registerListener(KoeEventListener listener) {
        dispatcher.register(listener);
    }

    @Override
    public void unregisterListener(KoeEventListener listener) {
        dispatcher.unregister(listener);
    }

    @Override
    public void close() {
        if (this.audioSender != null) {
            this.audioSender.dispose();
            this.audioSender = null;
        }

        if (this.videoSender != null) {
            this.videoSender.dispose();
            this.videoSender = null;
        }

        disconnect();
        client.removeConnection(guildId);
    }

    @Override
    public void updateSpeakingState(int mask) {
        if (this.gatewayConnection != null) {
            this.gatewayConnection.updateSpeaking(mask);
        }
    }

    public EventDispatcher getDispatcher() {
        return dispatcher;
    }

    public void setConnectionHandler(ConnectionHandler<?> connectionHandler) {
        this.connectionHandler = connectionHandler;
    }
}
