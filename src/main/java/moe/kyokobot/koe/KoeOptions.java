package moe.kyokobot.koe;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Objects;

public class KoeOptions {
    private final EventLoopGroup eventLoopGroup;

    KoeOptions(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public static KoeOptions defaultOptions() {
        var eventLoop = Epoll.isAvailable()
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();

        return new KoeOptions(eventLoop);
    }
}
