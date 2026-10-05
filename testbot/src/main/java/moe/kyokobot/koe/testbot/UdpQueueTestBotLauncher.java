package moe.kyokobot.koe.testbot;

import moe.kyokobot.koe.experimental.KoeOptionsBuilderExperimental;
import moe.kyokobot.koe.experimental.KoeOptionsExperimental;
import moe.kyokobot.koe.poller.udpqueue.QueueManagerPool;
import moe.kyokobot.koe.poller.udpqueue.UdpQueueFramePollerFactory;

public class UdpQueueTestBotLauncher {
    public static void main(String[] args) {
        var queuePool = new QueueManagerPool();

        var bot = new TestBot(System.getenv("TOKEN")) {
            @Override
            public KoeOptionsExperimental configureKoe(KoeOptionsBuilderExperimental options) {
                return (KoeOptionsExperimental) options
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
