package moe.kyokobot.koe.experimental;

import moe.kyokobot.libdave.netty.NettyDaveFactory;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DaveFactoryProvider {
    @Nullable NettyDaveFactory getFactory(boolean enableLogSink);
}
