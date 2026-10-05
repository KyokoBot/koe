package moe.kyokobot.koe.internal;

import moe.kyokobot.koe.experimental.DaveFactoryProvider;
import moe.kyokobot.koe.internal.dave.DAVELogger;
import moe.kyokobot.libdave.callbacks.DaveLogSink;
import moe.kyokobot.libdave.netty.NativeNettyDaveFactory;
import moe.kyokobot.libdave.netty.NettyDaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDaveFactoryProvider implements DaveFactoryProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDaveFactoryProvider.class);

    public static final DefaultDaveFactoryProvider INSTANCE = new DefaultDaveFactoryProvider();

    private DefaultDaveFactoryProvider() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public NettyDaveFactory getFactory(boolean enableLogSink) {
        // TODO: We have a pure Java implementation planned.
        try {
            var factoryClass = (Class<? extends NettyDaveFactory>) Class.forName("moe.kyokobot.libdave.netty.FfmNettyDaveFactory");
            return tryImplementation(factoryClass, enableLogSink, "FFM");
        } catch (Throwable e) {
            logger.debug("FFM implementation not available", e);
        }

        try {
            return tryImplementation(NativeNettyDaveFactory.class, enableLogSink, "JNI");
        } catch (RuntimeException e) {
            logger.warn("DAVE requested but the native library could not be loaded! Did you forget to include 'moe.kyokobot.libdave:natives-{platform}' dependency in your project?", e);
        } catch (ReflectiveOperationException e) {
            logger.warn("Native implementation not available", e);
        }

        return null;
    }

    private NettyDaveFactory tryImplementation(Class<? extends NettyDaveFactory> factoryClass, boolean enableLogSink, String implName) throws ReflectiveOperationException {
        factoryClass.getMethod("ensureAvailable").invoke(null);
        logger.debug("Using {} DAVE implementation", implName);

        if (enableLogSink) {
            factoryClass.getMethod("setLogSink", DaveLogSink.class).invoke(null, (DaveLogSink) DAVELogger::log);
        } else {
            factoryClass.getMethod("setLogSink", DaveLogSink.class).invoke(null, (DaveLogSink) null);
        }

        return factoryClass.getDeclaredConstructor().newInstance();
    }
}
