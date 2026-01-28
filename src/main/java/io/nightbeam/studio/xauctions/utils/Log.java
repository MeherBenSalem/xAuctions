package io.nightbeam.studio.xauctions.utils;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    private static final String PREFIX = "[xAuctions] ";

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void info(String message, Object... args) {
        getLogger().info(String.format(message, args));
    }

    public static void warn(String message) {
        getLogger().warning(message);
    }

    public static void warn(String message, Throwable t) {
        getLogger().log(Level.WARNING, message, t);
    }

    public static void error(String message) {
        getLogger().severe(message);
    }

    public static void error(String message, Throwable t) {
        getLogger().log(Level.SEVERE, message, t);
    }

    public static void debug(String message) {
        if (XAuctionsPlugin.getInstance().isDebugMode()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public static void debug(String message, Object... args) {
        if (XAuctionsPlugin.getInstance().isDebugMode()) {
            getLogger().info("[DEBUG] " + String.format(message, args));
        }
    }

    private static Logger getLogger() {
        return XAuctionsPlugin.getInstance().getLogger();
    }
}
