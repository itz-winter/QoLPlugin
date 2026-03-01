package com.kelpwing.kelpylandiaplugin.integrations;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
    
    private final KelpylandiaPlugin plugin;
    
    public PlaceholderAPIIntegration(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "multichat";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        
        switch (params.toLowerCase()) {
            case "channel":
                return plugin.getChannelManager().getPlayerChannel(player.getUniqueId());
                
            case "channel_display":
                String channelName = plugin.getChannelManager().getPlayerChannel(player.getUniqueId());
                Channel channel = plugin.getChannelManager().getChannel(channelName);
                return channel != null ? channel.getDisplayName() : channelName;
                
            case "channel_count":
                return String.valueOf(plugin.getChannelManager().getChannels().size());
                
            case "muted_channels":
                return String.valueOf(plugin.getChannelManager().getMutedChannels(player.getUniqueId()).size());
                
            default:
                if (params.startsWith("channel_permission_")) {
                    String checkChannel = params.substring("channel_permission_".length());
                    return plugin.getChannelManager().hasPermission(player, checkChannel) ? "true" : "false";
                }
                
                if (params.startsWith("channel_muted_")) {
                    String checkChannel = params.substring("channel_muted_".length());
                    return plugin.getChannelManager().isChannelMuted(player.getUniqueId(), checkChannel) ? "true" : "false";
                }
                
                return null;
        }
    }
    
    public String setPlaceholders(Player player, String text) {
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            return text;
        }
    }
    
    public boolean isEnabled() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
