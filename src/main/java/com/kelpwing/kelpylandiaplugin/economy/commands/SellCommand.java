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
import org.bukkit.inventory.PlayerInventory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public SellCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /sell.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.sell")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /sell <hand|inventory|all> [amount]");
            return true;
        }

        Player player = (Player) sender;
        String mode = args[0].toLowerCase();
        int maxAmount = -1; // -1 = sell all of the selected items
        if (args.length > 1) {
            try {
                maxAmount = Integer.parseInt(args[1]);
                if (maxAmount < 1) {
                    sender.sendMessage(ChatColor.RED + "Amount must be at least 1.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }
        }

        BigDecimal totalEarned = BigDecimal.ZERO;
        int totalSold = 0;

        boolean applyTax = eco.isTaxEnabled() && eco.isTaxOnServerSell();

        switch (mode) {
            case "hand": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "You're not holding anything!");
                    return true;
                }
                EconomyManager.PriceResult pr = eco.getPrice(hand.getType());
                if (!pr.sellable) {
                    String reason = eco.getUnsellableReason(hand.getType());
                    sender.sendMessage(eco.getMessage("price-unsellable")
                            .replace("{item}", formatMaterial(hand.getType()))
                            .replace("{reason}", reason != null ? reason : "."));
                    return true;
                }
                int toSell = maxAmount > 0 ? Math.min(maxAmount, hand.getAmount()) : hand.getAmount();
                BigDecimal value = pr.price.multiply(BigDecimal.valueOf(toSell));
                totalEarned = totalEarned.add(value);
                totalSold += toSell;

                if (toSell >= hand.getAmount()) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    hand.setAmount(hand.getAmount() - toSell);
                }
                break;
            }

            case "inventory":
            case "all": {
                PlayerInventory inv = player.getInventory();
                int remaining = maxAmount;

                for (int slot = 0; slot < inv.getSize(); slot++) {
                    if (remaining == 0) break;
                    ItemStack item = inv.getItem(slot);
                    if (item == null || item.getType() == Material.AIR) continue;

                    EconomyManager.PriceResult pr = eco.getPrice(item.getType());
                    if (!pr.sellable) continue;

                    int toSell;
                    if (remaining > 0) {
                        toSell = Math.min(remaining, item.getAmount());
                        remaining -= toSell;
                    } else {
                        toSell = item.getAmount(); // maxAmount == -1, sell all
                    }

                    BigDecimal value = pr.price.multiply(BigDecimal.valueOf(toSell));
                    totalEarned = totalEarned.add(value);
                    totalSold += toSell;

                    if (toSell >= item.getAmount()) {
                        inv.setItem(slot, null);
                    } else {
                        item.setAmount(item.getAmount() - toSell);
                    }
                }
                break;
            }

            default:
                sender.sendMessage(ChatColor.RED + "Usage: /sell <hand|inventory|all> [amount]");
                return true;
        }

        if (totalSold == 0) {
            sender.sendMessage(eco.getMessage("sell-nothing"));
            return true;
        }

        // Apply tax if configured for server sells
        BigDecimal tax = BigDecimal.ZERO;
        if (applyTax) {
            tax = eco.calculateTax(totalEarned);
            totalEarned = totalEarned.subtract(tax);
        }

        eco.deposit(player.getUniqueId(), totalEarned);

        String totalStr = totalEarned.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
        sender.sendMessage(eco.getMessage("sell-summary")
                .replace("{count}", String.valueOf(totalSold))
                .replace("{unit}", eco.getUnit())
                .replace("{total}", totalStr));

        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            String taxStr = tax.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            sender.sendMessage(eco.getMessage("pay-tax-notice")
                    .replace("{unit}", eco.getUnit())
                    .replace("{tax}", taxStr)
                    .replace("{rate}", eco.getTaxRate().toPlainString()));
        }
        return true;
    }

    public static String formatMaterial(Material mat) {
        String name = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("hand", "inventory", "all").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return new ArrayList<>();
    }
}
