package com.kelpwing.kelpylandiaplugin.economy.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PriceCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public PriceCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.price")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }

        Material material;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify an item: /price <item>");
                return true;
            }
            Player player = (Player) sender;
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                sender.sendMessage(ChatColor.RED + "You're not holding anything! Specify an item: /price <item>");
                return true;
            }
            material = hand.getType();
        } else {
            material = EconomyManager.parseMaterial(args[0]);
            if (material == null) {
                sender.sendMessage(ChatColor.RED + "Unknown item: " + args[0]);
                return true;
            }
        }

        EconomyManager.PriceResult result = eco.getPrice(material);
        String itemName = SellCommand.formatMaterial(material);

        if (!result.sellable) {
            String reason = eco.getUnsellableReason(material);
            sender.sendMessage(eco.getMessage("price-unsellable")
                    .replace("{item}", itemName)
                    .replace("{reason}", reason != null ? reason : "."));
            return true;
        }

        String priceStr = result.price.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
        sender.sendMessage(eco.getMessage("price-info")
                .replace("{item}", itemName)
                .replace("{unit}", eco.getUnit())
                .replace("{price}", priceStr));

        if (result.categoryName != null) {
            sender.sendMessage(eco.getMessage("price-category")
                    .replace("{category}", result.categoryName));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toUpperCase();
            return Stream.of(Material.values())
                    .filter(m -> m.isItem() && !m.isAir())
                    .map(m -> m.name().toLowerCase())
                    .filter(n -> n.startsWith(prefix.toLowerCase()))
                    .limit(30)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
