package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import com.kelpwing.kelpylandiaplugin.utils.DurationParser;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class IPBanCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public IPBanCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qol.mod.ipban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ipban <player> [duration] [reason]");
            return true;
        }

        String targetName = args[0];
        String durationStr = args.length > 1 ? args[1] : "permanent";
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : 
                       (args.length > 1 && !args[1].matches("\\d+[wdhms]?") && !args[1].equals("permanent")) ? 
                       String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        String ip = target.getAddress().getAddress().getHostAddress();
        
        // Parse duration - if "permanent" keywords, set to 0 (permanent)
        long duration;
        String lowerDuration = durationStr.toLowerCase();
        if (lowerDuration.equals("permanent") || lowerDuration.equals("perm")
            || lowerDuration.equals("inf") || lowerDuration.equals("infinite")) {
            duration = 0; // Permanent ban
        } else {
            duration = DurationParser.parseDuration(durationStr);
            if (duration == -1) {
                sender.sendMessage(ChatColor.RED + "Invalid duration format: " + durationStr);
                sender.sendMessage(ChatColor.YELLOW + "Duration formats: 1y2w3d4h5m (years/weeks/days/hours/minutes), inf/permanent");
                return true;
            }
        }

        // Create punishment object using proper constructor parameters
        Punishment punishment = new Punishment(
            target.getName(),   // player
            sender.getName(),   // punisher
            reason,             // reason
            "IPBAN",            // action
            duration            // duration
        );

        // Ban IP
        Date expiry = duration > 0 ? new Date(System.currentTimeMillis() + duration) : null;
        Bukkit.getBanList(BanList.Type.IP).addBan(ip, reason, expiry, sender.getName());

        target.kickPlayer(ChatColor.RED + "Your IP has been banned\nReason: " + reason);

        // Create a map of placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("staff_name", sender.getName());
        placeholders.put("player_name", target.getName());
        placeholders.put("reason", reason);
        placeholders.put("duration", duration > 0 ? formatDuration(duration) : "Permanent");

        // Format and send the broadcast message
        String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("ipban", placeholders);
        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            // Fallback message if config fails
            String fallbackMsg = ChatColor.RED + "------------------------\n" +
                "Action - IPBAN\n" +
                "Staff Member - " + sender.getName() + "\n" +
                "Punished Player - " + target.getName() + "\n" +
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
        plugin.getFileManager().saveIPBan(ip, target.getName(), reason, sender.getName(), duration);

        sender.sendMessage(ChatColor.GREEN + "Successfully IP banned " + target.getName() + " for: " + reason);

        return true;
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
