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
import java.util.stream.Collectors;

/**
 * Interactive inventory GUI for browsing and teleporting to homes.
 * - Click a home item to teleport
 * - Right-click a home item to open the icon picker
 * - Shift+click to delete (shows confirmation GUI)
 * - Pagination for players with many homes
 */
public class HomeGUI implements Listener {

    private final KelpylandiaPlugin plugin;

    // Title prefix used to identify our GUI inventories
    private static final String GUI_TITLE_PREFIX    = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Homes";
    private static final String CONFIRM_TITLE_PREFIX = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Delete Home: ";
    private static final String ICON_TITLE_PREFIX   = ChatColor.GOLD + "" + ChatColor.BOLD + "Choose Icon: ";

    // Slots per page (rows 1-5, row 6 is navigation)
    private static final int HOMES_PER_PAGE = 45;
    private static final int ICON_PICKER_SIZE = 54;

    // Common icon palette â€” shown in the icon picker.
    // We use 44 slots (rows 1-4 + partial row 5) leaving row 6 for navigation.
    private static final List<Material> ICON_PALETTE = buildPalette();

    // Track which page each player is on
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    // Track which player is viewing homes of which UUID (supports viewing own homes)
    private final Map<UUID, UUID> viewingHomes = new HashMap<>();

    // Track players currently in a delete-confirmation GUI: viewer -> home name pending deletion
    private final Map<UUID, String> pendingDelete = new HashMap<>();

    // Track players currently in the icon picker: viewer -> home name being icon-changed
    private final Map<UUID, String> pendingIcon = new HashMap<>();

    public HomeGUI(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Icon palette builder
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private static List<Material> buildPalette() {
        Material[] palette = {
            // Nature / terrain
            Material.GRASS_BLOCK, Material.DIRT, Material.SAND, Material.GRAVEL,
            Material.STONE, Material.COBBLESTONE, Material.NETHERRACK, Material.END_STONE,
            // Wood
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            // Planks
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS,
            // Wool colours
            Material.WHITE_WOOL, Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL,
            Material.BLUE_WOOL, Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL,
            Material.BROWN_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL, Material.BLACK_WOOL,
            // Ores / gems
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
            Material.COAL, Material.QUARTZ,
            Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.GOLD_ORE, Material.IRON_ORE,
            // Utility
            Material.CHEST, Material.CRAFTING_TABLE, Material.FURNACE, Material.ENCHANTING_TABLE,
            Material.BOOKSHELF, Material.JUKEBOX, Material.BEACON,
            // Decoration
            Material.FLOWER_POT, Material.LANTERN, Material.TORCH, Material.SEA_LANTERN,
            Material.GLOWSTONE, Material.JACK_O_LANTERN,
            // Signs + misc
            Material.OAK_SIGN, Material.PAINTING, Material.ITEM_FRAME,
            Material.NETHER_STAR, Material.ENDER_EYE, Material.COMPASS, Material.MAP,
            Material.CLOCK, Material.FILLED_MAP,
            // Beds
            Material.RED_BED, Material.BLUE_BED, Material.GREEN_BED, Material.YELLOW_BED,
        };
        return Arrays.stream(palette)
                .filter(m -> m.isItem() && !m.isAir())
                .distinct()
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Open methods
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /** Open the homes GUI for a player (viewing their own homes). */
    public void openGUI(Player player) {
        openGUI(player, player.getUniqueId(), 0);
    }

    /** Open the homes GUI at a specific page. */
    public void openGUI(Player player, UUID homeOwner, int page) {
        HomeManager homeManager = plugin.getHomeManager();
        List<Home> homes = homeManager.getHomeList(homeOwner);

        int totalPages = Math.max(1, (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = GUI_TITLE_PREFIX + ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        int startIndex = page * HOMES_PER_PAGE;
        int endIndex = Math.min(startIndex + HOMES_PER_PAGE, homes.size());

        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(i - startIndex, createHomeItem(homes.get(i)));
        }

        // Fill bottom row with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            gui.setItem(slot, filler);
        }

        if (page > 0) {
            gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "â† Previous Page"));
        }

        // Info item (slot 49)
        ItemStack info = createItem(Material.BOOK, ChatColor.GREEN + "Home Info");
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "You have " + ChatColor.WHITE + homes.size() + ChatColor.GRAY + " home(s)");
            lore.add(ChatColor.GRAY + "Max: " + ChatColor.WHITE + homeManager.getMaxHomes(player));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click" + ChatColor.GRAY + " to teleport");
            lore.add(ChatColor.AQUA + "Right-Click" + ChatColor.GRAY + " to change icon");
            lore.add(ChatColor.RED + "Shift+Click" + ChatColor.GRAY + " to delete");
            lore.add(ChatColor.GRAY + "Tip: " + ChatColor.WHITE + "/renhome <old> <new> to rename");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(49, info);

        if (page < totalPages - 1) {
            gui.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page â†’"));
        }

