package com.basti20999.dailyreward.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Scheduler abstraction that transparently supports both Bukkit/Paper and Folia.
 * On Folia, the Bukkit scheduler is not available; we delegate to the regional
 * schedulers instead. Detection happens once at class-load time via reflection.
 */
public final class SchedulerUtil {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private SchedulerUtil() {}

    public static boolean isFolia() {
        return FOLIA;
    }

    /** Run a task asynchronously (off the main/region thread). */
    public static void async(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /** Schedule a task on the next global/main-thread tick. */
    public static void sync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Start a repeating task bound to a specific player's entity region (Folia)
     * or the main thread (Bukkit/Paper).  Returns a {@link CancelableTask} that
     * can be used to stop the task later.
     */
    public static CancelableTask repeatForPlayer(Plugin plugin, Player player,
                                                 Runnable task,
                                                 long delayTicks, long periodTicks) {
        if (FOLIA) {
            var scheduled = player.getScheduler().runAtFixedRate(
                    plugin, t -> task.run(), null, delayTicks, periodTicks);
            return scheduled != null ? scheduled::cancel : () -> {};
        } else {
            var bukkit = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return bukkit::cancel;
        }
    }

    /**
     * Dispatch a console command.  On Folia this is routed through the global
     * region scheduler so it runs in a safe context.
     */
    public static void dispatchConsoleCommand(Plugin plugin, String command) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin,
                    t -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    @FunctionalInterface
    public interface CancelableTask {
        void cancel();
    }
}
