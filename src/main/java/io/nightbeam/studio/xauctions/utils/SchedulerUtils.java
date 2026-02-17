package io.nightbeam.studio.xauctions.utils;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Utility for abstracting scheduler Logic for Paper/Spigot/Folia support.
 */
public class SchedulerUtils {

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static void run(Plugin plugin, Runnable task) {
        adapter(plugin).runGlobalTask(task);
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        adapter(plugin).runAsync(task);
    }

    public static void runLater(Plugin plugin, Runnable task, long delayTicks) {
        adapter(plugin).runGlobalTaskLater(task, delayTicks);
    }

    public static void runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        adapter(plugin).runGlobalRepeatingTask(task, delayTicks, periodTicks);
    }

    public static void runTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        adapter(plugin).runAsyncRepeatingTask(task, delayTicks, periodTicks);
    }

    // Entity/Location specific scheduling for Folia
    public static void run(Plugin plugin, Entity entity, Runnable task) {
        adapter(plugin).runEntityTask(entity, task);
    }

    private static PlatformAdapter adapter(Plugin plugin) {
        if (plugin instanceof io.nightbeam.studio.xauctions.XAuctionsPlugin xaPlugin) {
            return xaPlugin.getPlatformAdapter();
        }
        return new PlatformAdapter(plugin);
    }
}
