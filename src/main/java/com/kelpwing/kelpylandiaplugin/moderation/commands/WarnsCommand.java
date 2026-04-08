package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Warning;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WarnsCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public WarnsCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("qol.mod.warns")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /warns <player>");
            return true;
        }

        String targetName = args[0];
        
        // Clean up expired warnings first
        plugin.getFileManager().cleanupExpiredWarnings();
        
        // Get active warnings only
        List<Warning> activeWarnings = plugin.getFileManager().getActiveWarnings(targetName);

        sender.sendMessage(ChatColor.YELLOW + "------------------------");
        sender.sendMessage(ChatColor.GOLD + "Active Warnings for " + targetName + ":");
        
        if (activeWarnings.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No active warnings found.");
        } else {
            for (int i = 0; i < activeWarnings.size(); i++) {
                Warning warning = activeWarnings.get(i);
                long timeLeft = warning.getExpiration() - System.currentTimeMillis();
                String timeLeftStr = formatTimeLeft(timeLeft);
                
                sender.sendMessage(ChatColor.RED + "" + (i + 1) + ". " + warning.getReason());
                sender.sendMessage(ChatColor.GRAY + "   By: " + warning.getPunisher() + 
                                 " | Expires in: " + timeLeftStr);
            }
        }
        
        sender.sendMessage(ChatColor.YELLOW + "------------------------");
        return true;
    }

    private String formatTimeLeft(long timeLeft) {
        if (timeLeft <= 0) return "Expired";
        
        long days = timeLeft / (24L * 60L * 60L * 1000L);
        long hours = (timeLeft % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L);
        
        if (days > 0) {
            return days + " day(s), " + hours + " hour(s)";
        } else {
            return hours + " hour(s)";
        }
    }
}
