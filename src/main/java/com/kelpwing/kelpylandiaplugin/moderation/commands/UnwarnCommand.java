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

public class UnwarnCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public UnwarnCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qol.mod.unwarn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unwarn <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason given";

        // Try to remove a warning
        if (plugin.getFileManager().removeLatestWarning(targetName)) {
            // Create punishment object using proper constructor parameters
            Punishment punishment = new Punishment(
                targetName,                 // player
                sender.getName(),           // punisher
                reason,                     // reason
                "UNWARN",                   // action
                0                           // duration
            );

            // Create a map of placeholders
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("staff_name", sender.getName());
            placeholders.put("player_name", targetName);
            placeholders.put("reason", reason);

            // Format and send the broadcast message
            String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("unwarn", placeholders);
            if (broadcastMsg != null) {
                Bukkit.broadcastMessage(broadcastMsg);
            } else {
                // Fallback message if config fails
                String fallbackMsg = ChatColor.GREEN + "------------------------\n" +
                    "Action - UNWARN\n" +
                    "Staff Member - " + sender.getName() + "\n" +
                    "Player - " + targetName + "\n" +
                    "Reason - " + reason + "\n" +
                    "------------------------";
                Bukkit.broadcastMessage(fallbackMsg);
            }

            // Send to Discord using new webhook system
            if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
                plugin.getDiscordIntegration().sendPunishmentMessage(punishment);
            }

            // Notify target if online
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "One of your warnings has been removed by " + sender.getName());
            }

            sender.sendMessage(ChatColor.GREEN + "Successfully removed a warning from " + targetName);
        } else {
            sender.sendMessage(ChatColor.RED + "No warnings found for this player!");
        }

        return true;
    }
}
