package moe.kyokobot.koe.testbot;

/**
 * Starts a Koe test bot instance with default configurations, without any extensions and etc.
 */
public class KoeTestBotLauncher {
    public static void main(String... args) {
        var bot = new TestBot(System.getenv("TOKEN"));
        Runtime.getRuntime().addShutdownHook(new Thread(bot::stop));
        bot.start();
    }
}
