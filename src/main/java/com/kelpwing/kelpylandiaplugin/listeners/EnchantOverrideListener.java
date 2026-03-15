package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Overrides vanilla enchantment conflict rules in the anvil:
 * <ul>
 *   <li>Infinity + Mending on bows and crossbows</li>
 *   <li>Multishot + Piercing on crossbows</li>
 *   <li>Sharpness + Smite (+ Bane of Arthropods) on swords and axes — any combo</li>
 *   <li>Sword enchantments (Sharpness, Smite, Bane, Looting, Fire Aspect, Sweeping Edge, Knockback) on tridents</li>
 *   <li>Fortune + Silk Touch remains BLOCKED</li>
 * </ul>
 */
public class EnchantOverrideListener implements Listener {

    private final KelpylandiaPlugin plugin;

    /** Enchantments that are normally exclusive damage group on swords/axes. */
    private static final Set<Enchantment> DAMAGE_GROUP = new HashSet<>(Arrays.asList(
            Enchantment.DAMAGE_ALL,        // Sharpness
            Enchantment.DAMAGE_UNDEAD,     // Smite
            Enchantment.DAMAGE_ARTHROPODS  // Bane of Arthropods
    ));

    /** Sword enchantments we also allow on tridents. */
    private static final Set<Enchantment> SWORD_ENCHANTS = new HashSet<>(Arrays.asList(
            Enchantment.DAMAGE_ALL,
            Enchantment.DAMAGE_UNDEAD,
            Enchantment.DAMAGE_ARTHROPODS,
            Enchantment.LOOT_BONUS_MOBS,   // Looting
            Enchantment.FIRE_ASPECT,
            Enchantment.KNOCKBACK
    ));

    // Try to add Sweeping Edge if it exists (it's not present on all versions)
    static {
        try {
            Enchantment sweep = Enchantment.SWEEPING_EDGE;
            if (sweep != null) SWORD_ENCHANTS.add(sweep);
        } catch (NoSuchFieldError ignored) {
            // Pre-1.11.1 — no sweeping edge
        }
    }

    public EnchantOverrideListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (left == null || right == null) return;
        if (left.getType() == Material.AIR || right.getType() == Material.AIR) return;

