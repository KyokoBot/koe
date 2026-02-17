package moe.kyokobot.koe.testbot;

import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.KoeOptionsBuilder;
import moe.kyokobot.koe.poller.udpqueue.QueueManagerPool;
import moe.kyokobot.koe.poller.udpqueue.UdpQueueFramePollerFactory;

public class UdpQueueTestBotLauncher {
    public static void main(String[] args) {
        var queuePool = new QueueManagerPool();

        var bot = new TestBot(System.getenv("TOKEN")) {
            @Override
            public KoeOptions configureKoe(KoeOptionsBuilder options) {
                return options
                        .setFramePollerFactory(new UdpQueueFramePollerFactory(queuePool))
                        .create();
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bot.stop();
            queuePool.close();
        }));
        bot.start();
    }
}
