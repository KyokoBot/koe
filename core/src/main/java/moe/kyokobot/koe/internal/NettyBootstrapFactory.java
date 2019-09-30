package moe.kyokobot.koe.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import moe.kyokobot.koe.KoeOptions;

public class NettyBootstrapFactory {
    private NettyBootstrapFactory() {
        //
    }

    public static Bootstrap socket(KoeOptions options) {
        return new Bootstrap()
                .group(options.getEventLoopGroup())
                .channel(options.getSocketChannelClass());
    }

    public static Bootstrap datagram(KoeOptions options) {
        var bootstrap = new Bootstrap()
                .group(options.getEventLoopGroup())
                .channel(options.getDatagramChannelClass())
                .option(ChannelOption.SO_REUSEADDR, true);

        if (options.isHighPacketPriority()) {
            bootstrap.option(ChannelOption.IP_TOS, 0x10 | 0x08); // IPTOS_LOWDELAY | IPTOS_THROUGHPUT
        }

        return bootstrap;
    }
}
