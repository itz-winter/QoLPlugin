package com.kelpwing.kelpylandiaplugin.economy;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.commands.SellCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin GUI for managing item/category sell prices.
 * - Browse all configured prices with pagination
 * - Click to edit price (via chat prompt)
 * - Shift+click to remove price
 * - Bottom row: navigation, add new price, search
 */
public class ShopEditGUI implements Listener {

    private final KelpylandiaPlugin plugin;

    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Shop Editor";
    private static final int GUI_SIZE = 54; // 6 rows
    private static final int ITEMS_PER_PAGE = 45; // rows 1-5

    // Navigation slot indices (row 6)
    private static final int SLOT_PREV = 45;
    private static final int SLOT_SEARCH = 49;
    private static final int SLOT_ADD = 50;
    private static final int SLOT_NEXT = 53;

    // Per-player state
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> playerSearch = new HashMap<>(); // active search filter

    // Players waiting for chat input (price edit or add new)
    private final Map<UUID, ChatInputState> awaitingChatInput = new HashMap<>();

    public ShopEditGUI(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the shop editor GUI for a player.
     */
    public void openGUI(Player player) {
        openGUI(player, 0);
    }

    public void openGUI(Player player, int page) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            player.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return;
        }

        List<Map.Entry<String, Double>> prices = getFilteredPrices(player);
        int totalPages = Math.max(1, (int) Math.ceil((double) prices.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        String title = GUI_TITLE_PREFIX + " " + ChatColor.GRAY + "(" + (page + 1) + "/" + totalPages + ")";
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);

        // Fill items for this page
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, prices.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Double> entry = prices.get(i);
            int slot = i - startIndex;
            gui.setItem(slot, createPriceItem(eco, entry.getKey(), entry.getValue()));
        }

