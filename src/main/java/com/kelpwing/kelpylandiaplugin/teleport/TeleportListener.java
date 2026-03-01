package com.kelpwing.kelpylandiaplugin.teleport;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles post-teleport invulnerability (cancels damage) and
 * cleans up TPA data when a player quits.
 */
public class TeleportListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public TeleportListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancel damage for players who are currently invulnerable after teleporting.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        TpaManager tpaManager = plugin.getTpaManager();
        if (tpaManager == null) return;

        if (tpaManager.isInvulnerable(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Clean up all TPA data for a player when they quit.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TpaManager tpaManager = plugin.getTpaManager();
        if (tpaManager == null) return;

        tpaManager.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
