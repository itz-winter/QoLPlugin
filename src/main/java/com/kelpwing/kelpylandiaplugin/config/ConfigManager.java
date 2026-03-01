package com.kelpwing.kelpylandiaplugin.config;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.utils.DiscordWebhook;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final KelpylandiaPlugin plugin;
    private DiscordWebhook discordWebhook;
    private final Map<String, String> broadcastFormats;
    private final Map<String, ChatColor> colors;
    private boolean discordEnabled;
    private boolean broadcastEnabled;
    private String discordBotToken;
    private String discordChannelId;
    private String discordMessageFormat;
    
    public ConfigManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.broadcastFormats = new HashMap<>();
        this.colors = new HashMap<>();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        FileConfiguration config = plugin.getConfig();
        
        // Set default channel values if they don't exist
        if (!config.contains("channels")) {
            createDefaultChannels(config);
        }
        
        // Load moderation settings
        loadModerationConfig(config);
        
        // Ensure Discord format settings exist
        if (!config.contains("discord.format.minecraft-to-discord")) {
            config.set("discord.format.minecraft-to-discord", "**{prefix}{player}**: {message}");
        }
        
        if (!config.contains("discord.format.discord-to-minecraft")) {
            config.set("discord.format.discord-to-minecraft", "&9[Discord] &7[{channel}] &b{user}&7: {message}");
        }
        
        plugin.saveConfig();
    }
    
    private void createDefaultChannels(FileConfiguration config) {
        // Global channel
        config.set("channels.global.display-name", "Global");
        config.set("channels.global.format", "&2[G]&r {prefix}{player}&r: {message}");
        config.set("channels.global.permission", "kelpylandia.channel.global");
        config.set("channels.global.proximity", false);
        config.set("channels.global.proximity-distance", 0.0);
        config.set("channels.global.discord-enabled", true);
        config.set("channels.global.discord-channel", "0000000000000000000");
        config.set("channels.global.default", true);
        
        // Local channel
        config.set("channels.local.display-name", "Local");
        config.set("channels.local.format", "&e[L]&r {prefix}{player}&r: {message}");
        config.set("channels.local.permission", "kelpylandia.channel.local");
        config.set("channels.local.proximity", true);
        config.set("channels.local.proximity-distance", 50.0);
        config.set("channels.local.discord-enabled", false);
        config.set("channels.local.discord-channel", null);
        config.set("channels.local.default", false);
        
        // Admin channel
        config.set("channels.admin.display-name", "Admin");
        config.set("channels.admin.format", "&c[A]&r {prefix}{player}&r: {message}");
        config.set("channels.admin.permission", "kelpylandia.channel.admin");
        config.set("channels.admin.proximity", false);
        config.set("channels.admin.proximity-distance", 0.0);
        config.set("channels.admin.discord-enabled", true);
        config.set("channels.admin.discord-channel", "0000000000000000000");
        config.set("channels.admin.default", false);
    }
    
    private void loadModerationConfig(FileConfiguration config) {
        // Load Discord settings
        discordEnabled = config.getBoolean("discord.enabled", false);
        discordBotToken = config.getString("discord.bot-token", "");
        discordChannelId = config.getString("discord.moderation-channel-id", "");
        discordMessageFormat = config.getString("discord.format.moderation-format", 
            "**Action:** {action}\n**Staff:** {staff_name}\n**Player:** {player_name}\n**Reason:** {reason}\n**Duration:** {duration}\n**Server:** {server_name}");

        // Load broadcast settings
        broadcastEnabled = config.getBoolean("broadcast.enabled", true);
        
        // Load broadcast formats
        loadBroadcastFormats(config);

        // Load colors
        loadColors(config);
        
        // Initialize Discord webhook if enabled
        if (discordEnabled) {
            this.discordWebhook = new DiscordWebhook(plugin);
        }
    }
    
    private void loadBroadcastFormats(FileConfiguration config) {
        broadcastFormats.put("ban", config.getString("broadcast.formats.ban", 
            "&c------------------------\n&cAction - BAN\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - {duration}\n&c------------------------"));
        broadcastFormats.put("kick", config.getString("broadcast.formats.kick", 
            "&c------------------------\n&cAction - KICK\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - N/A\n&c------------------------"));
        broadcastFormats.put("mute", config.getString("broadcast.formats.mute", 
            "&c------------------------\n&cAction - MUTE\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - {duration}\n&c------------------------"));
        broadcastFormats.put("warn", config.getString("broadcast.formats.warn", 
            "&c------------------------\n&cAction - WARN\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - 30 days\n&c------------------------"));
        broadcastFormats.put("ipban", config.getString("broadcast.formats.ipban", 
            "&c------------------------\n&cAction - IPBAN\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - {duration}\n&c------------------------"));
        broadcastFormats.put("unban", config.getString("broadcast.formats.unban", 
            "&a------------------------\n&aAction - UNBAN\n&aStaff Member - {staff_name}\n&aUnbanned Player - {player_name}\n&aReason - {reason}\n&aDuration - N/A\n&a------------------------"));
        broadcastFormats.put("unmute", config.getString("broadcast.formats.unmute", 
            "&a------------------------\n&aAction - UNMUTE\n&aStaff Member - {staff_name}\n&aUnmuted Player - {player_name}\n&aReason - {reason}\n&aDuration - N/A\n&a------------------------"));
        broadcastFormats.put("unwarn", config.getString("broadcast.formats.unwarn", 
            "&a------------------------\n&aAction - UNWARN\n&aStaff Member - {staff_name}\n&aPlayer - {player_name}\n&aReason - {reason}\n&aDuration - N/A\n&a------------------------"));
    }

    private void loadColors(FileConfiguration config) {
        if (config.getConfigurationSection("colors") != null) {
            for (String colorName : config.getConfigurationSection("colors").getKeys(false)) {
                String colorValue = config.getString("colors." + colorName);
                try {
                    ChatColor color = ChatColor.valueOf(colorValue.toUpperCase());
                    colors.put(colorName, color);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid color: " + colorValue);
                    colors.put(colorName, ChatColor.WHITE);
                }
            }
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    public String formatBroadcastMessage(String action, Map<String, String> placeholders) {
        if (!broadcastEnabled) return null;
        
        String format = broadcastFormats.get(action.toLowerCase());
        if (format == null) return null;
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            format = format.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return ChatColor.translateAlternateColorCodes('&', format);
    }

    public String formatDiscordMessage(Map<String, String> placeholders) {
        String format = discordMessageFormat;
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            format = format.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return format;
    }

    // Getters
    public boolean isDiscordEnabled() {
        return discordEnabled;
    }

    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }

    public String getDiscordBotToken() {
        return discordBotToken;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public String getDiscordMessageFormat() {
        return discordMessageFormat;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public ChatColor getColor(String colorName) {
        return colors.getOrDefault(colorName, ChatColor.WHITE);
    }
}
