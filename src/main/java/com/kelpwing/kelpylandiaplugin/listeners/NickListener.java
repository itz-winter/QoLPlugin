package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.utils.NickManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Re-applies a saved nickname when a player joins the server.
 */
public class NickListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public NickListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        NickManager nickManager = plugin.getNickManager();
        if (nickManager != null) {
            nickManager.applyNickname(event.getPlayer());
        }
    }
}