        playerPages.put(player.getUniqueId(), page);
        viewingHomes.put(player.getUniqueId(), homeOwner);

        player.openInventory(gui);
    }

    /** Open the icon picker for a specific home. */
    private void openIconPickerGUI(Player player, UUID homeOwner, String homeName, int pickerPage) {
        int slotsPerPage = 45; // rows 1-5; row 6 = nav
        int totalPages = Math.max(1, (int) Math.ceil((double) ICON_PALETTE.size() / slotsPerPage));
        pickerPage = Math.max(0, Math.min(pickerPage, totalPages - 1));

        String title = ICON_TITLE_PREFIX + ChatColor.WHITE + homeName;
        Inventory gui = Bukkit.createInventory(null, ICON_PICKER_SIZE, title);

        int start = pickerPage * slotsPerPage;
        int end = Math.min(start + slotsPerPage, ICON_PALETTE.size());

        for (int i = start; i < end; i++) {
            Material mat = ICON_PALETTE.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String matName = mat.name().replace("_", " ");
                // title-case
                StringBuilder sb = new StringBuilder();
                for (String word : matName.split(" ")) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
                }
                meta.setDisplayName(ChatColor.YELLOW + sb.toString());
                meta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to set as icon"));
                item.setItemMeta(meta);
            }
            gui.setItem(i - start, item);
        }

        // Bottom row nav
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            gui.setItem(slot, filler);
        }

        if (pickerPage > 0) {
            gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "â† Previous"));
        }

        // Custom item slot (slot 49) â€” lets the player set their hand item as the icon
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.getType().isAir() && hand.getType().isItem()) {
            ItemStack customBtn = hand.clone();
            customBtn.setAmount(1);
            ItemMeta m = customBtn.getItemMeta();
            if (m == null) m = Bukkit.getItemFactory().getItemMeta(hand.getType());
            if (m != null) {
                m.setDisplayName(ChatColor.AQUA + "Use held item as icon");
                m.setLore(Collections.singletonList(ChatColor.GRAY + "Sets " + hand.getType().name() + " as the icon"));
                customBtn.setItemMeta(m);
            }
            gui.setItem(49, customBtn);
        } else {
            gui.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Hold an item to use it as an icon"));
        }

        // Back button (slot 47)
        gui.setItem(47, createItem(Material.OAK_DOOR, ChatColor.RED + "â† Back to Homes"));

        if (pickerPage < totalPages - 1) {
            gui.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "Next â†’"));
        }

        // Store page in playerPages temporarily (reusing same map)
        playerPages.put(player.getUniqueId(), pickerPage);
        viewingHomes.put(player.getUniqueId(), homeOwner);
        pendingIcon.put(player.getUniqueId(), homeName);

        player.openInventory(gui);
    }

    /** Open the delete-confirmation GUI. */
    private void openConfirmGUI(Player player, UUID homeOwner, String homeName) {
        String title = CONFIRM_TITLE_PREFIX + ChatColor.WHITE + homeName;
        Inventory gui = Bukkit.createInventory(null, 9, title);

        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatColor.GREEN + "âœ” Confirm Delete");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Permanently delete home:");
            lore.add(ChatColor.YELLOW + homeName);
            confirmMeta.setLore(lore);
            confirm.setItemMeta(confirmMeta);
        }
        gui.setItem(2, confirm);

        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.RED + "âœ– Cancel");
            cancel.setItemMeta(cancelMeta);
        }
        gui.setItem(6, cancel);

        pendingDelete.put(player.getUniqueId(), homeName);
        viewingHomes.put(player.getUniqueId(), homeOwner);
        player.openInventory(gui);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Event handlers
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle() == null) return;

        String title = event.getView().getTitle();
        UUID viewerUUID = player.getUniqueId();

        // â”€â”€ Confirm-delete GUI â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (title.startsWith(CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 9) return;

            String homeName = pendingDelete.remove(viewerUUID);
            UUID homeOwner = viewingHomes.getOrDefault(viewerUUID, viewerUUID);

            if (slot == 2 && homeName != null) {
                HomeManager homeManager = plugin.getHomeManager();
                homeManager.deleteHome(homeOwner, homeName);
                player.sendMessage(ChatColor.RED + "Home " + ChatColor.GOLD + homeName + ChatColor.RED + " deleted.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    List<Home> updated = homeManager.getHomeList(homeOwner);
                    int prevPage = playerPages.getOrDefault(viewerUUID, 0);
                    if (updated.isEmpty()) {
                        player.closeInventory();
                        player.sendMessage(ChatColor.YELLOW + "You have no more homes.");
                    } else {
                        int maxPage = Math.max(0, (int) Math.ceil((double) updated.size() / HOMES_PER_PAGE) - 1);
                        openGUI(player, homeOwner, Math.min(prevPage, maxPage));
                    }
                }, 1L);
            } else {
                int prevPage = playerPages.getOrDefault(viewerUUID, 0);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        openGUI(player, homeOwner, prevPage), 1L);
            }
            return;
        }

        // â”€â”€ Icon picker GUI â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (title.startsWith(ICON_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= ICON_PICKER_SIZE) return;

            UUID homeOwner = viewingHomes.getOrDefault(viewerUUID, viewerUUID);
            String homeName = pendingIcon.get(viewerUUID);
            int currentPickerPage = playerPages.getOrDefault(viewerUUID, 0);

            if (slot == 45) {
                // Previous page
                if (currentPickerPage > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            openIconPickerGUI(player, homeOwner, homeName, currentPickerPage - 1), 1L);
                }
                return;
            }
            if (slot == 53) {
                // Next page
                int slotsPerPage = 45;
                int totalPages = Math.max(1, (int) Math.ceil((double) ICON_PALETTE.size() / slotsPerPage));
                if (currentPickerPage < totalPages - 1) {
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            openIconPickerGUI(player, homeOwner, homeName, currentPickerPage + 1), 1L);
                }
                return;
            }
            if (slot == 47) {
                // Back to homes
                pendingIcon.remove(viewerUUID);
                int prevPage = playerPages.getOrDefault(viewerUUID, 0);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        openGUI(player, homeOwner, prevPage), 1L);
                return;
            }

            // Clicked an icon slot â€” could be palette (rows 1-5) or custom (slot 49)
            if (slot >= 45) return; // nav row, handled above or ignore

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (homeName == null) return;

            // slot 49 is the custom-held-item button (within rows 1-5 = valid slot)
            Material chosen = clicked.getType();
            pendingIcon.remove(viewerUUID);

            plugin.getHomeManager().setHomeIcon(homeOwner, homeName, chosen.name());
            player.sendMessage(ChatColor.GREEN + "Icon for home " + ChatColor.GOLD + homeName
                    + ChatColor.GREEN + " set to " + ChatColor.YELLOW + chosen.name().replace("_", " ").toLowerCase() + ChatColor.GREEN + ".");

            int prevPage = playerPages.getOrDefault(viewerUUID, 0);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    openGUI(player, homeOwner, prevPage), 1L);
            return;
        }

        // â”€â”€ Main homes GUI â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true);

        UUID homeOwner = viewingHomes.get(viewerUUID);
        if (homeOwner == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Navigation buttons
        if (slot == 45) {
            int currentPage = playerPages.getOrDefault(viewerUUID, 0);
            if (currentPage > 0) openGUI(player, homeOwner, currentPage - 1);
            return;
        }
        if (slot == 53) {
            int currentPage = playerPages.getOrDefault(viewerUUID, 0);
            HomeManager homeManager = plugin.getHomeManager();
            List<Home> homes = homeManager.getHomeList(homeOwner);
            int totalPages = Math.max(1, (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE));
            if (currentPage < totalPages - 1) openGUI(player, homeOwner, currentPage + 1);
            return;
        }

        // Bottom row â€” ignore
        if (slot >= 45) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int page = playerPages.getOrDefault(viewerUUID, 0);
        int homeIndex = page * HOMES_PER_PAGE + slot;

        HomeManager homeManager = plugin.getHomeManager();
        List<Home> homes = homeManager.getHomeList(homeOwner);
        if (homeIndex >= homes.size()) return;

        Home home = homes.get(homeIndex);

        if (event.isShiftClick()) {
            // Shift+click â†’ delete confirmation
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    openConfirmGUI(player, homeOwner, home.getName()), 1L);
        } else if (event.isRightClick()) {
            // Right-click â†’ icon picker
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    openIconPickerGUI(player, homeOwner, home.getName(), 0), 1L);
        } else {
            // Left-click â†’ teleport
            Location loc = home.getLocation();
            if (loc == null) {
                player.sendMessage(ChatColor.RED + "Could not find world " + ChatColor.GOLD + home.getWorldName() + ChatColor.RED + "!");
                return;
            }
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                TpaManager tpaManager = plugin.getTpaManager();
                if (tpaManager != null) {
                    if (tpaManager.isOnCooldown(player) && !player.hasPermission("qol.teleport.bypass.cooldown")) {
                        long remaining = tpaManager.getCooldownRemaining(player);
                        player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.GOLD
                                + String.format("%.1f", remaining / 1000.0) + "s" + ChatColor.RED + " before teleporting again.");
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
        // Clean up only when not transitioning to a sub-GUI
        if (!pendingDelete.containsKey(uuid) && !pendingIcon.containsKey(uuid)) {
            playerPages.remove(uuid);
            viewingHomes.remove(uuid);
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //  Helpers
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /** Create an item representing a home, showing its current icon material. */
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
            lore.add(ChatColor.YELLOW + "Left-Click" + ChatColor.GRAY + " to teleport");
            lore.add(ChatColor.AQUA + "Right-Click" + ChatColor.GRAY + " to change icon");
            lore.add(ChatColor.RED + "Shift+Click" + ChatColor.GRAY + " to delete");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Create a simple named item (no lore). */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
