package com.basti20999.dailyreward.util;

public final class CooldownUtil {

    private CooldownUtil() {}

    public static long cooldownMillis(long cooldownHours) {
        return cooldownHours * 60L * 60L * 1000L;
    }

    public static boolean isReady(long lastClaimed, long cooldownHours) {
        if (lastClaimed <= 0L) return true;
        return System.currentTimeMillis() - lastClaimed >= cooldownMillis(cooldownHours);
    }

    public static long remainingMillis(long lastClaimed, long cooldownHours) {
        if (lastClaimed <= 0L) return 0L;
        long remaining = cooldownMillis(cooldownHours) - (System.currentTimeMillis() - lastClaimed);
        return Math.max(remaining, 0L);
    }

    public static String format(long millis) {
        if (millis <= 0L) return "Ready!";
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %02ds", minutes, seconds);
        return String.format("%ds", seconds);
    }
}
