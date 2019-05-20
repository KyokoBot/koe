package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.VoiceServerInfo;

public class VoiceGatewayV4Connection extends AbstractVoiceGatewayConnection {
    public VoiceGatewayV4Connection(VoiceConnection connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 4);
    }
}
