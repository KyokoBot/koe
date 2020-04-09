package moe.kyokobot.koe.testbot;

import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory;

public class UdpQueueTestBotLauncher {
    public static void main(String[] args) {
        var bot = new TestBot(System.getenv("TOKEN")) {
            @Override
            public Koe createKoe() {
                return Koe.koe(KoeOptions.builder()
                        .setFramePollerFactory(new UdpQueueFramePollerFactory())
                        .create());
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(bot::stop));
        bot.start();
    }
}
