package io.nightbeam.studio.xauctions.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Centralized scheduling bridge for Paper and Folia.
 *
 * <p>All platform detection and scheduler branching is isolated here to avoid
 * scheduler-specific code being spread across the plugin.</p>
 */
public final class PlatformAdapter {

    private final Plugin plugin;
    private final boolean folia;

    public PlatformAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    public void runGlobalTask(Runnable task) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runEntityTask(Entity entity, Runnable task) {
        if (entity == null) {
            runGlobalTask(task);
            return;
        }

        if (folia) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), () -> runGlobalTask(task));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runLocationTask(Location location, Runnable task) {
        if (location == null || location.getWorld() == null) {
            runGlobalTask(task);
            return;
        }

        if (folia) {
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;
            Bukkit.getRegionScheduler().execute(plugin, location.getWorld(), chunkX, chunkZ, task);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAsync(Runnable task) {
        if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public void runGlobalTaskLater(Runnable task, long delayTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public void runEntityTaskLater(Entity entity, Runnable task, long delayTicks) {
        if (entity == null) {
            runGlobalTaskLater(task, delayTicks);
            return;
        }

        if (folia) {
            entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), () -> runGlobalTask(task), delayTicks);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public void runAsyncRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        if (folia) {
            Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> task.run(),
                    delayTicks * 50L,
                    periodTicks * 50L,
                    TimeUnit.MILLISECONDS
            );
            return;
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    public void runGlobalRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delayTicks, periodTicks);
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }
}