package com.kelpwing.kelpylandiaplugin.economy.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public PayCommand(KelpylandiaPlugin plugin) {
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
            sender.sendMessage(ChatColor.RED + "Only players can use /pay.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.pay")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        if (target.equals(player)) {
            sender.sendMessage(ChatColor.RED + "You can't pay yourself!");
            return true;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]).setScale(eco.getDecimals(), RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }

        if (!eco.hasBalance(player.getUniqueId(), amount)) {
            String bal = eco.getBalance(player.getUniqueId()).setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            sender.sendMessage(eco.getMessage("insufficient-funds")
                    .replace("{unit}", eco.getUnit())
                    .replace("{balance}", bal));
            return true;
        }

        // Calculate tax (only when enabled AND configured for player-to-player)
        BigDecimal tax = (eco.isTaxEnabled() && eco.isTaxOnPay()) ? eco.calculateTax(amount) : BigDecimal.ZERO;
        BigDecimal received = amount.subtract(tax);

        eco.withdraw(player.getUniqueId(), amount);
        eco.deposit(target.getUniqueId(), received);

        String amtStr = amount.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
        String rcvStr = received.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();

        player.sendMessage(eco.getMessage("pay-sent")
                .replace("{unit}", eco.getUnit())
                .replace("{amount}", amtStr)
                .replace("{player}", target.getName()));

        target.sendMessage(eco.getMessage("pay-received")
                .replace("{unit}", eco.getUnit())
                .replace("{amount}", rcvStr)
                .replace("{player}", player.getName()));

        eco.sendTransactionHUD(player, amount, false);
        eco.sendTransactionHUD(target, received, true);

        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            String taxStr = tax.setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            String rateStr = eco.getTaxRate().toPlainString();
            player.sendMessage(eco.getMessage("pay-tax-notice")
                    .replace("{unit}", eco.getUnit())
                    .replace("{tax}", taxStr)
                    .replace("{rate}", rateStr));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
