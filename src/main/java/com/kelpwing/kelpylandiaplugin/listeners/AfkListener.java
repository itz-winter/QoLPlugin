package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.utils.AfkManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

/**
 * Resets AFK status on player activity (movement, chat, commands, interaction).
 * Also initialises activity tracking on join and cleans up on quit.
 */
public class AfkListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public AfkListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    private AfkManager afk() {
        return plugin.getAfkManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (afk() != null) {
            afk().recordActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (afk() != null) {
            afk().removePlayer(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only trigger on actual position change, not just looking around
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        if (afk() != null) {
            afk().recordActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (afk() != null) {
            afk().recordActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        // Don't reset AFK for the /afk command itself
        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equals("/afk")) return;
        if (afk() != null) {
            afk().recordActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (afk() != null && event.getPlayer() != null) {
            afk().recordActivity(event.getPlayer());
        }
    }
}
