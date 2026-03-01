package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class WarnCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public WarnCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.warn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /warn <player> <reason>");
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // Calculate expiration time (30 days from now)
        long warnExpiration = System.currentTimeMillis() + (30L * 24L * 60L * 60L * 1000L); // 30 days in milliseconds
        long durationMillis = 30L * 24L * 60L * 60L * 1000L; // 30 days in milliseconds

        // Create punishment object using proper constructor parameters
        Punishment punishment = new Punishment(
            targetName,         // player
            sender.getName(),   // punisher
            reason,             // reason
            "WARN",             // action
            durationMillis      // duration in milliseconds (consistent with other commands)
        );

        // Create a map of placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("staff_name", sender.getName());
        placeholders.put("player_name", targetName);
        placeholders.put("reason", reason);
        placeholders.put("duration", "30 days"); // Fixed duration for warnings

        // Format and send the broadcast message
        String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("warn", placeholders);
        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            // Fallback message if config fails
            String fallbackMsg = ChatColor.RED + "------------------------\n" +
                "Action - WARN\n" +
                "Staff Member - " + sender.getName() + "\n" +
                "Punished Player - " + targetName + "\n" +
                "Reason - " + reason + "\n" +
                "Duration - 30 days\n" +
                "------------------------";
            Bukkit.broadcastMessage(fallbackMsg);
        }

        // Send to Discord using new webhook system
        if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            plugin.getDiscordIntegration().sendPunishmentMessage(punishment);
        }

        // Save to database
        plugin.getFileManager().saveWarning(targetName, reason, sender.getName(), warnExpiration);

        // Notify target player if online
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been warned for: " + reason);
            target.sendMessage(ChatColor.YELLOW + "This warning will expire in 30 days.");
        }

        sender.sendMessage(ChatColor.GREEN + "Successfully warned " + targetName + " for: " + reason);

        return true;
    }
}
