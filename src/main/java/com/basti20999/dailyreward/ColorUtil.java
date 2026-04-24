package com.basti20999.dailyreward;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final String VALID_COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private ColorUtil() {}

    public static String translate(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            char[] chars = matcher.group(1).toCharArray();
            String replacement = "§x§" + chars[0] + "§" + chars[1]
                    + "§" + chars[2] + "§" + chars[3]
                    + "§" + chars[4] + "§" + chars[5];
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        char[] chars = buffer.toString().toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && VALID_COLOR_CODES.indexOf(chars[i + 1]) > -1) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static String strip(String message) {
        if (message == null) return "";
        return translate(message).replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
    }
}
