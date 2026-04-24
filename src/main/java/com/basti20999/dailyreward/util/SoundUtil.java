package com.basti20999.dailyreward.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class SoundUtil {

    private SoundUtil() {}

    public static void play(Player player, String soundKey, float volume, float pitch, Logger logger) {
        if (player == null || soundKey == null || soundKey.isBlank()) return;

        String normalized = soundKey.toLowerCase().replace('_', '.');
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
        if (sound == null) {
            if (logger != null) logger.warning("Unknown sound '" + soundKey + "', skipping.");
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
