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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetPriceCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public SetPriceCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.setprice")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setprice <item|#category> <price>");
            return true;
        }

        String target = args[0];
        BigDecimal price;
        try {
            price = new BigDecimal(args[1]);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage(ChatColor.RED + "Price cannot be negative.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid price: " + args[1]);
            return true;
        }

        price = price.setScale(eco.getDecimals(), RoundingMode.HALF_UP);

        if (target.startsWith("#")) {
            // category tag price
            String tagName = target.substring(1);
            Tag<Material> tag = EconomyManager.resolveTag(tagName);
            if (tag == null) {
                sender.sendMessage(ChatColor.RED + "Unknown category tag: " + tagName);
                sender.sendMessage(ChatColor.GRAY + "Example: #minecraft:planks, #minecraft:logs");
                return true;
            }
            eco.setCategoryPrice(tagName, price);
            sender.sendMessage(ChatColor.GREEN + "Set category " + ChatColor.YELLOW + "#" + tagName + ChatColor.GREEN + " price to " + ChatColor.YELLOW + eco.getUnit() + price.toPlainString() + ChatColor.GREEN + ".");
        } else {
            // individual item price
            Material material = EconomyManager.parseMaterial(target);
            if (material == null) {
                sender.sendMessage(ChatColor.RED + "Unknown item: " + target);
                return true;
            }
            eco.setPrice(material, price);
            sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + SellCommand.formatMaterial(material) + ChatColor.GREEN + " price to " + ChatColor.YELLOW + eco.getUnit() + price.toPlainString() + ChatColor.GREEN + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            // material names
            Stream.of(Material.values())
                    .filter(m -> m.isItem() && !m.isAir())
                    .map(m -> m.name().toLowerCase())
                    .filter(n -> n.startsWith(prefix))
                    .limit(20)
                    .forEach(suggestions::add);

            // common category tags
            if ("#".startsWith(prefix) || prefix.startsWith("#")) {
                String tagPrefix = prefix.startsWith("#") ? prefix.substring(1) : "";
                List<String> tags = new ArrayList<>(Arrays.asList(
                        "#minecraft:planks", "#minecraft:logs", "#minecraft:wool",
                        "#minecraft:flowers", "#minecraft:saplings", "#minecraft:sand",
                        "#minecraft:coral_blocks", "#minecraft:terracotta",
                        "#minecraft:stairs", "#minecraft:slabs", "#minecraft:walls",
                        "#minecraft:fences", "#minecraft:buttons", "#minecraft:doors"
                ));
                // Add custom categories from economy.yml
                EconomyManager eco2 = plugin.getEconomyManager();
                if (eco2 != null) {
                    for (String catName : eco2.getCustomCategories().keySet()) {
                        tags.add("#" + catName);
                    }
                }
                tags.stream()
                        .filter(t -> t.substring(1).startsWith(tagPrefix))
                        .forEach(suggestions::add);
            }

            return suggestions.stream().limit(30).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "5", "10", "50", "100");
        }
        return new ArrayList<>();
    }
}
