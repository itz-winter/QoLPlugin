package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class UnbanCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public UnbanCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kelpylandia.mod.unban")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unban <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason given";

        // Check if player is banned in Bukkit's ban list
        if (Bukkit.getBanList(VersionHelper.getNameBanListType()).isBanned(targetName)) {
            // Remove from Bukkit ban list
            Bukkit.getBanList(VersionHelper.getNameBanListType()).pardon(targetName);

            // Try to remove from database (optional, as it's in punishment history)
            plugin.getFileManager().removeBan(targetName);

            // Create punishment object using proper constructor parameters
            Punishment punishment = new Punishment(
                targetName,         // player
                sender.getName(),   // punisher
                reason,             // reason
                "UNBAN",            // action
                0                   // duration
            );
            
            // Create a map of placeholders
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("staff_name", sender.getName());
            placeholders.put("player_name", targetName);
            placeholders.put("reason", reason);

            // Format and send the broadcast message
            String broadcastMsg = plugin.getConfigManager().formatBroadcastMessage("unban", placeholders);
            if (broadcastMsg != null) {
                Bukkit.broadcastMessage(broadcastMsg);
            } else {
                // Fallback message if config fails
                String fallbackMsg = ChatColor.GREEN + "------------------------\n" +
                        "Action - UNBAN\n" +
                        "Staff Member - " + sender.getName() + "\n" +
                        "Unbanned Player - " + targetName + "\n" +
                        "Reason - " + reason + "\n" +
                        "------------------------";
                Bukkit.broadcastMessage(fallbackMsg);
            }

            // Send to Discord using new webhook system
            if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
                plugin.getDiscordIntegration().sendPunishmentMessage(punishment);
            }
            
            sender.sendMessage(ChatColor.GREEN + "Successfully unbanned " + targetName);
        } else {
            sender.sendMessage(ChatColor.RED + "Player is not banned!");
        }

        return true;
    }
}
