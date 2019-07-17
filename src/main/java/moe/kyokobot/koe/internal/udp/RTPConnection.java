package moe.kyokobot.koe.internal.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.crypto.EncryptionMode;
import moe.kyokobot.koe.internal.NettyBootstrapFactory;

public class RTPConnection {
    private final VoiceConnection voiceConnection;
    private final EncryptionMode encryptionMode;
    private final Bootstrap bootstrap;

    public RTPConnection(VoiceConnection voiceConnection, EncryptionMode encryptionMode) {
        this.voiceConnection = voiceConnection;
        this.encryptionMode = encryptionMode;
        this.bootstrap = NettyBootstrapFactory.datagram(voiceConnection.getOptions());
    }
}
