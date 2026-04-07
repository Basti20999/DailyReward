package com.basti20999.dailyReward;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RewardCommand implements CommandExecutor {

    private final DailyReward plugin;

    public RewardCommand(DailyReward plugin) {
        this.plugin = plugin;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            if (lore != null) {
                lore.replaceAll(ColorUtil::translate);
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ColorUtil.translate(plugin.getConfig().getString(
                    "messages.general.only_players", "&cOnly players can use this command.")));
            return true;
        }

        String title = ColorUtil.translate(plugin.getConfig().getString("gui.title", "&8Daily Reward"));
        int invSize = plugin.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, invSize, title);

        long now = System.currentTimeMillis();
        Long last = plugin.getRewardTimes().get(p.getUniqueId());
        long cooldownMillis = plugin.getConfig().getLong("cooldown_hours", 24) * 60L * 60L * 1000L;
        boolean ready = last == null || now - last >= cooldownMillis;

        String rewardName = ready
                ? plugin.getConfig().getString("gui.reward_name_ready", "&aCollect reward!")
                : plugin.getConfig().getString("gui.reward_name_cooldown", "&cAlready collected!");
        List<String> lore = plugin.getConfig().getStringList("gui.reward_lore");

        Material rewardMaterial = parseMaterial(
                plugin.getConfig().getString("gui.reward_material"), Material.CHEST);
        ItemStack rewardItem = createItem(rewardMaterial, rewardName, lore);
        int rewardSlot = plugin.getConfig().getInt("gui.reward_slot", 13);
        inv.setItem(rewardSlot, rewardItem);

        Material borderMaterial = parseMaterial(
                plugin.getConfig().getString("gui.border_material"), Material.BLACK_STAINED_GLASS_PANE);
        String borderName = plugin.getConfig().getString("gui.border_name", " ");
        ItemStack borderItem = createItem(borderMaterial, borderName, null);
        for (int slot : plugin.getConfig().getIntegerList("gui.border_slots")) {
            inv.setItem(slot, borderItem);
        }

        p.openInventory(inv);

        String soundName = plugin.getConfig().getString("sounds.open.sound", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) plugin.getConfig().getDouble("sounds.open.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.open.pitch", 1.0);
        playSound(p, soundName, volume, pitch);

        return true;
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material '" + name + "', using fallback: " + fallback.name());
            return fallback;
        }
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown sound '" + soundName + "', skipping.");
        }
    }
}
