package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.JailManager;
import com.kelpwing.kelpylandiaplugin.utils.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /jail <player> <duration> [reason]
 * Duration CANNOT be permanent.
 */
public class JailCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public JailCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jail <player> <duration> [reason]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player " + ChatColor.GOLD + args[0] + ChatColor.RED + " is not online.");
            return true;
        }

        // Prevent self-jailing
        if (sender instanceof Player && target.getUniqueId().equals(((Player) sender).getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You cannot jail yourself!");
            return true;
        }

        // Parse duration
        long durationMs = DurationParser.parseDuration(args[1]);
        if (durationMs <= 0) {
            sender.sendMessage(ChatColor.RED + "Jail duration must be a valid, non-permanent time (e.g. 10m, 1h, 1d).");
            return true;
        }

        // Build reason
        String reason = "No reason given";
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(" ");
                sb.append(args[i]);
            }
            reason = sb.toString();
        }

        String staffName = sender instanceof Player ? sender.getName() : "Console";
        JailManager jm = plugin.getJailManager();

        // Check if already jailed
        if (jm.isJailed(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already jailed!");
            return true;
        }

        jm.jailPlayer(target.getUniqueId(), target.getName(), staffName, reason, durationMs);

        // Format duration for display
        String durationStr = formatDuration(durationMs);

        target.sendMessage(ChatColor.RED + "You have been jailed for " + ChatColor.GOLD + durationStr +
                ChatColor.RED + ". Reason: " + ChatColor.WHITE + reason);

        sender.sendMessage(ChatColor.GREEN + "Jailed " + ChatColor.GOLD + target.getName() +
                ChatColor.GREEN + " for " + ChatColor.GOLD + durationStr +
                ChatColor.GREEN + ". Reason: " + ChatColor.WHITE + reason);

        // Broadcast
        Bukkit.broadcastMessage(ChatColor.RED + "[Jail] " + ChatColor.GOLD + target.getName() +
                ChatColor.RED + " has been jailed for " + ChatColor.GOLD + durationStr +
                ChatColor.RED + " by " + ChatColor.GOLD + staffName +
                ChatColor.RED + ". Reason: " + ChatColor.WHITE + reason);

        return true;
    }

    private String formatDuration(long ms) {
        long totalSec = ms / 1000;
        long days = totalSec / 86400;
        long hours = (totalSec % 86400) / 3600;
        long minutes = (totalSec % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        if (sb.length() == 0) sb.append(totalSec).append("s");
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) names.add(p.getName());
            }
            Collections.sort(names);
            return names;
        }
        if (args.length == 2) {
            return DurationParser.getTabCompletions(args[1]);
        }
        return Collections.emptyList();
    }
}
