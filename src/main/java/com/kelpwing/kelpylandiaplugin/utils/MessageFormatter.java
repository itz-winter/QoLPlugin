package com.kelpwing.kelpylandiaplugin.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageFormatter {
    private static Boolean placeholderAPILoaded = null;
    
    private static boolean isPlaceholderAPILoaded() {
        if (placeholderAPILoaded == null) {
            placeholderAPILoaded = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return placeholderAPILoaded;
    }
    
    public static String format(String message, Map<String, String> placeholders, Player player) {
        String formatted = message;
        
        // Replace custom placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Apply PlaceholderAPI if player is not null and PlaceholderAPI is loaded
        if (player != null && isPlaceholderAPILoaded()) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }
        
        // Translate color codes
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }
}
