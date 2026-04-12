package com.kelpwing.kelpylandiaplugin.chat;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces InteractiveChat — provides [item], [inv], and [enderchest] keywords
 * in chat using Spigot's BungeeCord Chat Component API.
 * <p>
 * When a player types one of the keywords, this manager:
 * <ul>
 *   <li>[item]  — shows a hoverable item component (name, lore, enchants) and
 *                 a clickable inventory viewer for that item</li>
 *   <li>[inv]   — shows a hoverable summary and opens a read-only snapshot of
 *                 the player's inventory on click</li>
 *   <li>[enderchest] — same but for ender chest</li>
 * </ul>
 * <p>
 * Snapshots are stored briefly so that clicking the component can open a
 * frozen view even if the player changes their inventory later.
 */
public class ItemDisplayManager {

    // Pattern that matches [item], [inv], [inventory], [enderchest], [ec] — case insensitive
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "\\[(item|inv|inventory|enderchest|ec)]",
            Pattern.CASE_INSENSITIVE
    );

    private final KelpylandiaPlugin plugin;

    /**
     * Cached inventory snapshots keyed by a random UUID.
     * Each snapshot expires after a configurable time (default 5 min).
     */
    private final ConcurrentHashMap<UUID, SnapshotEntry> snapshots = new ConcurrentHashMap<>();

    public ItemDisplayManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;

        // Purge expired snapshots every 60 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long ttl = plugin.getConfig().getLong("chat-items.snapshot-ttl-seconds", 300) * 1000L;
            snapshots.entrySet().removeIf(e -> now - e.getValue().createdAt > ttl);
        }, 20L * 60, 20L * 60);
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the given raw message contains at least one
     * recognised keyword ([item], [inv], [enderchest]).
     */
    public boolean containsKeyword(String message) {
        return KEYWORD_PATTERN.matcher(message).find();
    }

    /**
     * Build the full chat line as a component array, replacing any recognised
     * keywords with interactive components.  Non-keyword portions are kept as
     * plain {@link TextComponent}s with the supplied legacy chat colours.
     *
     * @param player        sender — used to snapshot held item / inventory
     * @param prefixAndName the already-coloured prefix+display-name section
     *                      (everything <em>before</em> the message body)
     * @param rawMessage    the player's raw chat message
     * @return ready-to-send component array
     */
    public BaseComponent[] buildChatLine(Player player, String prefixAndName, String rawMessage) {
        ComponentBuilder builder = new ComponentBuilder();

        // Append the prefix + name + separator
        builder.append(TextComponent.fromLegacyText(prefixAndName), ComponentBuilder.FormatRetention.NONE);

        // Now process the message body, splitting on keywords
        Matcher matcher = KEYWORD_PATTERN.matcher(rawMessage);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append any text before this keyword
            if (matcher.start() > lastEnd) {
                String before = rawMessage.substring(lastEnd, matcher.start());
                builder.append(TextComponent.fromLegacyText(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', before)),
                        ComponentBuilder.FormatRetention.NONE);
            }

            String keyword = matcher.group(1).toLowerCase();
            switch (keyword) {
                case "item":
                    builder.append(buildItemComponent(player), ComponentBuilder.FormatRetention.NONE);
                    break;
                case "inv":
                case "inventory":
                    builder.append(buildInventoryComponent(player, false), ComponentBuilder.FormatRetention.NONE);
                    break;
                case "enderchest":
                case "ec":
                    builder.append(buildInventoryComponent(player, true), ComponentBuilder.FormatRetention.NONE);
                    break;
            }

            lastEnd = matcher.end();
        }

        // Append any trailing text after the last keyword
        if (lastEnd < rawMessage.length()) {
            String after = rawMessage.substring(lastEnd);
            builder.append(TextComponent.fromLegacyText(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', after)),
                    ComponentBuilder.FormatRetention.NONE);
        }

        return builder.create();
    }

    /**
     * Returns a plain-text description of the keywords for Discord relay.
     * Replaces [item] with the item name, [inv]/[enderchest] with summaries.
     */
    public String buildDiscordLine(Player player, String rawMessage) {
        Matcher matcher = KEYWORD_PATTERN.matcher(rawMessage);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String keyword = matcher.group(1).toLowerCase();
            String replacement;
            switch (keyword) {
                case "item":
                    replacement = getItemDiscordText(player);
                    break;
                case "inv":
                case "inventory":
                    replacement = "\uD83C\uDF92 [Inventory]";
                    break;
                case "enderchest":
                case "ec":
                    replacement = "\uD83D\uDC9C [Ender Chest]";
                    break;
                default:
                    replacement = matcher.group(0);
                    break;
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Retrieves and opens a snapshot inventory for the given snapshot UUID.
     * Returns {@code null} if the snapshot has expired or doesn't exist.
     */
    public Inventory getSnapshot(UUID snapshotId) {
        SnapshotEntry entry = snapshots.get(snapshotId);
        return entry != null ? entry.inventory : null;
    }

    /**
     * Returns the snapshot title (used when opening the GUI).
     */
    public String getSnapshotTitle(UUID snapshotId) {
        SnapshotEntry entry = snapshots.get(snapshotId);
        return entry != null ? entry.title : null;
    }

    // ── Item component ──────────────────────────────────────────────────────

    private BaseComponent[] buildItemComponent(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            // Nothing in hand — show placeholder
            TextComponent comp = new TextComponent("[Nothing]");
            comp.setColor(ChatColor.GRAY);
            comp.setItalic(true);
            return new BaseComponent[]{comp};
        }

        String displayName = getItemDisplayName(item);
        int amount = item.getAmount();

        // Build the visible text: [Item Name x64]
        String label = org.bukkit.ChatColor.AQUA + "[" + displayName;
        if (amount > 1) {
            label += " x" + amount;
        }
        label += "]";

        TextComponent comp = new TextComponent(TextComponent.fromLegacyText(label));

        // Hover: show item details (name, lore, enchants)
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(buildItemHoverText(item))));

        // Click: open a 1-slot inventory showing the item
        UUID snapId = createItemSnapshot(player, item);
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/qol:viewsnapshot " + snapId));

        return new BaseComponent[]{comp};
    }

    private BaseComponent[] buildItemHoverText(ItemStack item) {
        ComponentBuilder hover = new ComponentBuilder();

        // Item name (with colour)
        String name = getItemDisplayName(item);
        hover.append(TextComponent.fromLegacyText(org.bukkit.ChatColor.AQUA + name),
                ComponentBuilder.FormatRetention.NONE);

        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;

        // Enchantments
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (meta != null && meta.hasEnchants()) {
            enchants = new HashMap<>(enchants);
            enchants.putAll(meta.getEnchants());
        }
        if (!enchants.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
                hover.append("\n", ComponentBuilder.FormatRetention.NONE);
                String enchName = formatEnchantmentName(e.getKey());
                hover.append(TextComponent.fromLegacyText(
                        org.bukkit.ChatColor.GRAY + enchName + " " + toRoman(e.getValue())),
                        ComponentBuilder.FormatRetention.NONE);
            }
        }

        // Lore
        if (meta != null && meta.hasLore()) {
            for (String line : meta.getLore()) {
                hover.append("\n", ComponentBuilder.FormatRetention.NONE);
                hover.append(TextComponent.fromLegacyText(
                        org.bukkit.ChatColor.DARK_PURPLE + "" + org.bukkit.ChatColor.ITALIC + line),
                        ComponentBuilder.FormatRetention.NONE);
            }
        }

        // Amount & type
        hover.append("\n", ComponentBuilder.FormatRetention.NONE);
        hover.append(TextComponent.fromLegacyText(
                org.bukkit.ChatColor.DARK_GRAY + item.getType().name() + " x" + item.getAmount()),
                ComponentBuilder.FormatRetention.NONE);

        return hover.create();
    }

    // ── Inventory / Ender Chest component ───────────────────────────────────

    private BaseComponent[] buildInventoryComponent(Player player, boolean enderChest) {
        String label;
        String title;
        Inventory source;

        if (enderChest) {
            label = org.bukkit.ChatColor.LIGHT_PURPLE + "[Ender Chest]";
            title = player.getName() + "'s Ender Chest";
            source = player.getEnderChest();
        } else {
            label = org.bukkit.ChatColor.GREEN + "[Inventory]";
            title = player.getName() + "'s Inventory";
            source = player.getInventory();
        }

        // Create a frozen snapshot
        UUID snapId = createInventorySnapshot(source, title);

        TextComponent comp = new TextComponent(TextComponent.fromLegacyText(label));

        // Hover: summary of contents
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(buildInventoryHoverText(source, enderChest, player.getName()))));

        // Click: open the snapshot
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/qol:viewsnapshot " + snapId));

        return new BaseComponent[]{comp};
    }

    private BaseComponent[] buildInventoryHoverText(Inventory inv, boolean enderChest, String playerName) {
        ComponentBuilder hover = new ComponentBuilder();

        String headerColor = enderChest
                ? org.bukkit.ChatColor.LIGHT_PURPLE.toString()
                : org.bukkit.ChatColor.GREEN.toString();
        hover.append(TextComponent.fromLegacyText(
                headerColor + (enderChest ? "Ender Chest" : "Inventory") + " of " + playerName),
                ComponentBuilder.FormatRetention.NONE);

        // Count non-air items
        int usedSlots = 0;
        int totalSlots = inv.getSize();
        Map<String, Integer> itemCounts = new HashMap<>();

        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                usedSlots++;
                String name = getItemDisplayName(stack);
                itemCounts.merge(name, stack.getAmount(), Integer::sum);
            }
        }

        hover.append("\n", ComponentBuilder.FormatRetention.NONE);
        hover.append(TextComponent.fromLegacyText(
                org.bukkit.ChatColor.GRAY + "Slots used: " + usedSlots + "/" + totalSlots),
                ComponentBuilder.FormatRetention.NONE);

        // Show top 5 items
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemCounts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int shown = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (shown >= 5) break;
            hover.append("\n", ComponentBuilder.FormatRetention.NONE);
            hover.append(TextComponent.fromLegacyText(
                    org.bukkit.ChatColor.GRAY + "  " + entry.getKey() + " x" + entry.getValue()),
                    ComponentBuilder.FormatRetention.NONE);
            shown++;
        }

        if (itemCounts.size() > 5) {
            hover.append("\n", ComponentBuilder.FormatRetention.NONE);
            hover.append(TextComponent.fromLegacyText(
                    org.bukkit.ChatColor.DARK_GRAY + "  ..." + (itemCounts.size() - 5) + " more types"),
                    ComponentBuilder.FormatRetention.NONE);
        }

        hover.append("\n\n", ComponentBuilder.FormatRetention.NONE);
        hover.append(TextComponent.fromLegacyText(
                org.bukkit.ChatColor.YELLOW + "Click to view"),
                ComponentBuilder.FormatRetention.NONE);

        return hover.create();
    }

    // ── Snapshot management ─────────────────────────────────────────────────

    private UUID createItemSnapshot(Player player, ItemStack item) {
        UUID id = UUID.randomUUID();
        String title = getItemDisplayName(item);
        // Single-slot chest that shows the item
        Inventory inv = Bukkit.createInventory(null, 27,
                org.bukkit.ChatColor.DARK_AQUA + title);
        inv.setItem(13, item.clone()); // Centre slot
        snapshots.put(id, new SnapshotEntry(inv, title));
        return id;
    }

    private UUID createInventorySnapshot(Inventory source, String title) {
        UUID id = UUID.randomUUID();
        // Clone into a chest inventory matching the source size (rounded up to multiple of 9)
        int size = source.getSize();
        // Clamp to max 54 (double chest)
        if (size > 54) size = 54;
        // Round up to nearest 9
        if (size % 9 != 0) size = ((size / 9) + 1) * 9;
        if (size < 9) size = 9;

        Inventory snapshot = Bukkit.createInventory(null, size,
                org.bukkit.ChatColor.DARK_AQUA + title);
        ItemStack[] contents = source.getContents();
        for (int i = 0; i < Math.min(contents.length, size); i++) {
            if (contents[i] != null) {
                snapshot.setItem(i, contents[i].clone());
            }
        }
        snapshots.put(id, new SnapshotEntry(snapshot, title));
        return id;
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        // Pretty-print material name: DIAMOND_SWORD → Diamond Sword
        String name = item.getType().name();
        StringBuilder sb = new StringBuilder();
        boolean capitalise = true;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                capitalise = true;
            } else {
                sb.append(capitalise ? Character.toUpperCase(c) : Character.toLowerCase(c));
                capitalise = false;
            }
        }
        return sb.toString();
    }

    private String getItemDiscordText(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return "[Nothing]";
        }
        String name = org.bukkit.ChatColor.stripColor(getItemDisplayName(item));
        int amount = item.getAmount();
        return "\uD83D\uDFEB [" + name + (amount > 1 ? " x" + amount : "") + "]";
    }

    private static String formatEnchantmentName(Enchantment ench) {
        // getKey().getKey() → "sharpness", "fire_aspect"  →  "Sharpness", "Fire Aspect"
        String raw = ench.getKey().getKey();
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                cap = true;
            } else {
                sb.append(cap ? Character.toUpperCase(c) : c);
                cap = false;
            }
        }
        return sb.toString();
    }

    private static String toRoman(int value) {
        if (value <= 0 || value > 10) return String.valueOf(value);
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return numerals[value];
    }

    // ── Snapshot entry ──────────────────────────────────────────────────────

    private static class SnapshotEntry {
        final Inventory inventory;
        final String title;
        final long createdAt;

        SnapshotEntry(Inventory inventory, String title) {
            this.inventory = inventory;
            this.title = title;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
