package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.utils.LevelManager;
import com.kelpwing.kelpylandiaplugin.utils.SpyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listens for player commands and relays them to CommandSpy users.
 * Also cleans up spy state on quit.
 */
public class SpyListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public SpyListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        SpyManager spyManager = plugin.getSpyManager();
        if (spyManager == null) return;

        Player player = event.getPlayer();
        String fullCommand = event.getMessage(); // includes the leading /

        for (UUID spyUUID : spyManager.getCommandSpies()) {
            if (spyUUID.equals(player.getUniqueId())) continue; // don't spy on yourself
            Player spy = Bukkit.getPlayer(spyUUID);
            if (spy != null && spy.isOnline()) {
                // Level check: spy must have >= level than the player being spied on
                if (!LevelManager.canObserve(spy, player, "commandspy")) continue;
                spy.sendMessage(ChatColor.DARK_GRAY + "[CS] " + ChatColor.GRAY + player.getName() + ": " + ChatColor.WHITE + fullCommand);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        SpyManager spyManager = plugin.getSpyManager();
        if (spyManager != null) {
            // Defer cleanup to next tick so saveState() (LOWEST) still sees
            // the spy flags during this event cycle
            final UUID uuid = event.getPlayer().getUniqueId();
            Bukkit.getScheduler().runTask(plugin, () -> spyManager.removePlayer(uuid));
        }
    }
}
