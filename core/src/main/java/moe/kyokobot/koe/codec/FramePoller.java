package moe.kyokobot.koe.codec;

public interface FramePoller {
    boolean isPolling();

    void start();

    void stop();
}
