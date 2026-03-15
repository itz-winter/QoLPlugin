package com.kelpwing.kelpylandiaplugin.chat.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import com.kelpwing.kelpylandiaplugin.chat.ChatUtils;
import com.kelpwing.kelpylandiaplugin.utils.PlayerStateManager;
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
        Player player = event.getPlayer();

        // Set player to default channel
        plugin.getChannelManager().setPlayerToDefaultChannel(player);
        
        // Restore persisted toggle states (fly, god, vanish, spy, wt)
        PlayerStateManager stateManager = plugin.getPlayerStateManager();
        if (stateManager != null) {
            stateManager.restoreState(player);
        }
        
        plugin.getLogger().info("Player " + player.getName() + " joined and was assigned to default channel");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save toggle states before cleanup
        PlayerStateManager stateManager = plugin.getPlayerStateManager();
        if (stateManager != null) {
            stateManager.saveState(player);
        }
        
        // Remove player from channel manager
        plugin.getChannelManager().removePlayerFromChannel(player);
        
        plugin.getLogger().info("Player " + player.getName() + " left and was removed from channels");
    }
}
