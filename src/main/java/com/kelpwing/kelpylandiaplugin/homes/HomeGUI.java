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

public class HomeGUI implements Listener {

    private final KelpylandiaPlugin plugin;

    private static final String GUI_TITLE_PREFIX     = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Homes";
    private static final String CONFIRM_TITLE_PREFIX = ChatColor.DARK_RED   + "" + ChatColor.BOLD + "Delete: ";
    private static final String ICON_TITLE_PREFIX    = ChatColor.GOLD       + "" + ChatColor.BOLD + "Icon: ";

    private static final int HOMES_PER_PAGE   = 45;
    private static final int ICONS_PER_PAGE   = 45;
    private static final int ICON_PICKER_SIZE = 54;

    private static final List<Material> ICON_PALETTE = buildPalette();

    private final Map<UUID, Integer> playerPages          = new HashMap<>();
    private final Map<UUID, UUID>    viewingHomes         = new HashMap<>();
    private final Map<UUID, String>  pendingDelete        = new HashMap<>();
    private final Map<UUID, String>  pendingIcon          = new HashMap<>();
    private final Map<UUID, Integer> iconPickerReturnPage = new HashMap<>();
    private final Set<UUID>          transitioning        = new HashSet<>();

    public HomeGUI(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    // === Icon palette =========================================================

    private static List<Material> buildPalette() {
        String[] candidates = {
            "OAK_SIGN","SPRUCE_SIGN","BIRCH_SIGN","JUNGLE_SIGN","ACACIA_SIGN",
            "DARK_OAK_SIGN","CHERRY_SIGN","MANGROVE_SIGN","BAMBOO_SIGN",
            "CRIMSON_SIGN","WARPED_SIGN",
            "WHITE_BED","ORANGE_BED","MAGENTA_BED","LIGHT_BLUE_BED",
            "YELLOW_BED","LIME_BED","PINK_BED","GRAY_BED","LIGHT_GRAY_BED",
            "CYAN_BED","PURPLE_BED","BLUE_BED","BROWN_BED","GREEN_BED","RED_BED","BLACK_BED",
            "CHEST","ENDER_CHEST","TRAPPED_CHEST",
            "BARREL","SHULKER_BOX",
            "WHITE_SHULKER_BOX","ORANGE_SHULKER_BOX","MAGENTA_SHULKER_BOX",
            "LIGHT_BLUE_SHULKER_BOX","YELLOW_SHULKER_BOX","LIME_SHULKER_BOX",
            "PINK_SHULKER_BOX","GRAY_SHULKER_BOX","LIGHT_GRAY_SHULKER_BOX",
            "CYAN_SHULKER_BOX","PURPLE_SHULKER_BOX","BLUE_SHULKER_BOX",
            "BROWN_SHULKER_BOX","GREEN_SHULKER_BOX","RED_SHULKER_BOX","BLACK_SHULKER_BOX",
            "CRAFTING_TABLE","FURNACE","BLAST_FURNACE","SMOKER",
            "ANVIL","CHIPPED_ANVIL","DAMAGED_ANVIL","GRINDSTONE",
            "SMITHING_TABLE","STONECUTTER","CARTOGRAPHY_TABLE","LOOM",
            "ENCHANTING_TABLE","BOOKSHELF","CHISELED_BOOKSHELF","LECTERN",
            "BREWING_STAND","CAULDRON","COMPOSTER",
            "LANTERN","SOUL_LANTERN","TORCH","SOUL_TORCH","GLOWSTONE",
            "SEA_LANTERN","SHROOMLIGHT","CAMPFIRE","SOUL_CAMPFIRE",
            "END_ROD","BEACON","CRYING_OBSIDIAN","JACK_O_LANTERN",
            "OAK_SAPLING","SPRUCE_SAPLING","BIRCH_SAPLING","JUNGLE_SAPLING",
            "ACACIA_SAPLING","DARK_OAK_SAPLING","CHERRY_SAPLING","MANGROVE_PROPAGULE",
            "DANDELION","POPPY","BLUE_ORCHID","ALLIUM","AZURE_BLUET",
            "RED_TULIP","ORANGE_TULIP","PINK_TULIP","WHITE_TULIP",
            "OXEYE_DAISY","CORNFLOWER","LILY_OF_THE_VALLEY",
            "SUNFLOWER","LILAC","ROSE_BUSH","PEONY","PITCHER_PLANT","TORCHFLOWER",
            "RED_MUSHROOM","BROWN_MUSHROOM",
            "CACTUS","BAMBOO","SUGAR_CANE","LILY_PAD","VINE","SEA_PICKLE",
            "KELP","SEAGRASS","DRIED_KELP_BLOCK",
            "WHEAT","MELON","PUMPKIN","CHORUS_FLOWER","GLOW_BERRIES",
            "DIAMOND","EMERALD","GOLD_INGOT","IRON_INGOT","NETHERITE_INGOT",
            "AMETHYST_SHARD","COPPER_INGOT","LAPIS_LAZULI","QUARTZ",
            "COAL","REDSTONE","GLOWSTONE_DUST",
            "PRISMARINE_CRYSTALS","PRISMARINE_SHARD",
            "DIAMOND_ORE","EMERALD_ORE","DEEPSLATE_GOLD_ORE",
            "DEEPSLATE_IRON_ORE","DEEPSLATE_DIAMOND_ORE",
            "ANCIENT_DEBRIS","RAW_GOLD","RAW_IRON","RAW_COPPER",
            "OAK_LOG","SPRUCE_LOG","BIRCH_LOG","JUNGLE_LOG","ACACIA_LOG",
            "DARK_OAK_LOG","CHERRY_LOG","MANGROVE_LOG","BAMBOO_BLOCK",
            "OAK_PLANKS","SPRUCE_PLANKS","BIRCH_PLANKS","JUNGLE_PLANKS",
            "ACACIA_PLANKS","DARK_OAK_PLANKS","CHERRY_PLANKS","MANGROVE_PLANKS",
            "STONE","COBBLESTONE","MOSSY_COBBLESTONE","SMOOTH_STONE",
            "STONE_BRICKS","MOSSY_STONE_BRICKS","CRACKED_STONE_BRICKS","CHISELED_STONE_BRICKS",
            "GRANITE","POLISHED_GRANITE","DIORITE","POLISHED_DIORITE","ANDESITE","POLISHED_ANDESITE",
            "CALCITE","TUFF","DEEPSLATE","COBBLED_DEEPSLATE","DEEPSLATE_BRICKS","DEEPSLATE_TILES",
            "SAND","RED_SAND","GRAVEL",
            "DIRT","COARSE_DIRT","PODZOL","GRASS_BLOCK","MYCELIUM","ROOTED_DIRT",
            "MUD","PACKED_MUD","MUD_BRICKS",
            "SNOW_BLOCK","ICE","PACKED_ICE","BLUE_ICE","OBSIDIAN","BRICKS",
            "TERRACOTTA",
            "WHITE_TERRACOTTA","ORANGE_TERRACOTTA","MAGENTA_TERRACOTTA","LIGHT_BLUE_TERRACOTTA",
            "YELLOW_TERRACOTTA","LIME_TERRACOTTA","PINK_TERRACOTTA",
            "GRAY_TERRACOTTA","LIGHT_GRAY_TERRACOTTA","CYAN_TERRACOTTA","PURPLE_TERRACOTTA",
            "BLUE_TERRACOTTA","BROWN_TERRACOTTA","GREEN_TERRACOTTA","RED_TERRACOTTA","BLACK_TERRACOTTA",
            "WHITE_GLAZED_TERRACOTTA","ORANGE_GLAZED_TERRACOTTA","MAGENTA_GLAZED_TERRACOTTA",
            "YELLOW_GLAZED_TERRACOTTA","LIME_GLAZED_TERRACOTTA","CYAN_GLAZED_TERRACOTTA","BLUE_GLAZED_TERRACOTTA",
            "CLAY","HAY_BLOCK","SPONGE",
            "PRISMARINE","DARK_PRISMARINE","PRISMARINE_BRICKS",
            "SANDSTONE","SMOOTH_SANDSTONE","CHISELED_SANDSTONE",
            "RED_SANDSTONE","SMOOTH_RED_SANDSTONE",
            "QUARTZ_BLOCK","SMOOTH_QUARTZ","QUARTZ_PILLAR",
            "WHITE_WOOL","ORANGE_WOOL","MAGENTA_WOOL","LIGHT_BLUE_WOOL",
            "YELLOW_WOOL","LIME_WOOL","PINK_WOOL","GRAY_WOOL","LIGHT_GRAY_WOOL",
            "CYAN_WOOL","PURPLE_WOOL","BLUE_WOOL","BROWN_WOOL","GREEN_WOOL","RED_WOOL","BLACK_WOOL",
            "WHITE_CONCRETE","ORANGE_CONCRETE","MAGENTA_CONCRETE","LIGHT_BLUE_CONCRETE",
            "YELLOW_CONCRETE","LIME_CONCRETE","PINK_CONCRETE","GRAY_CONCRETE",
            "CYAN_CONCRETE","PURPLE_CONCRETE","BLUE_CONCRETE","BROWN_CONCRETE",
            "GREEN_CONCRETE","RED_CONCRETE","BLACK_CONCRETE",
            "NETHER_BRICK","RED_NETHER_BRICKS","NETHERRACK","SOUL_SAND","SOUL_SOIL",
            "BASALT","POLISHED_BASALT","SMOOTH_BASALT",
            "BLACKSTONE","POLISHED_BLACKSTONE","GILDED_BLACKSTONE",
            "CRIMSON_PLANKS","WARPED_PLANKS","CRIMSON_STEM","WARPED_STEM",
            "CRIMSON_FUNGUS","WARPED_FUNGUS","NETHERWART_BLOCK","WARPED_WART_BLOCK",
            "MAGMA_BLOCK","NETHER_WART","BLAZE_ROD","GHAST_TEAR","BONE_BLOCK",
            "END_STONE","END_STONE_BRICKS","PURPUR_BLOCK","PURPUR_PILLAR",
            "CHORUS_PLANT","DRAGON_EGG","ENDER_PEARL","ENDER_EYE","SHULKER_SHELL","ELYTRA",
            "WOODEN_SWORD","STONE_SWORD","IRON_SWORD","DIAMOND_SWORD","NETHERITE_SWORD",
            "IRON_AXE","DIAMOND_AXE","NETHERITE_AXE",
            "IRON_PICKAXE","DIAMOND_PICKAXE","NETHERITE_PICKAXE",
            "IRON_SHOVEL","DIAMOND_SHOVEL","IRON_HOE","DIAMOND_HOE",
            "BOW","CROSSBOW","TRIDENT","SHIELD","MACE",
            "IRON_HELMET","DIAMOND_HELMET","NETHERITE_HELMET",
            "IRON_CHESTPLATE","DIAMOND_CHESTPLATE","NETHERITE_CHESTPLATE",
            "IRON_LEGGINGS","DIAMOND_LEGGINGS","NETHERITE_LEGGINGS",
            "IRON_BOOTS","DIAMOND_BOOTS","NETHERITE_BOOTS",
            "TOTEM_OF_UNDYING","FISHING_ROD","FLINT_AND_STEEL","SHEARS",
            "APPLE","GOLDEN_APPLE","ENCHANTED_GOLDEN_APPLE",
            "BREAD","COOKED_BEEF","COOKED_PORKCHOP","COOKED_CHICKEN",
            "COOKED_MUTTON","COOKED_SALMON","COOKED_COD",
            "PUMPKIN_PIE","CAKE","COOKIE","HONEY_BOTTLE","SUSPICIOUS_STEW",
            "CARROT","GOLDEN_CARROT","BEETROOT","MELON_SLICE","SWEET_BERRIES",
            "POTION","SPLASH_POTION","LINGERING_POTION",
            "EXPERIENCE_BOTTLE","ENCHANTED_BOOK",
            "NAME_TAG","COMPASS","CLOCK","SPYGLASS","MAP","FILLED_MAP","BOOK","WRITABLE_BOOK",
            "REDSTONE_BLOCK","OBSERVER","PISTON","STICKY_PISTON",
            "DISPENSER","DROPPER","HOPPER","COMPARATOR","REPEATER",
            "LEVER","REDSTONE_TORCH","TARGET","TRIPWIRE_HOOK","DAYLIGHT_DETECTOR",
            "NOTE_BLOCK","JUKEBOX","TNT",
            "BONE","FEATHER","EGG","STRING","SPIDER_EYE","GUNPOWDER",
            "INK_SAC","GLOW_INK_SAC","RABBIT_FOOT","LEATHER","SLIME_BALL",
            "MAGMA_CREAM","NETHER_STAR","HEART_OF_THE_SEA",
            "NAUTILUS_SHELL","TURTLE_SCUTE","HONEYCOMB","AMETHYST_BLOCK",
            "WITHER_SKELETON_SKULL","SKELETON_SKULL","CREEPER_HEAD",
            "DRAGON_HEAD","PIGLIN_HEAD","PLAYER_HEAD",
            "PAINTING","ITEM_FRAME","GLOW_ITEM_FRAME","ARMOR_STAND",
            "FLOWER_POT","DECORATED_POT",
            "CANDLE","WHITE_CANDLE","ORANGE_CANDLE","YELLOW_CANDLE",
            "LIME_CANDLE","CYAN_CANDLE","BLUE_CANDLE","PURPLE_CANDLE","RED_CANDLE","BLACK_CANDLE",
            "WHITE_BANNER","ORANGE_BANNER","MAGENTA_BANNER","LIGHT_BLUE_BANNER",
            "YELLOW_BANNER","LIME_BANNER","PINK_BANNER","GRAY_BANNER",
            "LIGHT_GRAY_BANNER","CYAN_BANNER","PURPLE_BANNER","BLUE_BANNER",
            "BROWN_BANNER","GREEN_BANNER","RED_BANNER","BLACK_BANNER",
        };
        List<Material> result = new ArrayList<>();
        for (String name : candidates) {
            try {
                Material m = Material.valueOf(name);
                if (m.isItem() && !m.isAir()) result.add(m);
            } catch (IllegalArgumentException ignored) {}
        }
        return Collections.unmodifiableList(result);
    }

    // === Public open methods ==================================================

    public void openGUI(Player player) {
        openGUI(player, player.getUniqueId(), 0);
    }

    public void openGUI(Player player, UUID homeOwner, int page) {
        HomeManager homeManager = plugin.getHomeManager();
        List<Home> homes = homeManager.getHomeList(homeOwner);
        int totalHomes = homes.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalHomes / HOMES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = GUI_TITLE_PREFIX + ChatColor.GRAY + " (" + totalHomes + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        ItemStack filler = makeFiller();
        for (int slot = 0; slot < 54; slot++) gui.setItem(slot, filler);

        int startIdx = page * HOMES_PER_PAGE;
        int endIdx   = Math.min(startIdx + HOMES_PER_PAGE, totalHomes);
        for (int i = startIdx; i < endIdx; i++) {
            gui.setItem(i - startIdx, createHomeItem(homes.get(i)));
        }

        if (page > 0) {
            gui.setItem(45, makeNavItem(Material.ARROW,
                ChatColor.YELLOW + "< Previous Page",
                ChatColor.GRAY + "Page " + page + " of " + totalPages));
        }
        gui.setItem(49, makeNavItem(Material.BOOK,
            ChatColor.GOLD + "Homes " + ChatColor.GRAY + "(" + totalHomes + ")",
            ChatColor.GRAY + "Page " + (page + 1) + " of " + totalPages,
            "",
            ChatColor.YELLOW + "Left-click"  + ChatColor.GRAY + ": Teleport",
            ChatColor.YELLOW + "Right-click" + ChatColor.GRAY + ": Change icon",
            ChatColor.YELLOW + "Shift-click" + ChatColor.GRAY + ": Delete",
            ChatColor.GRAY   + "Tip: /renhome <old> <new>"));
        if (page < totalPages - 1) {
            gui.setItem(53, makeNavItem(Material.ARROW,
                ChatColor.YELLOW + "Next Page >",
                ChatColor.GRAY + "Page " + (page + 2) + " of " + totalPages));
        }

        UUID uuid = player.getUniqueId();
        playerPages.put(uuid, page);
        viewingHomes.put(uuid, homeOwner);
        openWithTransition(player, gui);
    }

    // === Private GUI openers ==================================================

    private void openIconPickerGUI(Player player, UUID homeOwner, String homeName, int iconPage) {
        int totalIcons = ICON_PALETTE.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalIcons / ICONS_PER_PAGE));
        iconPage = Math.max(0, Math.min(iconPage, totalPages - 1));

