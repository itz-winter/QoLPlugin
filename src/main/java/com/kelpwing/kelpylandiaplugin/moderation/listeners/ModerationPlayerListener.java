package com.kelpwing.kelpylandiaplugin.moderation.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class ModerationPlayerListener implements Listener {
    private final KelpylandiaPlugin plugin;

    public ModerationPlayerListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        
        // Check if player is muted
        if (plugin.getMutedPlayers().containsKey(playerUUID)) {
            long muteExpiry = plugin.getMutedPlayers().get(playerUUID);
            
            // Check if mute has expired
            if (System.currentTimeMillis() >= muteExpiry) {
                plugin.getMutedPlayers().remove(playerUUID);
                event.getPlayer().sendMessage(ChatColor.GREEN + "Your mute has expired!");
            } else {
                event.setCancelled(true);
                long timeLeft = muteExpiry - System.currentTimeMillis();
                event.getPlayer().sendMessage(ChatColor.RED + "You are muted for " + formatTime(timeLeft));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        
        // Check if player's mute has expired
        if (plugin.getMutedPlayers().containsKey(playerUUID)) {
            long muteExpiry = plugin.getMutedPlayers().get(playerUUID);
            
            if (System.currentTimeMillis() >= muteExpiry) {
                plugin.getMutedPlayers().remove(playerUUID);
                event.getPlayer().sendMessage(ChatColor.GREEN + "Your mute has expired!");
            }
        }
    }

    private String formatTime(long timeLeft) {
        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " day(s)";
        if (hours > 0) return hours + " hour(s)";
        if (minutes > 0) return minutes + " minute(s)";
        return seconds + " second(s)";
    }
}
