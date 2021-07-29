package moe.kyokobot.koe.testbot;

import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.gateway.GatewayVersion;

public class VideoTestBotLauncher {
    public static void main(String[] args) {
        var bot = new TestBot(System.getenv("TOKEN"), true) {
            @Override
            public Koe createKoe() {
                return Koe.koe(KoeOptions.builder()
                        .setGatewayVersion(GatewayVersion.V5)
                        .create());
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(bot::stop));
        bot.start();
    }
}
