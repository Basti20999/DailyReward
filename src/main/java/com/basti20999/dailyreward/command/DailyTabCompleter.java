package com.basti20999.dailyreward.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class DailyTabCompleter implements TabCompleter {

    private static final List<String> ALL_SUBS = Arrays.asList(
            "stats", "top", "help", "reset", "give", "reload");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            List<String> allowed = new ArrayList<>();
            for (String sub : ALL_SUBS) {
                if (isAllowed(sender, sub)
                        && sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    allowed.add(sub);
                }
            }
            return allowed;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reset") || sub.equals("give") || sub.equals("stats")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        names.add(p.getName());
                    }
                }
                return names;
            }
        }
        return List.of();
    }

    private static boolean isAllowed(CommandSender sender, String sub) {
        return switch (sub) {
            case "reset"  -> sender.hasPermission("dailyreward.admin.reset");
            case "give"   -> sender.hasPermission("dailyreward.admin.give");
            case "reload" -> sender.hasPermission("dailyreward.admin.reload");
            case "stats", "top", "help" -> true;
            default -> false;
        };
    }
}
