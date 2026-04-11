package com.kelpwing.kelpylandiaplugin.economy.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DelPriceCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public DelPriceCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.delprice")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /delprice <item|#category>");
            return true;
        }

        String target = args[0];

        if (target.startsWith("#")) {
            String tagName = target.substring(1);
            Tag<Material> tag = EconomyManager.resolveTag(tagName);
            if (tag == null) {
                sender.sendMessage(ChatColor.RED + "Unknown category tag: " + tagName);
                return true;
            }
            if (eco.removeCategoryPrice(tagName)) {
                sender.sendMessage(ChatColor.GREEN + "Removed price for category " + ChatColor.YELLOW + "#" + tagName + ChatColor.GREEN + ".");
            } else {
                sender.sendMessage(ChatColor.RED + "Category #" + tagName + " does not have a configured price.");
            }
        } else {
            Material material = EconomyManager.parseMaterial(target);
            if (material == null) {
                sender.sendMessage(ChatColor.RED + "Unknown item: " + target);
                return true;
            }
            if (eco.removePrice(material)) {
                sender.sendMessage(ChatColor.GREEN + "Removed price for " + ChatColor.YELLOW + SellCommand.formatMaterial(material) + ChatColor.GREEN + ".");
            } else {
                sender.sendMessage(ChatColor.RED + SellCommand.formatMaterial(material) + " does not have a configured price.");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            EconomyManager eco = plugin.getEconomyManager();
            if (eco == null) return new ArrayList<>();

            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            // show configured item prices
            Map<String, Double> prices = eco.getAllConfiguredPrices();
            for (String key : prices.keySet()) {
                String suggest;
                if (key.contains(":")) {
                    suggest = "#" + key; // category
                } else {
                    suggest = key.toLowerCase();
                }
                if (suggest.toLowerCase().startsWith(prefix)) {
                    suggestions.add(suggest);
                }
            }

            return suggestions.stream().limit(30).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