        String title = ICON_TITLE_PREFIX + ChatColor.WHITE + homeName;
        Inventory gui = Bukkit.createInventory(null, ICON_PICKER_SIZE, title);

        ItemStack filler = makeFiller();
        for (int slot = 0; slot < 54; slot++) gui.setItem(slot, filler);

        int start = iconPage * ICONS_PER_PAGE;
        int end   = Math.min(start + ICONS_PER_PAGE, totalIcons);
        for (int i = start; i < end; i++) {
            gui.setItem(i - start, makeIconPaletteItem(ICON_PALETTE.get(i)));
        }

        // Nav: 45=Back, 46=Prev, 49=Info, 52=Next, 53=Use Held Item
        gui.setItem(45, makeNavItem(Material.BARRIER,
            ChatColor.RED + "< Back",
            ChatColor.GRAY + "Return to homes list"));
        if (iconPage > 0) {
            gui.setItem(46, makeNavItem(Material.ARROW,
                ChatColor.YELLOW + "< Previous Icons",
                ChatColor.GRAY + "Page " + iconPage + " of " + totalPages));
        }
        gui.setItem(49, makeNavItem(Material.COMPASS,
            ChatColor.GOLD + "Choose Icon " + ChatColor.GRAY + "for " + ChatColor.YELLOW + homeName,
            ChatColor.GRAY + "Page " + (iconPage + 1) + " of " + totalPages,
            "",
            ChatColor.YELLOW + "Click"    + ChatColor.GRAY + " an icon above to set it",
            ChatColor.YELLOW + "Or click" + ChatColor.GRAY + " any item in your inventory",
            ChatColor.YELLOW + "Slot 53"  + ChatColor.GRAY + " = Use your held item"));
        if ((iconPage + 1) * ICONS_PER_PAGE < totalIcons) {
            gui.setItem(52, makeNavItem(Material.ARROW,
                ChatColor.YELLOW + "Next Icons >",
                ChatColor.GRAY + "Page " + (iconPage + 2) + " of " + totalPages));
        }
        gui.setItem(53, makeNavItem(Material.STICK,
            ChatColor.GREEN + "Use Held Item",
            ChatColor.GRAY + "Sets the icon to whatever",
            ChatColor.GRAY + "item you are currently holding"));

