package moe.kyokobot.koe.internal.gateway;

import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.internal.KoeClientImpl;
import moe.kyokobot.koe.internal.MediaConnectionImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayEndpointTest {
    private KoeOptions options;

    @AfterEach
    void tearDown() {
        options.getEventLoopGroup().shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }

    @ParameterizedTest
    @CsvSource({
            // endpoint, WSS port override, expected URI
            "c-fra11-1a2b3c4d.discord.media:443, false, wss://c-fra11-1a2b3c4d.discord.media:443/?v=8",
            "wss://c-fra11-1a2b3c4d.discord.media:443, false, wss://c-fra11-1a2b3c4d.discord.media:443/?v=8",
            "c-fra11-1a2b3c4d.discord.media, false, wss://c-fra11-1a2b3c4d.discord.media/?v=8",
            "c-fra11-1a2b3c4d.discord.media:80, false, wss://c-fra11-1a2b3c4d.discord.media:80/?v=8",
            "c-fra11-1a2b3c4d.discord.media:80, true, wss://c-fra11-1a2b3c4d.discord.media/?v=8",
            "wss://c-fra11-1a2b3c4d.discord.media:80, true, wss://c-fra11-1a2b3c4d.discord.media/?v=8",
            "c-fra11-1a2b3c4d.discord.media:8080, true, wss://c-fra11-1a2b3c4d.discord.media:8080/?v=8",
            "[2001:db8::1]:443, false, wss://[2001:db8::1]:443/?v=8",
    })
    void endpointIsTurnedIntoWebSocketUri(String endpoint, boolean portOverride, String expected) {
        assertEquals(expected, gateway(endpoint, portOverride, true).websocketURI.toString());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void hostnameVerificationCanBeDisabled(boolean verify) {
        assertEquals("wss://127.0.0.1:443/?v=8", gateway("127.0.0.1:443", false, verify).websocketURI.toString());
    }

    private MediaGatewayV8Connection gateway(String endpoint, boolean portOverride, boolean verifyHostname) {
        options = KoeOptions.builder()
                .setDAVEEnabled(false)
                .setEnableWSSPortOverride(portOverride)
                .setVerifyWSSHostname(verifyHostname)
                .create();
        var connection = new MediaConnectionImpl(new KoeClientImpl(1L, options), 2L);
        var info = VoiceServerInfo.builder()
                .setSessionId("session")
                .setToken("token")
                .setEndpoint(endpoint)
                .setChannelId(3L)
                .build();
        return new MediaGatewayV8Connection(connection, info);
    }
}
