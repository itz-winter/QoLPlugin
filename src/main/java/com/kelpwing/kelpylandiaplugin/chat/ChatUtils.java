package com.kelpwing.kelpylandiaplugin.chat;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.entity.Player;

public class ChatUtils {
    
    /**
     * Builds the fully-formatted chat prefix (everything before the message body).
     * Returns a legacy colour-coded string like "&2[G]&r [Owner] Rivulet&r: ".
     * <p>
     * The caller is responsible for appending the actual message content —
     * either as plain text or as JSON components (when [item] etc. are present).
     */
    public static String formatPrefix(KelpylandiaPlugin plugin, Player player, Channel channel) {
        String format = channel.getFormat();

        String prefix = getPlayerPrefix(player);
        String suffix = getPlayerSuffix(player);

        format = format.replace("{prefix}", prefix);
        format = format.replace("{suffix}", suffix);
        format = format.replace("{player}", player.getName());
        format = format.replace("{displayname}", player.getDisplayName());
        format = format.replace("{channel}", channel.getDisplayName());

        // Apply colour codes
        format = org.bukkit.ChatColor.translateAlternateColorCodes('&', format);

        // Split on {message} — we only want the prefix portion
        int msgIdx = format.indexOf("{message}");
        if (msgIdx >= 0) {
            return format.substring(0, msgIdx);
        }
        // If no {message} token at all, append ": " and return
        return format + ": ";
    }

    /**
     * Builds the complete chat line as a legacy-colour string (no JSON components).
     * Used for logging and as a simple fallback when no keywords are present.
     */
    public static String formatMessage(KelpylandiaPlugin plugin, Player player, Channel channel, String message) {
        String format = channel.getFormat();

        String prefix = getPlayerPrefix(player);
        String suffix = getPlayerSuffix(player);

        format = format.replace("{prefix}", prefix);
        format = format.replace("{suffix}", suffix);
        format = format.replace("{player}", player.getName());
        format = format.replace("{displayname}", player.getDisplayName());
        format = format.replace("{channel}", channel.getDisplayName());
        format = format.replace("{message}", message);

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