        UUID uuid = player.getUniqueId();
        playerPages.put(uuid, iconPage);
        viewingHomes.put(uuid, homeOwner);
        pendingIcon.put(uuid, homeName);
        openWithTransition(player, gui);
    }

    private void openConfirmGUI(Player player, UUID homeOwner, String homeName) {
        String title = CONFIRM_TITLE_PREFIX + ChatColor.WHITE + homeName;
        Inventory gui = Bukkit.createInventory(null, 9, title);

        // slot 2=Cancel, slot 4=Display, slot 6=Confirm
        gui.setItem(2, makeNavItem(Material.RED_STAINED_GLASS_PANE,
            ChatColor.RED + "Cancel",
            ChatColor.GRAY + "Keep " + ChatColor.YELLOW + homeName));
        gui.setItem(4, makeNavItem(Material.BARRIER,
            ChatColor.DARK_RED + "Delete " + ChatColor.YELLOW + homeName,
            ChatColor.GRAY + "This cannot be undone!"));
        gui.setItem(6, makeNavItem(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "Confirm Delete",
            ChatColor.GRAY + "Permanently delete " + ChatColor.YELLOW + homeName));

        UUID uuid = player.getUniqueId();
        pendingDelete.put(uuid, homeName);
        viewingHomes.put(uuid, homeOwner);
        openWithTransition(player, gui);
    }

    /**
     * Opens a GUI inventory while guarding state against InventoryCloseEvent
     * firing synchronously inside Player#openInventory().
     *
     * Without the transitioning guard, the close event would fire mid-call and
     * immediately wipe playerPages/viewingHomes that were just written.
     */
    private void openWithTransition(Player player, Inventory gui) {
        UUID uuid = player.getUniqueId();
        transitioning.add(uuid);
        player.openInventory(gui); // InventoryCloseEvent fires here; handler returns early
        transitioning.remove(uuid); // safety: clear if close event did not fire
    }

    // === Event handlers =======================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title == null) return;

        UUID uuid = player.getUniqueId();

        // --- Delete-confirm GUI ----------------------------------------------
        if (title.startsWith(CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 9) return;

            String homeName  = pendingDelete.get(uuid);
            UUID   homeOwner = viewingHomes.getOrDefault(uuid, uuid);
            int    prevPage  = playerPages.getOrDefault(uuid, 0);

            if (slot == 6 && homeName != null) {
                HomeManager hm = plugin.getHomeManager();
                hm.deleteHome(homeOwner, homeName);
                player.sendMessage(ChatColor.RED + "Home " + ChatColor.GOLD + homeName + ChatColor.RED + " deleted.");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    pendingDelete.remove(uuid);
                    List<Home> updated = hm.getHomeList(homeOwner);
                    if (updated.isEmpty()) {
                        player.closeInventory();
                        player.sendMessage(ChatColor.YELLOW + "You have no more homes.");
                    } else {
                        int maxPage = Math.max(0, (int) Math.ceil((double) updated.size() / HOMES_PER_PAGE) - 1);
                        openGUI(player, homeOwner, Math.min(prevPage, maxPage));
                    }
                }, 1L);
            } else if (slot == 2) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    pendingDelete.remove(uuid);
                    openGUI(player, homeOwner, prevPage);
                }, 1L);
            }
            return;
        }

        // --- Icon-picker GUI -------------------------------------------------
        if (title.startsWith(ICON_TITLE_PREFIX)) {
            event.setCancelled(true);
            int    slot      = event.getRawSlot();
            UUID   homeOwner = viewingHomes.getOrDefault(uuid, uuid);
            String homeName  = pendingIcon.get(uuid);
            int    iconPage  = playerPages.getOrDefault(uuid, 0);
            int    homesPage = iconPickerReturnPage.getOrDefault(uuid, 0);

            // Player clicked their own inventory (slots 54-89 in a 6-row handler)
            if (slot >= ICON_PICKER_SIZE) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && !clicked.getType().isAir()
                        && clicked.getType().isItem() && homeName != null) {
                    applyIcon(player, homeOwner, homeName, homesPage, clicked.getType());
                }
                return;
            }
            if (slot < 0) return;

            switch (slot) {
                case 45 -> Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    pendingIcon.remove(uuid);
                    iconPickerReturnPage.remove(uuid);
                    openGUI(player, homeOwner, homesPage);
                }, 1L);
                case 46 -> {
                    if (iconPage > 0) {
                        int np = iconPage - 1;
                        Bukkit.getScheduler().runTaskLater(plugin, () ->
                            openIconPickerGUI(player, homeOwner, homeName, np), 1L);
                    }
                }
                case 52 -> {
                    int totalPages = Math.max(1, (int) Math.ceil((double) ICON_PALETTE.size() / ICONS_PER_PAGE));
                    if (iconPage < totalPages - 1) {
                        int np = iconPage + 1;
                        Bukkit.getScheduler().runTaskLater(plugin, () ->
                            openIconPickerGUI(player, homeOwner, homeName, np), 1L);
                    }
                }
                case 53 -> {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (!held.getType().isAir() && held.getType().isItem() && homeName != null) {
                        applyIcon(player, homeOwner, homeName, homesPage, held.getType());
                    } else {
                        player.sendMessage(ChatColor.RED + "You are not holding any item.");
                    }
                }
                default -> {
                    if (slot >= 45) return; // filler nav slots
                    ItemStack clicked = event.getCurrentItem();
                    if (clicked == null || clicked.getType().isAir()
                            || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
                    if (homeName == null) return;
                    applyIcon(player, homeOwner, homeName, homesPage, clicked.getType());
                }
            }
            return;
        }

        // --- Main homes GUI --------------------------------------------------
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        event.setCancelled(true);

        UUID homeOwner = viewingHomes.get(uuid);
        if (homeOwner == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        int page = playerPages.getOrDefault(uuid, 0);

        if (slot == 45) {
            if (page > 0) openGUI(player, homeOwner, page - 1);
            return;
        }
        if (slot == 53) {
            List<Home> homes = plugin.getHomeManager().getHomeList(homeOwner);
            int total = Math.max(1, (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE));
            if (page < total - 1) openGUI(player, homeOwner, page + 1);
            return;
        }
        if (slot >= 45) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int homeIndex = page * HOMES_PER_PAGE + slot;
        List<Home> homes = plugin.getHomeManager().getHomeList(homeOwner);
        if (homeIndex >= homes.size()) return;

        Home home = homes.get(homeIndex);

        if (event.isShiftClick()) {
            pendingDelete.put(uuid, home.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                openConfirmGUI(player, homeOwner, home.getName()), 1L);
        } else if (event.isRightClick()) {
            iconPickerReturnPage.put(uuid, page);
            pendingIcon.put(uuid, home.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                openIconPickerGUI(player, homeOwner, home.getName(), 0), 1L);
        } else {
            Location loc = home.getLocation();
            if (loc == null) {
                player.sendMessage(ChatColor.RED + "Could not find world "
                    + ChatColor.GOLD + home.getWorldName() + ChatColor.RED + "!");
                return;
            }
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                TpaManager tpaManager = plugin.getTpaManager();
                if (tpaManager != null) {
                    if (tpaManager.isOnCooldown(player)
                            && !player.hasPermission("qol.teleport.bypass.cooldown")) {
                        long rem = tpaManager.getCooldownRemaining(player);
                        player.sendMessage(ChatColor.RED + "Wait " + ChatColor.GOLD
                            + String.format("%.1f", rem / 1000.0) + "s"
                            + ChatColor.RED + " before teleporting again.");
                        return;
                    }
                    tpaManager.applyCooldown(player);
                    tpaManager.applyInvulnerability(player);
                }
                player.teleport(loc);
                player.sendMessage(ChatColor.GREEN + "Teleported to home "
                    + ChatColor.GOLD + home.getName() + ChatColor.GREEN + "!");
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (transitioning.remove(uuid)) return; // mid-transition: skip cleanup
        if (!pendingDelete.containsKey(uuid) && !pendingIcon.containsKey(uuid)) {
            playerPages.remove(uuid);
            viewingHomes.remove(uuid);
            iconPickerReturnPage.remove(uuid);
        }
    }

    // === Helpers ==============================================================

    private void applyIcon(Player player, UUID homeOwner, String homeName, int homesPage, Material icon) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingIcon.remove(uuid);
            iconPickerReturnPage.remove(uuid);
            plugin.getHomeManager().setHomeIcon(homeOwner, homeName, icon.name());
            player.sendMessage(ChatColor.GREEN + "Icon for " + ChatColor.GOLD + homeName
                + ChatColor.GREEN + " set to "
                + ChatColor.YELLOW + titleCase(icon.name()) + ChatColor.GREEN + ".");
            openGUI(player, homeOwner, homesPage);
        }, 1L);
    }

    private ItemStack createHomeItem(Home home) {
        Material material;
        try {
            material = Material.valueOf(home.getIcon().toUpperCase());
            if (!material.isItem() || material.isAir()) material = Material.OAK_SIGN;
        } catch (IllegalArgumentException e) {
            material = Material.OAK_SIGN;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + home.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + String.format("%.1f, %.1f, %.1f",
                home.getX(), home.getY(), home.getZ()));
            lore.add(ChatColor.GRAY + formatDimension(home.getWorldName()));
            if (home.getDescription() != null && !home.getDescription().isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + home.getDescription());
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-click"  + ChatColor.GRAY + ": Teleport");
            lore.add(ChatColor.YELLOW + "Right-click" + ChatColor.GRAY + ": Change icon");
            lore.add(ChatColor.YELLOW + "Shift-click" + ChatColor.GRAY + ": Delete");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeIconPaletteItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + titleCase(mat.name()));
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to use as icon"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeNavItem(Material material, String... lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lines[0]);
            if (lines.length > 1) {
                List<String> lore = new ArrayList<>();
                for (int i = 1; i < lines.length; i++) lore.add(lines[i]);
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatDimension(String worldName) {
        return switch (worldName.toLowerCase()) {
            case "world"         -> "Overworld";
            case "world_nether"  -> "Nether";
            case "world_the_end" -> "The End";
            default              -> worldName;
        };
    }

    private static String titleCase(String underscored) {
        StringBuilder sb = new StringBuilder();
        for (String word : underscored.split("_")) {
            if (sb.length() > 0) sb.append(" ");
            if (!word.isEmpty())
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}