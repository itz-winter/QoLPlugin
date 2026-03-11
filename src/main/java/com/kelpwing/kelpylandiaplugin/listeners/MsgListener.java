package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.commands.MsgCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up messaging data (last conversation, toggles) when a player quits.
 */
public class MsgListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public MsgListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        MsgCommand msgCmd = plugin.getMsgCommand();
        if (msgCmd != null) {
            msgCmd.removePlayer(event.getPlayer().getUniqueId());
        }
    }
}
