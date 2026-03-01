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
import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public UnmuteCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.unmute")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unmute <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason given";
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        // Check if player is muted
        if (!plugin.getMutedPlayers().containsKey(targetUUID)) {
            sender.sendMessage(ChatColor.RED + "Player is not muted!");
            return true;
        }

        // Create punishment object using proper constructor parameters
        Punishment punishment = new Punishment(
            targetName,         // player
            sender.getName(),   // punisher
            reason,             // reason
            "UNMUTE",           // action
            0                   // duration
        );

        // Remove from muted players
        plugin.getMutedPlayers().remove(targetUUID);

        // Create a map of placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("staff_name", sender.getName());
        placeholders.put("player_name", targetName);
        placeholders.put("reason", reason);

        // Format and send the broadcast message
        String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("unmute", placeholders);
        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg);
        } else {
            // Fallback message if config fails
            String fallbackMsg = ChatColor.GREEN + "------------------------\n" +
                "Action - UNMUTE\n" +
                "Staff Member - " + sender.getName() + "\n" +
                "Unmuted Player - " + targetName + "\n" +
                "Reason - " + reason + "\n" +
                "------------------------";
            Bukkit.broadcastMessage(fallbackMsg);
        }

        // Send to Discord using new webhook system
        if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            plugin.getDiscordIntegration().sendPunishmentMessage(punishment);
        }

        // Notify target if online
        target.sendMessage(ChatColor.GREEN + "You have been unmuted by " + sender.getName());

        sender.sendMessage(ChatColor.GREEN + "Successfully unmuted " + targetName);

        return true;
    }
}
