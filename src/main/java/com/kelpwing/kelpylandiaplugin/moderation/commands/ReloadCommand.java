package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final KelpylandiaPlugin plugin;

    public ReloadCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("kelpylandia.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Check if command has no arguments
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /kpaumodreload");
            return true;
        }

        try {
            // Reload the plugin configuration
            plugin.reloadConfig();
            
            // Reload ConfigManager
            plugin.getConfigManager().reloadConfig();
            
            // Reinitialize Discord integration with new config
            if (plugin.getDiscordIntegration() != null) {
                plugin.getDiscordIntegration().disable();
                plugin.getDiscordIntegration().initialize();
            }
            
            // Reload channels from config
            if (plugin.getChannelManager() != null) {
                plugin.getChannelManager().reloadChannels();
                plugin.getLogger().info("Channels reloaded from config.");
            }
            
            // Send success message
            sender.sendMessage(ChatColor.GREEN + "KelpylandiaPlugin configuration has been reloaded successfully!");
            
        } catch (Exception e) {
            // Send error message if reload fails
            sender.sendMessage(ChatColor.RED + "Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
