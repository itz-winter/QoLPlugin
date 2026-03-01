package com.kelpwing.kelpylandiaplugin.chat.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import com.kelpwing.kelpylandiaplugin.chat.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public PlayerListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getLogger().info("[KPAU] Received join message!");
        plugin.getLogger().info("[KPAU DEBUG] Chat PlayerListener.onPlayerJoin triggered for: " + event.getPlayer().getName());
        Player player = event.getPlayer();

        // Set player to default channel
        plugin.getChannelManager().setPlayerToDefaultChannel(player);
        
        // NOTE: Join message handling is done by PlayerEventListener to avoid conflicts
        // Log join message
        plugin.getLogger().info("Player " + player.getName() + " joined and was assigned to default channel");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLogger().info("[KPAU DEBUG] Chat PlayerListener.onPlayerQuit triggered for: " + event.getPlayer().getName());
        Player player = event.getPlayer();
        
        // Remove player from channel manager
        plugin.getChannelManager().removePlayerFromChannel(player);
        
        // NOTE: Quit message handling is done by PlayerEventListener to avoid conflicts
        // Log quit message
        plugin.getLogger().info("Player " + player.getName() + " left and was removed from channels");
    }
}
