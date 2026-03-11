package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * /enchant <enchantment> [level] — Apply an enchantment to the item in hand.
 * Supports unsafe enchantments (any level, any item) with the admin permission.
 */
public class EnchantCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    // Friendly name -> Enchantment lookup
    private static final Map<String, Enchantment> ENCHANT_MAP = new LinkedHashMap<>();

    static {
        for (Enchantment ench : Enchantment.values()) {
            // Use the key name (e.g. "sharpness", "fire_aspect")
            ENCHANT_MAP.put(ench.getKey().getKey().toLowerCase(), ench);
        }
    }

    public EnchantCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /enchant <enchantment> [level]");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must hold an item to enchant it.");
            return true;
        }

        String enchantName = args[0].toLowerCase().replace("-", "_");
        Enchantment enchantment = ENCHANT_MAP.get(enchantName);
        if (enchantment == null) {
            // Try NamespacedKey directly
            enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName));
        }
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Unknown enchantment: " + args[0]);
            player.sendMessage(ChatColor.GRAY + "Use tab-complete to see available enchantments.");
            return true;
        }

        int level = 1;
        if (args.length >= 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level: " + args[1]);
                return true;
            }
            if (level < 0) {
                player.sendMessage(ChatColor.RED + "Level must be 0 or higher.");
                return true;
            }
        }

        // Level 0 = remove the enchantment
        if (level == 0) {
            item.removeEnchantment(enchantment);
            player.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.GOLD + formatName(enchantment) + ChatColor.GREEN + " from your item.");
            return true;
        }

        boolean unsafe = player.hasPermission("kelpylandia.enchant.unsafe");
        if (unsafe) {
            // Bypass normal limits
            item.addUnsafeEnchantment(enchantment, level);
        } else {
            // Check if enchantment can apply to this item
            if (!enchantment.canEnchantItem(item)) {
                player.sendMessage(ChatColor.RED + "That enchantment cannot be applied to this item.");
                return true;
            }
            if (level > enchantment.getMaxLevel()) {
                player.sendMessage(ChatColor.RED + "Maximum level for " + formatName(enchantment) + " is " + enchantment.getMaxLevel() + ".");
                return true;
            }
            item.addEnchantment(enchantment, level);
        }

        player.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.GOLD + formatName(enchantment) + " " + level + ChatColor.GREEN + " to your item.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> results = new ArrayList<>();
            for (String name : ENCHANT_MAP.keySet()) {
                if (name.startsWith(prefix)) {
                    results.add(name);
                }
            }
            return results;
        }
        if (args.length == 2) {
            // Suggest common levels
            return Arrays.asList("1", "2", "3", "4", "5");
        }
        return Collections.emptyList();
    }

    private String formatName(Enchantment enchantment) {
        String key = enchantment.getKey().getKey();
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
