package com.basti20999.dailyReward;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for translating color codes in strings.
 * Supports both hex colors (#RRGGBB) and legacy Bukkit color codes (&amp;a, &amp;b, ...).
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    /**
     * Translates hex color codes (#RRGGBB) and legacy codes (&amp;x) into Minecraft color codes.
     *
     * @param message the raw message string
     * @return the colorized string
     */
    @SuppressWarnings("deprecation")
    public static String translate(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char[] chars = matcher.group(1).toCharArray();
            String replacement = "§x§" + chars[0] + "§" + chars[1] + "§"
                    + chars[2] + "§" + chars[3] + "§" + chars[4] + "§" + chars[5];
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
