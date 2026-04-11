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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class BaltopCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public BaltopCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return true;
        }
        if (!sender.hasPermission("qol.economy.baltop")) {
            sender.sendMessage(eco.getMessage("no-permission"));
            return true;
        }

        // Check if sender is excluded
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("qol.economy.baltop.exclude")) {
                sender.sendMessage(eco.getMessage("baltop-excluded"));
                // Still show the list though, just with the notice
            }
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }
        if (page < 1) page = 1;

        int pageSize = eco.getBaltopPageSize();
        List<Map.Entry<UUID, BigDecimal>> sorted = eco.getSortedBalances();
        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / pageSize));

        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, sorted.size());

        // Header
        sender.sendMessage(eco.getMessage("baltop-header")
                .replace("{top}", String.valueOf(pageSize))
                .replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(totalPages)));

        // Entries
        for (int i = start; i < end; i++) {
            Map.Entry<UUID, BigDecimal> entry = sorted.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : entry.getKey().toString().substring(0, 8);
            String bal = entry.getValue().setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();

            sender.sendMessage(eco.getMessage("baltop-entry")
                    .replace("{rank}", String.valueOf(i + 1))
                    .replace("{player}", name)
                    .replace("{unit}", eco.getUnit())
                    .replace("{balance}", bal));
        }

        // Footer
        if (page < totalPages) {
            sender.sendMessage(eco.getMessage("baltop-footer")
                    .replace("{next}", String.valueOf(page + 1)));
        }

        // Player's own rank
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int rank = -1;
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getKey().equals(player.getUniqueId())) {
                    rank = i + 1;
                    break;
                }
            }
            String bal = eco.getBalance(player.getUniqueId())
                    .setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            String rankStr = rank > 0 ? String.valueOf(rank) : "N/A";
            sender.sendMessage(eco.getMessage("baltop-self")
                    .replace("{rank}", rankStr)
                    .replace("{unit}", eco.getUnit())
                    .replace("{balance}", bal));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
