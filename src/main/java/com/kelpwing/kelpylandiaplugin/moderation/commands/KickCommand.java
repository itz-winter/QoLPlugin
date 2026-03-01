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

public class KickCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public KickCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.kick")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /kick <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        // Create punishment object using proper constructor parameters
        Punishment punishment = new Punishment(
            target.getName(),   // player
            sender.getName(),   // punisher
            reason,             // reason
            "KICK",             // action
            0                   // duration
        );

        // Kick player
        target.kickPlayer(ChatColor.RED + "You have been kicked\nReason: " + reason);

        // Create a map of placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("staff_name", sender.getName());
        placeholders.put("player_name", target.getName());
        placeholders.put("reason", reason);
        placeholders.put("duration", "N/A");

        // Format and send the broadcast message
        String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("kick", placeholders);
        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            // Fallback message if config fails
            String fallbackMsg = ChatColor.RED + "------------------------\n" +
                "Action - KICK\n" +
                "Staff Member - " + sender.getName() + "\n" +
                "Punished Player - " + target.getName() + "\n" +
                "Reason - " + reason + "\n" +
                "Duration - N/A\n" +
                "------------------------";
            Bukkit.broadcastMessage(fallbackMsg);
        }

        // Send to Discord using new webhook system
        if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            plugin.getDiscordIntegration().sendPunishmentMessage(punishment);
        }

        // Save to database
        plugin.getFileManager().savePunishment(punishment);

        sender.sendMessage(ChatColor.GREEN + "Successfully kicked " + target.getName() + " for: " + reason);

        return true;
    }
}