        // Vanilla already computed a result (or null if it thinks it's invalid)
        // We recompute to allow our custom combinations
        ItemStack result = computeResult(left, right);
        if (result != null) {
            event.setResult(result);
        }
    }

    /**
     * Computes a merged result, allowing our custom enchantment combinations.
     * Returns null if no override is needed (vanilla handles it fine, or it's genuinely invalid).
     */
    private ItemStack computeResult(ItemStack left, ItemStack right) {
        Material itemType = left.getType();

        // Gather enchantments from the right item (could be a book or same-type item)
        Map<Enchantment, Integer> rightEnchants = getEnchantments(right);
        if (rightEnchants.isEmpty()) return null;

        Map<Enchantment, Integer> leftEnchants = new HashMap<>(left.getEnchantments());

        // Check if any of our overrides are relevant
        boolean needsOverride = false;
        for (Enchantment ench : rightEnchants.keySet()) {
            if (wouldBeBlockedByVanilla(ench, leftEnchants.keySet(), itemType)) {
                needsOverride = true;
                break;
            }
            // Also check if a right enchant wouldn't normally apply to this item type (trident case)
            if (isTrident(itemType) && SWORD_ENCHANTS.contains(ench) && !ench.canEnchantItem(left)) {
                needsOverride = true;
                break;
            }
        }

        if (!needsOverride) return null; // Let vanilla handle it

        // Build merged enchantment map
        Map<Enchantment, Integer> merged = new HashMap<>(leftEnchants);

        for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int rightLevel = entry.getValue();

            // HARD BLOCK: Fortune + Silk Touch stays forbidden
            if (isFortuneSilkConflict(ench, merged.keySet())) continue;

            // Check if this enchant can go on this item type (with our extensions)
            if (!canApplyTo(ench, itemType, left)) continue;

            // Merge levels
            if (merged.containsKey(ench)) {
                int leftLevel = merged.get(ench);
                if (leftLevel == rightLevel) {
                    merged.put(ench, Math.min(leftLevel + 1, ench.getMaxLevel()));
                } else {
                    merged.put(ench, Math.max(leftLevel, rightLevel));
                }
            } else {
                merged.put(ench, rightLevel);
            }
        }

        // If merged is the same as left enchants, no change needed
        if (merged.equals(leftEnchants)) return null;

        // Build result item
        ItemStack result = left.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return null;

        // Clear existing enchants and re-apply merged set
        for (Enchantment e : new ArrayList<>(result.getEnchantments().keySet())) {
            result.removeEnchantment(e);
        }
        for (Map.Entry<Enchantment, Integer> entry : merged.entrySet()) {
            result.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Checks if vanilla would block this enchantment from being combined.
     */
    private boolean wouldBeBlockedByVanilla(Enchantment adding, Set<Enchantment> existing, Material itemType) {
        // Infinity vs Mending
        if (adding == Enchantment.ARROW_INFINITE && existing.contains(Enchantment.MENDING)) return true;
        if (adding == Enchantment.MENDING && existing.contains(Enchantment.ARROW_INFINITE)) return true;

        // Multishot vs Piercing
        if (adding == Enchantment.MULTISHOT && existing.contains(Enchantment.PIERCING)) return true;
        if (adding == Enchantment.PIERCING && existing.contains(Enchantment.MULTISHOT)) return true;

        // Damage group (sharpness/smite/bane) mutual exclusion
        if (DAMAGE_GROUP.contains(adding)) {
            for (Enchantment e : existing) {
                if (DAMAGE_GROUP.contains(e) && !e.equals(adding)) return true;
            }
        }

        return false;
    }

    /**
     * Hard-blocked: fortune + silk touch.
     */
    private boolean isFortuneSilkConflict(Enchantment adding, Set<Enchantment> existing) {
        if (adding == Enchantment.LOOT_BONUS_BLOCKS && existing.contains(Enchantment.SILK_TOUCH)) return true;
        if (adding == Enchantment.SILK_TOUCH && existing.contains(Enchantment.LOOT_BONUS_BLOCKS)) return true;
        return false;
    }

    /**
     * Determines if an enchantment can be applied to a given item type,
     * including our custom extensions (sword enchants on tridents, etc.).
     */
    private boolean canApplyTo(Enchantment ench, Material itemType, ItemStack item) {
        // Vanilla check first
        if (ench.canEnchantItem(item)) return true;

        // --- Custom extensions ---

        // Allow Infinity and Mending on bows and crossbows
        if ((isBow(itemType) || isCrossbow(itemType)) &&
            (ench == Enchantment.ARROW_INFINITE || ench == Enchantment.MENDING)) {
            return true;
        }

        // Allow Multishot + Piercing on crossbows
        if (isCrossbow(itemType) &&
            (ench == Enchantment.MULTISHOT || ench == Enchantment.PIERCING)) {
            return true;
        }

        // Allow sword enchantments on tridents
        if (isTrident(itemType) && SWORD_ENCHANTS.contains(ench)) {
            return true;
        }

        // Allow damage group combos on swords and axes
        if ((isSword(itemType) || isAxe(itemType)) && DAMAGE_GROUP.contains(ench)) {
            return true;
        }

        return false;
    }

    /** Extract enchantments from an item or enchanted book. */
    private Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK && item.getItemMeta() instanceof EnchantmentStorageMeta bookMeta) {
            return bookMeta.getStoredEnchants();
        }
        return item.getEnchantments();
    }

    // ─── Material helpers ───────────────────────────────────

    private boolean isBow(Material m) {
        return m == Material.BOW;
    }

    private boolean isCrossbow(Material m) {
        return m.name().equals("CROSSBOW"); // Safe for 1.16+
    }

    private boolean isTrident(Material m) {
        return m == Material.TRIDENT;
    }

    private boolean isSword(Material m) {
        String name = m.name();
        return name.endsWith("_SWORD");
    }

    private boolean isAxe(Material m) {
        String name = m.name();
        return name.endsWith("_AXE");
    }
}
