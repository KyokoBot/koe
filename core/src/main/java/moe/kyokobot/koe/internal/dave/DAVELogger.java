package moe.kyokobot.koe.internal.dave;

import moe.kyokobot.libdave.NativeDaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAVELogger {
    private static final Logger logger = LoggerFactory.getLogger(DAVELogger.class);

    public static void log(int severity, String file, int line, String message) {
        switch (severity) {
            case 0: // LS_VERBOSE
                logger.trace("[{}:{}] {}", file, line, message);
                break;
            case 1: // LS_INFO
                logger.debug("[{}:{}] {}", file, line, message);
                break;
            case 2: // LS_WARNING
                logger.warn("[{}:{}] {}", file, line, message);
                break;
            case 3: // LS_ERROR
                logger.error("[{}:{}] {}", file, line, message);
                break;
            default:
                break;
        }
    }

    public static void setNativeLoggingEnabled(boolean enableLogging) {
        if (enableLogging) {
            NativeDaveFactory.setLogSink(DAVELogger::log);
        } else {
            NativeDaveFactory.setLogSink(null);
        }
    }
}
