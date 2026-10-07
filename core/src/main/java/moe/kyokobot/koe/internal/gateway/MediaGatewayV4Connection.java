package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.MediaConnectionImpl;

/**
 * Speaking state as a bitmask. Koe announces only audio codecs on this version, the protocol itself is implemented in
 * {@link AbstractMediaGatewayConnection}.
 */
public class MediaGatewayV4Connection extends AbstractMediaGatewayConnection {
    public MediaGatewayV4Connection(MediaConnectionImpl connection, VoiceServerInfo voiceServerInfo) {
        super(connection, voiceServerInfo, 4);
    }
}
