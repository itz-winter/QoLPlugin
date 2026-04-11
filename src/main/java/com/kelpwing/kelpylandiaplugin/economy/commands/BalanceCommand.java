package com.kelpwing.kelpylandiaplugin.economy.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public BalanceCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }

        if (args.length == 0) {
            // Check own balance
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /balance <player>");
                return true;
            }
            if (!sender.hasPermission("qol.economy.use")) {
                sender.sendMessage(eco.getMessage("no-permission"));
                return true;
            }
            Player player = (Player) sender;
            String msg = eco.getMessage("balance-self")
                    .replace("{unit}", eco.getUnit())
                    .replace("{balance}", eco.formatMoney(eco.getBalance(player.getUniqueId())).replace(eco.getUnit(), ""));
            player.sendMessage(msg);
        } else {
            // Check another player's balance
            if (!sender.hasPermission("qol.economy.use")) {
                sender.sendMessage(eco.getMessage("no-permission"));
                return true;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!eco.hasAccount(target.getUniqueId()) && !target.hasPlayedBefore()) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            eco.createAccount(target.getUniqueId());
            String name = target.getName() != null ? target.getName() : args[0];
            String bal = eco.getBalance(target.getUniqueId())
                    .setScale(eco.getDecimals(), java.math.RoundingMode.HALF_UP).toPlainString();
            String msg = eco.getMessage("balance-other")
                    .replace("{player}", name)
                    .replace("{unit}", eco.getUnit())
                    .replace("{balance}", bal);
            sender.sendMessage(msg);
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
