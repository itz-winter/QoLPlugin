package com.kelpwing.kelpylandiaplugin.chat;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import org.bukkit.entity.Player;

public class ChatUtils {
    
    public static String formatMessage(KelpylandiaPlugin plugin, Player player, Channel channel, String message) {
        String format = channel.getFormat();
        
        // Get player prefix and suffix from LuckPerms (if available)
        String prefix = getPlayerPrefix(player);
        String suffix = getPlayerSuffix(player);
        
        // Replace placeholders
        format = format.replace("{prefix}", prefix);
        format = format.replace("{suffix}", suffix);
        format = format.replace("{player}", player.getName());
        format = format.replace("{displayname}", player.getDisplayName());
        format = format.replace("{message}", message);
        format = format.replace("{channel}", channel.getDisplayName());
        
        // Apply color codes
        format = org.bukkit.ChatColor.translateAlternateColorCodes('&', format);
        
        return format;
    }
    
    private static String getPlayerPrefix(Player player) {
        // Try to get prefix from LuckPerms or other permission plugins
        try {
            // Check if PlaceholderAPI is available
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
                if (prefix != null && !prefix.equals("%luckperms_prefix%")) {
                    return prefix;
                }
            }
        } catch (Exception e) {
            // PlaceholderAPI not available or error occurred
        }
        
        return ""; // Default empty prefix
    }
    
    private static String getPlayerSuffix(Player player) {
        // Try to get suffix from LuckPerms or other permission plugins
        try {
            // Check if PlaceholderAPI is available
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String suffix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_suffix%");
                if (suffix != null && !suffix.equals("%luckperms_suffix%")) {
                    return suffix;
                }
            }
        } catch (Exception e) {
            // PlaceholderAPI not available or error occurred
        }
        
        return ""; // Default empty suffix
    }
    
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }
    
    public static boolean isPlayerMuted(Player player) {
        // Get the plugin instance to access mute data
        KelpylandiaPlugin plugin = KelpylandiaPlugin.getInstance();
        if (plugin == null) {
            return false;
        }
        
        // Check if player is in the muted players map
        Long muteExpiry = plugin.getMutedPlayers().get(player.getUniqueId());
        if (muteExpiry == null) {
            return false;
        }
        
        // Check if mute has expired
        long currentTime = System.currentTimeMillis();
        if (muteExpiry <= currentTime && muteExpiry > 0) {
            // Mute has expired, remove from map
            plugin.getMutedPlayers().remove(player.getUniqueId());
            return false;
        }
        
        // Player is still muted (either permanent mute if muteExpiry is -1, or hasn't expired yet)
        return true;
    }
}
