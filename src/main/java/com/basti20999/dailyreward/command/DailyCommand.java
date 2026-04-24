package com.basti20999.dailyreward.command;

import com.basti20999.dailyreward.ColorUtil;
import com.basti20999.dailyreward.DailyReward;
import com.basti20999.dailyreward.PlayerData;
import com.basti20999.dailyreward.config.PluginConfig;
import com.basti20999.dailyreward.gui.GuiBuilder;
import com.basti20999.dailyreward.util.CooldownUtil;
import com.basti20999.dailyreward.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public final class DailyCommand implements CommandExecutor {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final DailyReward plugin;

    public DailyCommand(DailyReward plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            openGui(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "reset"  -> handleReset(sender, args);
            case "give"   -> handleGive(sender, args);
            case "stats"  -> handleStats(sender, args);
            case "top"    -> handleTop(sender);
            case "help"   -> sendHelp(sender);
            default       -> sendMessage(sender, plugin.cfg().msgUnknownSubcommand);
        }
        return true;
    }

    private void openGui(CommandSender sender) {
        PluginConfig cfg = plugin.cfg();
        if (!(sender instanceof Player p)) {
            sendMessage(sender, cfg.msgOnlyPlayers);
            return;
        }
        Inventory inv = GuiBuilder.build(plugin, p);
        p.openInventory(inv);
        plugin.getGuiRefresher().start(p, inv);
        SoundUtil.play(p, cfg.openSound.key(), cfg.openSound.volume(),
                cfg.openSound.pitch(), plugin.getLogger());
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("dailyreward.admin.reload")) {
            sendMessage(sender, plugin.cfg().msgNoPermission);
            return;
        }
        plugin.reloadPluginConfig();
        sendMessage(sender, plugin.cfg().msgReloaded);
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dailyreward.admin.reset")) {
            sendMessage(sender, plugin.cfg().msgNoPermission);
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, plugin.cfg().msgUsage.replace("{usage}", "/daily reset <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendMessage(sender, plugin.cfg().msgPlayerNotFound.replace("{player}", args[1]));
            return;
        }
        plugin.getRewardService().resetPlayer(target.getUniqueId());
        sendMessage(sender, plugin.cfg().msgReset.replace("{player}", args[1]));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dailyreward.admin.give")) {
            sendMessage(sender, plugin.cfg().msgNoPermission);
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, plugin.cfg().msgUsage.replace("{usage}", "/daily give <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sendMessage(sender, plugin.cfg().msgPlayerNotFound.replace("{player}", args[1]));
            return;
        }
        plugin.getRewardService().forceGive(target);
        sendMessage(sender, plugin.cfg().msgGiven.replace("{player}", target.getName()));
    }

    private void handleStats(CommandSender sender, String[] args) {
        PluginConfig cfg = plugin.cfg();
        String targetName;
        UUID targetUuid;

        if (args.length >= 2) {
            if (!sender.hasPermission("dailyreward.admin.stats")) {
                sendMessage(sender, cfg.msgNoPermission);
                return;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (!op.hasPlayedBefore() && !op.isOnline()) {
                sendMessage(sender, cfg.msgPlayerNotFound.replace("{player}", args[1]));
                return;
            }
            targetName = op.getName() != null ? op.getName() : args[1];
            targetUuid = op.getUniqueId();
        } else {
            if (!(sender instanceof Player p)) {
                sendMessage(sender, cfg.msgUsage.replace("{usage}", "/daily stats <player>"));
                return;
            }
            targetName = p.getName();
            targetUuid = p.getUniqueId();
        }

        PlayerData data = plugin.getPlayerDataOrNull(targetUuid);
        sendMessage(sender, cfg.msgStatsHeader.replace("{player}", targetName));
        if (data == null || !data.hasEverClaimed()) {
            sendMessage(sender, cfg.msgNoData);
            return;
        }
        String lastDate = DATE_FMT.format(new Date(data.lastClaimed()));
        long remaining = CooldownUtil.remainingMillis(data.lastClaimed(), cfg.cooldownHours);
        sendMessage(sender, statsLine(cfg, "Streak", String.valueOf(data.streak())));
        sendMessage(sender, statsLine(cfg, "Total claims", String.valueOf(data.totalClaims())));
        sendMessage(sender, statsLine(cfg, "Last claim", lastDate));
        sendMessage(sender, statsLine(cfg, "Next claim",
                remaining == 0 ? "Ready!" : CooldownUtil.format(remaining)));
    }

    private String statsLine(PluginConfig cfg, String label, String value) {
        return cfg.msgStatsLine.replace("{label}", label).replace("{value}", value);
    }

    private void handleTop(CommandSender sender) {
        PluginConfig cfg = plugin.cfg();
        sendMessage(sender, cfg.msgTopHeader);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, PlayerData> top;
            try {
                top = plugin.getDatabase().topStreaks(10);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load top streaks: " + e.getMessage());
                return;
            }
            if (top.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> sendMessage(sender, cfg.msgNoData));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                int rank = 1;
                for (var entry : top.entrySet()) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                    String name = op.getName() != null ? op.getName() : entry.getKey().toString().substring(0, 8);
                    String line = cfg.msgTopEntry
                            .replace("{rank}", String.valueOf(rank++))
                            .replace("{player}", name)
                            .replace("{streak}", String.valueOf(entry.getValue().streak()))
                            .replace("{total}", String.valueOf(entry.getValue().totalClaims()));
                    sendMessage(sender, line);
                }
            });
        });
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "&6=== DailyReward Commands ===");
        sendMessage(sender, "&e/daily &7- Open the reward GUI");
        sendMessage(sender, "&e/daily stats [player] &7- Show claim stats");
        sendMessage(sender, "&e/daily top &7- Show top streaks");
        if (sender.hasPermission("dailyreward.admin.reset"))
            sendMessage(sender, "&e/daily reset <player> &7- Reset a player's cooldown");
        if (sender.hasPermission("dailyreward.admin.give"))
            sendMessage(sender, "&e/daily give <player> &7- Force-give a reward");
        if (sender.hasPermission("dailyreward.admin.reload"))
            sendMessage(sender, "&e/daily reload &7- Reload config");
    }

    private static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ColorUtil.translate(message));
    }
}
