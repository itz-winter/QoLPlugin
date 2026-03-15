package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for virtual workbench inventories opened by /anvil, /grindstone, etc.
 * 
 * Handles:
 * - Returning items to the player when a virtual workbench is closed (prevents item deletion)
 * - Applying color codes to anvil rename text for preview
 */
public class WorkbenchListener implements Listener {

    private final KelpylandiaPlugin plugin;

    /**
     * Set of player UUIDs that currently have a virtual workbench open.
     * Used to distinguish virtual (command-opened) inventories from real block inventories.
     */
    private final Set<UUID> virtualWorkbenchUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Set of virtual workbench InventoryTypes that we manage.
     * Crafting table and ender chest are handled natively so they are NOT in this set.
     */
    private static final Set<InventoryType> VIRTUAL_TYPES = Set.of(
            InventoryType.ANVIL,
            InventoryType.GRINDSTONE,
            InventoryType.STONECUTTER,
            InventoryType.LOOM,
            InventoryType.CARTOGRAPHY
    );

    public WorkbenchListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Mark a player as having a virtual workbench open.
     * Call this from WorkbenchCommand right before opening the inventory.
     */
    public void markVirtualOpen(UUID playerId) {
        virtualWorkbenchUsers.add(playerId);
    }

    /**
     * When a virtual workbench inventory is closed, return all items to the player.
     * This prevents the item-deletion bug caused by Bukkit.createInventory() for
     * non-functional workbench types.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        if (!virtualWorkbenchUsers.remove(uuid)) return; // Not a virtual workbench

        Inventory inv = event.getInventory();
        InventoryType type = inv.getType();

        // Also handle SMITHING if it exists on this server version
        boolean isVirtual = VIRTUAL_TYPES.contains(type) || type.name().equals("SMITHING");
        if (!isVirtual) return;

        // Return all non-null, non-air items to the player
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                // Try to add to inventory, drop on ground if full
                for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inv.setItem(i, null);
            }
        }
    }

    /**
     * Apply color codes to anvil rename text so players can preview colored names.
     * Works for both real and virtual anvils.
     * Only if the player has the kelpylandia.anvil.color permission.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("workbenches.anvil-color", true)) return;

        if (event.getViewers().isEmpty()) return;
        if (!(event.getViewers().get(0) instanceof Player player)) return;
        if (!player.hasPermission("kelpylandia.anvil.color")) return;

        ItemStack result = event.getResult();
        if (result == null) return;

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;
        if (!meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();
        // Only process if it contains & (color code indicator)
        if (!displayName.contains("&")) return;

        String colored = ChatColor.translateAlternateColorCodes('&', displayName);
        meta.setDisplayName(colored);
        result.setItemMeta(meta);
        event.setResult(result);
    }
}
