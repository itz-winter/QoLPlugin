package com.kelpwing.kelpylandiaplugin.chat.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Prevents players from taking items out of snapshot inventories opened via
 * the [item], [inv], or [enderchest] chat components.
 * <p>
 * Snapshot inventories are identified by their title prefix
 * ({@code §3} = {@link ChatColor#DARK_AQUA}).
 */
public class SnapshotListener implements Listener {

    private static final String SNAPSHOT_TITLE_PREFIX = ChatColor.DARK_AQUA.toString();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(SNAPSHOT_TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith(SNAPSHOT_TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }
}
