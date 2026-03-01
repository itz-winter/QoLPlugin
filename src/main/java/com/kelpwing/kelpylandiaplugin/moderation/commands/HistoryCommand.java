package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HistoryCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.history")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /hist <player>");
            return true;
        }

        String targetName = args[0];
        
        // Get player history
        List<Map<String, String>> history = plugin.getFileManager().getPlayerHistory(targetName);

        sender.sendMessage(ChatColor.YELLOW + "========================");
        sender.sendMessage(ChatColor.GOLD + "Punishment History for " + targetName + ":");
        
        if (history.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No punishment history found.");
        } else {
            int count = 1;
            for (Map<String, String> record : history) {
                String action = record.get("action");
                String reason = record.get("reason");
                String punisher = record.get("punisher");
                String duration = record.get("duration");
                long timestamp = Long.parseLong(record.get("timestamp"));
                
                String date = dateFormat.format(new Date(timestamp));
                String durationText = formatDuration(Long.parseLong(duration));
                
                // Color code based on action
                ChatColor actionColor = getActionColor(action);
                
                sender.sendMessage(actionColor + "" + count + ". " + action);
                sender.sendMessage(ChatColor.GRAY + "   Reason: " + reason);
                sender.sendMessage(ChatColor.GRAY + "   Staff: " + punisher);
                sender.sendMessage(ChatColor.GRAY + "   Duration: " + durationText);
                sender.sendMessage(ChatColor.GRAY + "   Date: " + date);
                
                count++;
                
                // Limit to 10 most recent entries to avoid spam
                if (count > 10) {
                    sender.sendMessage(ChatColor.YELLOW + "... and " + (history.size() - 10) + " more entries");
                    break;
                }
            }
        }
        
        sender.sendMessage(ChatColor.YELLOW + "========================");
        return true;
    }

    private String formatDuration(long duration) {
        if (duration <= 0) return "Permanent";
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " day(s)";
        if (hours > 0) return hours + " hour(s)";
        if (minutes > 0) return minutes + " minute(s)";
        return seconds + " second(s)";
    }

    private ChatColor getActionColor(String action) {
        switch (action.toUpperCase()) {
            case "BAN":
            case "IPBAN":
                return ChatColor.DARK_RED;
            case "KICK":
            case "MUTE":
                return ChatColor.RED;
            case "WARN":
                return ChatColor.YELLOW;
            case "UNBAN":
            case "UNMUTE":
            case "UNWARN":
                return ChatColor.GREEN;
            default:
                return ChatColor.WHITE;
        }
    }
}
