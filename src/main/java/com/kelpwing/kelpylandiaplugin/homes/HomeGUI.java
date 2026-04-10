package com.kelpwing.kelpylandiaplugin.homes;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.teleport.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Interactive inventory GUI for browsing and teleporting to homes.
 * - Click a home item to teleport
 * - Shift+click to delete
 * - Pagination for players with many homes
 */
public class HomeGUI implements Listener {

    private final KelpylandiaPlugin plugin;

    // Title prefix used to identify our GUI inventories
    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Homes";

    // Slots per page (rows 1-5, row 6 is navigation)
    private static final int HOMES_PER_PAGE = 45;

    // Track which page each player is on
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    // Track which player is viewing homes of which UUID (supports viewing own homes)
    private final Map<UUID, UUID> viewingHomes = new HashMap<>();

    public HomeGUI(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the homes GUI for a player (viewing their own homes).
     */
    public void openGUI(Player player) {
        openGUI(player, player.getUniqueId(), 0);
    }

    /**
     * Open the homes GUI at a specific page.
     */
    public void openGUI(Player player, UUID homeOwner, int page) {
        HomeManager homeManager = plugin.getHomeManager();
        List<Home> homes = homeManager.getHomeList(homeOwner);

        int totalPages = Math.max(1, (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = GUI_TITLE_PREFIX + ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Populate home items for this page
        int startIndex = page * HOMES_PER_PAGE;
        int endIndex = Math.min(startIndex + HOMES_PER_PAGE, homes.size());

        for (int i = startIndex; i < endIndex; i++) {
            Home home = homes.get(i);
            gui.setItem(i - startIndex, createHomeItem(home));
        }

        // Fill bottom row with glass panes (navigation bar)
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            gui.setItem(slot, filler);
        }

        // Previous page button (slot 45)
        if (page > 0) {
            gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "← Previous Page"));
        }

        // Info item (slot 49 - center)
        ItemStack info = createItem(Material.BOOK, ChatColor.GREEN + "Home Info");
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "You have " + ChatColor.WHITE + homes.size() + ChatColor.GRAY + " home(s)");
            lore.add(ChatColor.GRAY + "Max: " + ChatColor.WHITE + homeManager.getMaxHomes(player));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click" + ChatColor.GRAY + " a home to teleport");
            lore.add(ChatColor.RED + "Shift+Click" + ChatColor.GRAY + " to delete");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(49, info);

        // Next page button (slot 53)
        if (page < totalPages - 1) {
            gui.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page →"));
        }

        // Track state
        playerPages.put(player.getUniqueId(), page);
        viewingHomes.put(player.getUniqueId(), homeOwner);

        player.openInventory(gui);
    }

    /**
     * Create an item representing a home.
     */
    private ItemStack createHomeItem(Home home) {
        Material material;
        try {
            material = Material.valueOf(home.getIcon().toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.OAK_SIGN;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + home.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName());
            lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE
                + String.format("%.1f, %.1f, %.1f", home.getX(), home.getY(), home.getZ()));

            if (home.getDescription() != null && !home.getDescription().isEmpty()) {
                lore.add("");
                lore.add(ChatColor.AQUA + home.getDescription());
            }

            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to teleport");
            lore.add(ChatColor.RED + "Shift+Click to delete");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a simple named item.
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===================== Event Handlers =====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true); // Prevent item manipulation

        UUID viewerUUID = player.getUniqueId();
        UUID homeOwner = viewingHomes.get(viewerUUID);
        if (homeOwner == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Navigation buttons
        if (slot == 45) {
            // Previous page
            int currentPage = playerPages.getOrDefault(viewerUUID, 0);
            if (currentPage > 0) {
                openGUI(player, homeOwner, currentPage - 1);
            }
            return;
        }

        if (slot == 53) {
            // Next page
            int currentPage = playerPages.getOrDefault(viewerUUID, 0);
            HomeManager homeManager = plugin.getHomeManager();
            List<Home> homes = homeManager.getHomeList(homeOwner);
            int totalPages = Math.max(1, (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE));
            if (currentPage < totalPages - 1) {
                openGUI(player, homeOwner, currentPage + 1);
            }
            return;
        }

        // Bottom row (navigation) - ignore
        if (slot >= 45) return;

        // Home item clicked
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int page = playerPages.getOrDefault(viewerUUID, 0);
        int homeIndex = page * HOMES_PER_PAGE + slot;

        HomeManager homeManager = plugin.getHomeManager();
        List<Home> homes = homeManager.getHomeList(homeOwner);

        if (homeIndex >= homes.size()) return;

        Home home = homes.get(homeIndex);

        if (event.isShiftClick()) {
            // Shift+click = delete
            homeManager.deleteHome(homeOwner, home.getName());
            player.sendMessage(ChatColor.RED + "Home " + ChatColor.GOLD + home.getName() + ChatColor.RED + " has been deleted.");

            // Refresh GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                List<Home> updated = homeManager.getHomeList(homeOwner);
                if (updated.isEmpty()) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "You have no more homes.");
                } else {
                    openGUI(player, homeOwner, Math.min(page, Math.max(0, (int) Math.ceil((double) updated.size() / HOMES_PER_PAGE) - 1)));
                }
            }, 1L);
        } else {
            // Regular click = teleport
            Location loc = home.getLocation();
            if (loc == null) {
                player.sendMessage(ChatColor.RED + "Could not find world " + ChatColor.GOLD + home.getWorldName() + ChatColor.RED + "!");
                return;
            }

            // Close inventory first, then teleport next tick to avoid event conflicts
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                // Apply cooldown/invulnerability if teleport system is available
                TpaManager tpaManager = plugin.getTpaManager();
                if (tpaManager != null) {
                    if (tpaManager.isOnCooldown(player) && !player.hasPermission("qol.teleport.bypass.cooldown")) {
                        long remaining = tpaManager.getCooldownRemaining(player);
                        player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.GOLD + String.format("%.1f", remaining / 1000.0) + "s" + ChatColor.RED + " before teleporting again.");
                        return;
                    }
                    tpaManager.applyCooldown(player);
                    tpaManager.applyInvulnerability(player);
                }

                player.teleport(loc);
                player.sendMessage(ChatColor.GREEN + "Teleported to home " + ChatColor.GOLD + home.getName() + ChatColor.GREEN + "!");
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        playerPages.remove(uuid);
        viewingHomes.remove(uuid);
    }
}