        // ── Navigation row ───────────────────────────────────────
        // Previous page
        if (page > 0) {
            gui.setItem(SLOT_PREV, createNavItem(Material.ARROW, ChatColor.YELLOW + "← Previous Page"));
        } else {
            gui.setItem(SLOT_PREV, createNavItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "No Previous Page"));
        }

        // Search
        String searchLabel = playerSearch.containsKey(player.getUniqueId())
                ? ChatColor.AQUA + "Search: " + ChatColor.WHITE + playerSearch.get(player.getUniqueId())
                : ChatColor.AQUA + "Search (click to filter)";
        gui.setItem(SLOT_SEARCH, createNavItem(Material.COMPASS, searchLabel));

        // Add new price
        gui.setItem(SLOT_ADD, createNavItem(Material.EMERALD, ChatColor.GREEN + "Add New Price"));

        // Next page
        if (page < totalPages - 1) {
            gui.setItem(SLOT_NEXT, createNavItem(Material.ARROW, ChatColor.YELLOW + "Next Page →"));
        } else {
            gui.setItem(SLOT_NEXT, createNavItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "No Next Page"));
        }

        // Fill empty nav slots with glass panes
        for (int slot = 45; slot < 54; slot++) {
            if (gui.getItem(slot) == null) {
                gui.setItem(slot, createNavItem(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true); // prevent taking items

        Player player = (Player) event.getWhoClicked();
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // ── Navigation clicks ────────────────────────────────────
        if (slot == SLOT_PREV) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) openGUI(player, page - 1);
            return;
        }
        if (slot == SLOT_NEXT) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            openGUI(player, page + 1);
            return;
        }
        if (slot == SLOT_SEARCH) {
            if (event.isShiftClick()) {
                // Clear search
                playerSearch.remove(player.getUniqueId());
                openGUI(player, 0);
            } else {
                player.closeInventory();
                player.sendMessage(ChatColor.AQUA + "Type a search term in chat (or 'cancel' to cancel):");
                awaitingChatInput.put(player.getUniqueId(), new ChatInputState(ChatInputType.SEARCH, null));
            }
            return;
        }
        if (slot == SLOT_ADD) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Type the item or category name to add (e.g. 'diamond' or '#minecraft:planks'):");
            player.sendMessage(ChatColor.GRAY + "Type 'cancel' to cancel.");
            awaitingChatInput.put(player.getUniqueId(), new ChatInputState(ChatInputType.ADD_ITEM, null));
            return;
        }

        // ── Price item clicks ────────────────────────────────────
        if (slot < ITEMS_PER_PAGE) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // Get the config key from the item lore
            String configKey = getConfigKeyFromItem(clicked);
            if (configKey == null) return;

            if (event.isShiftClick()) {
                // Remove price
                eco.removePrice(configKey);
                player.sendMessage(ChatColor.GREEN + "Removed price for " + ChatColor.YELLOW + configKey + ChatColor.GREEN + ".");
                int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                openGUI(player, page);
            } else {
                // Edit price via chat
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Enter new price for " + ChatColor.WHITE + configKey + ChatColor.YELLOW + ":");
                player.sendMessage(ChatColor.GRAY + "Type 'cancel' to cancel.");
                awaitingChatInput.put(player.getUniqueId(), new ChatInputState(ChatInputType.EDIT_PRICE, configKey));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        // Clean up page tracking if needed (keep it for re-opening)
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatInputState state = awaitingChatInput.remove(player.getUniqueId());
        if (state == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.GRAY + "Cancelled.");
            Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0)));
            return;
        }

        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) return;

        switch (state.type) {
            case SEARCH:
                playerSearch.put(player.getUniqueId(), input.toLowerCase());
                Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, 0));
                break;

            case ADD_ITEM:
                // They typed an item/category name — now ask for price
                if (input.startsWith("#")) {
                    String tagName = input.substring(1);
                    Tag<Material> tag = EconomyManager.resolveTag(tagName);
                    if (tag == null) {
                        player.sendMessage(ChatColor.RED + "Unknown category: " + tagName);
                        Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0)));
                        return;
                    }
                } else {
                    Material mat = EconomyManager.parseMaterial(input);
                    if (mat == null) {
                        player.sendMessage(ChatColor.RED + "Unknown item: " + input);
                        Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0)));
                        return;
                    }
                    input = mat.name().toLowerCase(); // normalize
                }
                player.sendMessage(ChatColor.YELLOW + "Now enter the sell price for " + ChatColor.WHITE + input + ChatColor.YELLOW + ":");
                awaitingChatInput.put(player.getUniqueId(), new ChatInputState(ChatInputType.EDIT_PRICE, input.startsWith("#") ? input : input));
                break;

            case EDIT_PRICE:
                try {
                    double price = Double.parseDouble(input);
                    if (price < 0) {
                        player.sendMessage(ChatColor.RED + "Price cannot be negative.");
                        Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0)));
                        return;
                    }
                    String key = state.configKey;
                    // Normalize the key for storage
                    if (key.startsWith("#")) {
                        // Category — store as #minecraft:planks etc
                        eco.setPrice(key, price);
                    } else {
                        eco.setPrice(key, price);
                    }
                    player.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + key + ChatColor.GREEN
                            + " price to " + ChatColor.YELLOW + eco.getUnit() + String.format("%." + eco.getDecimals() + "f", price) + ChatColor.GREEN + ".");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid price: " + input);
                }
                Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0)));
                break;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════

    private List<Map.Entry<String, Double>> getFilteredPrices(Player player) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) return Collections.emptyList();

        Map<String, Double> all = eco.getAllConfiguredPrices();
        String search = playerSearch.get(player.getUniqueId());

        if (search == null || search.isEmpty()) {
            return new ArrayList<>(all.entrySet());
        }

        return all.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(search))
                .collect(Collectors.toList());
    }

    private ItemStack createPriceItem(EconomyManager eco, String configKey, double price) {
        Material displayMat;
        String displayName;

        if (configKey.startsWith("#")) {
            // Category — show a representative icon
            displayMat = Material.NAME_TAG;
            displayName = ChatColor.LIGHT_PURPLE + configKey;
        } else {
            Material mat = EconomyManager.parseMaterial(configKey);
            displayMat = (mat != null && mat.isItem()) ? mat : Material.BARRIER;
            displayName = ChatColor.WHITE + SellCommand.formatMaterial(displayMat);
        }

        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Price: " + ChatColor.GREEN + eco.getUnit()
                    + BigDecimal.valueOf(price).setScale(eco.getDecimals(), RoundingMode.HALF_UP).toPlainString());
            lore.add("");
            lore.add(ChatColor.GRAY + "Click to edit price");
            lore.add(ChatColor.GRAY + "Shift+Click to remove");
            lore.add(ChatColor.DARK_GRAY + "Key: " + configKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getConfigKeyFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;

        // The config key is stored in the last lore line as "Key: <key>"
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.startsWith("Key: ")) {
                return stripped.substring(5);
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════
    //  Chat input state
    // ════════════════════════════════════════════════════════════════

    private enum ChatInputType {
        SEARCH, ADD_ITEM, EDIT_PRICE
    }

    private static class ChatInputState {
        final ChatInputType type;
        final String configKey; // used for EDIT_PRICE

        ChatInputState(ChatInputType type, String configKey) {
            this.type = type;
            this.configKey = configKey;
        }
    }
}
