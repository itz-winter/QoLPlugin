package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import com.kelpwing.kelpylandiaplugin.utils.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuteCommand implements CommandExecutor, TabCompleter {
    private final KelpylandiaPlugin plugin;

    public MuteCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.mute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mute <player> <duration> [reason]");
            sender.sendMessage(ChatColor.YELLOW + DurationParser.getExamples());
            return true;
        }

        String targetName = args[0];
        String durationStr = args[1];
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        // Parse duration
        long duration;
        String lowerDuration = durationStr.toLowerCase();
        if (lowerDuration.equals("permanent") || lowerDuration.equals("perm")
            || lowerDuration.equals("inf") || lowerDuration.equals("infinite")) {
            duration = 0; // Permanent mute
        } else {
            duration = DurationParser.parseDuration(durationStr);
            if (duration == -1) {
                sender.sendMessage(ChatColor.RED + "Invalid duration format!");
                sender.sendMessage(ChatColor.YELLOW + DurationParser.getExamples());
                return true;
            }
        }

        // Check if already muted
        if (plugin.getMutedPlayers().containsKey(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Player is already muted!");
            return true;
        }

        // Add player to muted list (permanent = Long.MAX_VALUE so it never expires)
        long muteExpiry = duration > 0 ? System.currentTimeMillis() + duration : Long.MAX_VALUE;
        plugin.getMutedPlayers().put(target.getUniqueId(), muteExpiry);

        // Create punishment object using proper constructor parameters
        Punishment punishment = new Punishment(
            target.getName(),    // player
            sender.getName(),    // punisher  
            reason,              // reason
            "MUTE",             // action
            duration            // duration
        );

        // Notify target
        target.sendMessage(ChatColor.RED + "You have been muted for: " + reason);
        if (duration > 0) {
            target.sendMessage(ChatColor.YELLOW + "Duration: " + formatDuration(duration));
        } else {
            target.sendMessage(ChatColor.YELLOW + "This mute is permanent.");
        }

        // Create a map of placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("staff_name", sender.getName());
        placeholders.put("player_name", target.getName());
        placeholders.put("reason", reason);
        placeholders.put("duration", duration > 0 ? formatDuration(duration) : "Permanent");

        // Format and send the broadcast message
        String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("mute", placeholders);
        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            // Fallback message if config fails
            String fallbackMsg = ChatColor.RED + "------------------------\n" +
                "Action - MUTE\n" +
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
        plugin.getFileManager().savePunishment(punishment);

        sender.sendMessage(ChatColor.GREEN + "Successfully muted " + target.getName() + " for: " + reason);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete player names
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // Tab complete duration
            String partial = args[1];
            completions.addAll(DurationParser.getTabCompletions(partial));
        } else if (args.length == 3) {
            // Tab complete common reasons
            String partial = args[2].toLowerCase();
            String[] commonReasons = {
                "Spam", "Toxicity", "Inappropriate language", "Harassment", 
                "Advertising", "Trolling", "Rule violation", "Disrespect"
            };
            
            for (String reason : commonReasons) {
                if (reason.toLowerCase().startsWith(partial)) {
                    completions.add(reason);
                }
            }
        }
        
        return completions;
    }

    private String formatDuration(long duration) {
        if (duration <= 0) return "Permanent";
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
}
