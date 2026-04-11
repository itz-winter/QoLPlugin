package com.kelpwing.kelpylandiaplugin.economy.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public ValueCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.value")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            // value of held item
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify an item: /value <item> [amount]");
                return true;
            }
            Player player = (Player) sender;
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                sender.sendMessage(ChatColor.RED + "You're not holding anything! Specify an item: /value <item> [amount]");
                return true;
            }

            // check if it's a shulker box
            if (isShulkerBox(hand.getType()) && hand.hasItemMeta() && hand.getItemMeta() instanceof BlockStateMeta) {
                showShulkerValue(sender, eco, hand);
            } else {
                showItemValue(sender, eco, hand.getType(), hand.getAmount());
            }
            return true;
        }

        // /value <item> [amount]
        Material material = EconomyManager.parseMaterial(args[0]);
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "Unknown item: " + args[0]);
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1) {
                    sender.sendMessage(ChatColor.RED + "Amount must be at least 1.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }
        }

        showItemValue(sender, eco, material, amount);
        return true;
    }

    private void showItemValue(CommandSender sender, EconomyManager eco, Material material, int amount) {
        String itemName = SellCommand.formatMaterial(material);
        EconomyManager.PriceResult result = eco.getPrice(material);

        if (!result.sellable) {
            String reason = eco.getUnsellableReason(material);
            sender.sendMessage(eco.getMessage("price-unsellable")
                    .replace("{item}", itemName)
                    .replace("{reason}", reason != null ? reason : "."));
            return;
        }

        BigDecimal unitPrice = result.price;
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(amount));

        boolean taxApplied = eco.isTaxEnabled() && eco.isTaxOnServerSell();
        BigDecimal afterTax = taxApplied ? eco.afterTax(total) : total;

        String priceStr = unitPrice.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
        String totalStr = afterTax.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();

        sender.sendMessage(eco.getMessage("value-info")
                .replace("{item}", itemName)
                .replace("{amount}", String.valueOf(amount))
                .replace("{unit}", eco.getUnit())
                .replace("{price}", priceStr)
                .replace("{total}", totalStr));

        if (result.categoryName != null) {
            sender.sendMessage(eco.getMessage("price-category")
                    .replace("{category}", result.categoryName));
        }

        if (taxApplied) {
            BigDecimal taxAmount = total.subtract(afterTax);
            sender.sendMessage(eco.getMessage("value-tax")
                    .replace("{rate}", eco.getTaxRateDisplay())
                    .replace("{tax}", taxAmount.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString())
                    .replace("{unit}", eco.getUnit()));
        }
    }

    private void showShulkerValue(CommandSender sender, EconomyManager eco, ItemStack shulker) {
        BigDecimal contentsValue = eco.getShulkerValue(shulker);
        int sellableCount = eco.getShulkerSellableCount(shulker);

        BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        int totalItems = (int) Arrays.stream(box.getInventory().getContents())
                .filter(i -> i != null && i.getType() != Material.AIR)
                .count();

        boolean taxApplied = eco.isTaxEnabled() && eco.isTaxOnServerSell();
        BigDecimal afterTax = taxApplied ? eco.afterTax(contentsValue) : contentsValue;

        sender.sendMessage(ChatColor.GOLD + "---- Shulker Box Value ----");
        sender.sendMessage(ChatColor.YELLOW + "Sellable stacks: " + ChatColor.WHITE + sellableCount + "/" + totalItems);
        sender.sendMessage(ChatColor.YELLOW + "Contents value: " + ChatColor.GREEN + eco.getUnit() + afterTax.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString());

        if (taxApplied) {
            BigDecimal taxAmount = contentsValue.subtract(afterTax);
            sender.sendMessage(ChatColor.GRAY + "(Tax: " + eco.getTaxRateDisplay() + " = " + eco.getUnit() + taxAmount.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString() + ")");
        }

        // also check the shulker box itself
        EconomyManager.PriceResult boxResult = eco.getPrice(shulker.getType());
        if (boxResult.sellable) {
            sender.sendMessage(ChatColor.GRAY + "(Empty box worth: " + eco.getUnit() + boxResult.price.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString() + ")");
        }
    }

    private boolean isShulkerBox(Material m) {
        return m.name().contains("SHULKER_BOX");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of(Material.values())
                    .filter(m -> m.isItem() && !m.isAir())
                    .map(m -> m.name().toLowerCase())
                    .filter(n -> n.startsWith(prefix))
                    .limit(30)
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "16", "32", "64");
        }
        return new ArrayList<>();
    }
}
