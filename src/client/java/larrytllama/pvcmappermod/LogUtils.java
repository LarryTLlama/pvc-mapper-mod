package larrytllama.pvcmappermod;

import org.slf4j.Logger;

public class LogUtils {
    private static final Logger LOGGER = PVCMapperMod.LOGGER;

    public static void info(Object msg) {
        LOGGER.info("{}", msg);
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

    public static void warn(Object msg) {
        LOGGER.warn("{}", msg);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));
    }

    public static void warn(String msg, Throwable e) {
        LOGGER.warn(msg, e);
    }

    public static void error(Object msg) {
        LOGGER.error("{}", msg);
    }

    public static void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
    }

    public static void error(String format, Object... args) {
        LOGGER.error(String.format(format, args));
    }

    public static void debug(Object msg) {
        if (isDebugEnabled()) {
            // Route through INFO so the in-game debugMode toggle appears in console/latest.log under default Log4j levels
            LOGGER.info("[DEBUG] {}", msg);
        }
    }

    public static void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            LOGGER.info("[DEBUG] {}", String.format(format, args));
        }
    }

    private static boolean isDebugEnabled() {
        SettingsProvider sp = SettingsProvider.getInstance();
        return sp != null && sp.debugMode;
    }
}
