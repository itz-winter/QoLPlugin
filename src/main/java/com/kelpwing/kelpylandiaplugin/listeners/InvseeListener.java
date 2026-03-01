package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.commands.InvseeCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class InvseeListener implements Listener {

    private final KelpylandiaPlugin plugin;
    private final InvseeCommand invseeCommand;

    public InvseeListener(KelpylandiaPlugin plugin, InvseeCommand invseeCommand) {
        this.plugin = plugin;
        this.invseeCommand = invseeCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player admin = (Player) event.getWhoClicked();
        
        // Check if this player is viewing someone's inventory via invsee
        if (!invseeCommand.isViewingInventory(admin)) return;
        
        // Get inventory title to determine if it's an invsee inventory
        String title = event.getView().getTitle();
        if (!title.contains("'s Inventory") && !title.contains("'s Armor") && !title.contains("'s Ender")) {
            return;
        }
        
        // Prevent clicking on barrier blocks (placeholder items)
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
            event.setCancelled(true);
            admin.sendMessage(ChatColor.RED + "You cannot modify this slot!");
            return;
        }
        
        // Handle the inventory modification
        try {
            // Schedule the update to happen after the click event is processed
            invseeCommand.handleInventoryClick(admin, event.getInventory(), event.getSlot(), 
                event.getCurrentItem(), event.getCursor());
                
        } catch (Exception e) {
            admin.sendMessage(ChatColor.RED + "An error occurred while modifying the inventory!");
            plugin.getLogger().warning("Error in invsee inventory click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player admin = (Player) event.getPlayer();
        
        // Check if this player was viewing someone's inventory via invsee
        if (invseeCommand.isViewingInventory(admin)) {
            invseeCommand.handleInventoryClose(admin);
            admin.sendMessage(ChatColor.YELLOW + "Closed inventory view.");
        }
    }
}
