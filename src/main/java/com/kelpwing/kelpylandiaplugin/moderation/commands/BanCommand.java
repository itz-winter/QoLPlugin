package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import com.kelpwing.kelpylandiaplugin.utils.DurationParser;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class BanCommand implements CommandExecutor, TabCompleter {
    private final KelpylandiaPlugin plugin;

    public BanCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.ban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ban <player> [duration] [reason]");
            sender.sendMessage(ChatColor.YELLOW + "Duration formats: 1y2w3d4h5m (years/weeks/days/hours/minutes), inf/permanent");
            return true;
        }

        String targetName = args[0];
        
        // Prevent self-punishment for ban command only
        if (targetName.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(ChatColor.RED + "You cannot ban yourself!");
            return true;
        }
        
        String durationStr;
        String reason;
        
        if (args.length == 1) {
            // Only player name provided
            durationStr = "permanent";
            reason = "No reason provided";
        } else if (args.length == 2) {
            // Check if second argument is a valid duration
            if (DurationParser.isValidDuration(args[1]) || args[1].equalsIgnoreCase("permanent") 
                || args[1].equalsIgnoreCase("inf") || args[1].equalsIgnoreCase("infinite") 
                || args[1].equalsIgnoreCase("perm")) {
                durationStr = args[1];
                reason = "No reason provided";
            } else {
                // Second argument is reason, duration is permanent
                durationStr = "permanent";
                reason = args[1];
            }
        } else {
            // Three or more arguments: player duration reason...
            durationStr = args[1];
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        }

        // Parse duration - if "permanent" or infinite keywords, set to 0 (permanent)
        long duration;
        if (durationStr.equals("permanent") || durationStr.equalsIgnoreCase("inf") 
            || durationStr.equalsIgnoreCase("infinite") || durationStr.equalsIgnoreCase("perm")) {
            duration = 0; // Permanent ban
        } else {
            duration = DurationParser.parseDuration(durationStr);
            if (duration == -1) {
                duration = 0; // If parsing failed, treat as permanent
            }
        }

        // Create punishment object using proper constructor parameters
        Punishment punishment = new Punishment(
            targetName,         // player
            sender.getName(),   // punisher
            reason,             // reason
            "BAN",              // action
            duration            // duration
        );

        // Ban the player using Bukkit's ban system
        Date expiry = duration > 0 ? new Date(System.currentTimeMillis() + duration) : null;
        Bukkit.getBanList(VersionHelper.getNameBanListType()).addBan(targetName, reason, expiry, sender.getName());

        // Kick if online
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.kickPlayer(ChatColor.RED + "You have been banned\nReason: " + reason + 
                            (duration > 0 ? "\nDuration: " + formatDuration(duration) : "\nThis ban is permanent"));
        }

        // Create a map of placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("staff_name", sender.getName());
        placeholders.put("player_name", targetName);
        placeholders.put("reason", reason);
        placeholders.put("duration", duration > 0 ? formatDuration(duration) : "Permanent");

        // Format and send the broadcast message
        String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("ban", placeholders);
        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            // Fallback message if config fails
            String fallbackMsg = ChatColor.RED + "------------------------\n" +
                "Action - BAN\n" +
                "Staff Member - " + sender.getName() + "\n" +
                "Punished Player - " + targetName + "\n" +
                "Reason - " + reason + "\n" +
                "Duration - " + (duration > 0 ? formatDuration(duration) : "Permanent") + "\n" +
                "------------------------";
            Bukkit.broadcastMessage(fallbackMsg);
        }

        // Send to Discord using new webhook system
        if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            plugin.getDiscordIntegration().sendPunishmentMessage(punishment);
        }

        // Save to database
        plugin.getFileManager().savePunishment(punishment);

        sender.sendMessage(ChatColor.GREEN + "Successfully banned " + targetName + " for: " + reason);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Complete player names
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Complete durations
            return DurationParser.getTabCompletions(args[1]);
        } else if (args.length == 3) {
            // Complete common reasons
            List<String> reasons = Arrays.asList(
                "Griefing",
                "Hacking/Cheating", 
                "Inappropriate behavior",
                "Spam",
                "Harassment",
                "Rule violation",
                "Toxicity",
                "Exploiting"
            );
            
            String partial = args[2].toLowerCase();
            return reasons.stream()
                    .filter(reason -> reason.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }

    private String formatDuration(long duration) {
        if (duration <= 0) return "Permanent";
        
        // Handle very large durations (likely data corruption)
        if (duration > 31536000000000L) { // More than 1000 years in milliseconds
            return "Permanent";
        }
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        
        if (weeks > 0) return weeks + "w";
        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
}
