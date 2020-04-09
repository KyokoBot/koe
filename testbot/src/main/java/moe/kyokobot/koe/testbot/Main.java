package moe.kyokobot.koe.testbot;

public class Main {
    public static void main(String... args) {
        var bot = new TestBot(System.getenv("TOKEN"));
        Runtime.getRuntime().addShutdownHook(new Thread(bot::stop));
        bot.start();
    }
}
