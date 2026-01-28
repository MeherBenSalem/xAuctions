package io.nightbeam.studio.xauctions.utils;

import io.nightbeam.studio.xauctions.XAuctionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility for abstracting scheduler Logic for Paper/Spigot/Folia support.
 */
public class SchedulerUtils {

    private static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public static void run(Plugin plugin, Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia) {
            long delayMs = delayTicks * 50;
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (t) -> task.run(), delayMs / 50); // expects ticks for
                                                                                                   // global?
            // Actually GlobalRegionScheduler.runDelayed takes long delayTicks.
            // Wait, check API.
            // Folia API: runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long
            // delayTicks)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (t) -> task.run(), delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            long delayMs = delayTicks * 50;
            long periodMs = periodTicks * 50;
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (t) -> task.run(), delayMs, periodMs,
                    TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    // Entity/Location specific scheduling for Folia
    public static void run(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia) {
            entity.getScheduler().run(plugin, (t) -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
