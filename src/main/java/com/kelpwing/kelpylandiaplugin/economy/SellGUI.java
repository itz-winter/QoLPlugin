package com.kelpwing.kelpylandiaplugin.economy;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.economy.commands.SellCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 54-slot sell GUI: players put items in, on close all sellable items are sold.
 * Shulker boxes: contents are sold, empty box is returned (or with unsellable contents).
 * Unsellable items are returned to the player. If inventory is full, items drop at feet.
 */
public class SellGUI implements Listener {

    private final KelpylandiaPlugin plugin;

    private static final String GUI_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Sell Items";
    private static final int GUI_SIZE = 54; // 6 rows

    // Track which players have the sell GUI open
    private final Set<UUID> openGUIs = new HashSet<>();

    public SellGUI(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the sell GUI for a player.
     */
    public void openGUI(Player player) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) {
            player.sendMessage(ChatColor.RED + "The economy system is disabled.");
            return;
        }
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        openGUIs.add(player.getUniqueId());
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (!openGUIs.remove(player.getUniqueId())) return;

        // Check title matches
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null || !eco.isEnabled()) return;

        // Process all items in the GUI
        Inventory gui = event.getInventory();
        BigDecimal totalEarnings = BigDecimal.ZERO;
        int totalItemsSold = 0;
        List<ItemStack> unsellableReturns = new ArrayList<>();

        for (int slot = 0; slot < gui.getSize(); slot++) {
            ItemStack item = gui.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            // ── Shulker box handling ──────────────────────────────
            if (EconomyManager.isShulkerBox(item.getType()) && item.hasItemMeta()
                    && item.getItemMeta() instanceof BlockStateMeta) {
                ShulkerResult result = processShulkerBox(eco, item);
                totalEarnings = totalEarnings.add(result.earnings);
                totalItemsSold += result.itemsSold;

                // Return the box with unsellable contents (or empty box if all sold)
                if (result.returnBox != null) {
                    unsellableReturns.add(result.returnBox);
                }
                continue;
            }

            // ── Normal item ──────────────────────────────────────
            EconomyManager.PriceResult priceResult = eco.getPrice(item.getType());
            if (priceResult.sellable) {
                BigDecimal value = priceResult.price.multiply(BigDecimal.valueOf(item.getAmount()));
                totalEarnings = totalEarnings.add(value);
                totalItemsSold += item.getAmount();
            } else {
                // Return unsellable item
                unsellableReturns.add(item.clone());
            }
        }

        // Apply tax if configured
        boolean taxApplied = eco.isTaxEnabled() && eco.isTaxOnServerSell();
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal netEarnings = totalEarnings;
        if (taxApplied && totalEarnings.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = eco.calculateTax(totalEarnings);
            netEarnings = eco.afterTax(totalEarnings);
        }

        // Deposit earnings
        if (netEarnings.compareTo(BigDecimal.ZERO) > 0) {
            eco.deposit(player.getUniqueId(), netEarnings);
        }

        // Return unsellable items
        returnItems(player, unsellableReturns);

        // Send summary to player
        if (totalItemsSold > 0) {
            String formatted = eco.formatMoney(netEarnings);
            player.sendMessage(eco.getMessage("sell-gui-success")
                    .replace("{amount}", String.valueOf(totalItemsSold))
                    .replace("{total}", formatted)
                    .replace("{unit}", eco.getUnit()));

            if (taxApplied && taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                player.sendMessage(eco.getMessage("sell-tax")
                        .replace("{rate}", eco.getTaxRateDisplay())
                        .replace("{tax}", eco.formatMoney(taxAmount))
                        .replace("{unit}", eco.getUnit()));
            }
        } else if (!unsellableReturns.isEmpty()) {
            player.sendMessage(ChatColor.RED + "None of those items could be sold. Unsellable items returned.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You didn't put anything in the sell GUI.");
        }
    }

    /**
     * Process a shulker box: sell sellable contents, return box with unsellable contents.
     */
    private ShulkerResult processShulkerBox(EconomyManager eco, ItemStack shulkerItem) {
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

        BigDecimal earnings = BigDecimal.ZERO;
        int itemsSold = 0;
        boolean hasUnsellable = false;

        ItemStack[] contents = shulker.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content == null || content.getType() == Material.AIR) continue;

            EconomyManager.PriceResult result = eco.getPrice(content.getType());
            if (result.sellable) {
                earnings = earnings.add(result.price.multiply(BigDecimal.valueOf(content.getAmount())));
                itemsSold += content.getAmount();
                contents[i] = null; // remove sold item
            } else {
                hasUnsellable = true;
            }
        }

        // If everything was sold, check if the empty shulker box itself is sellable
        if (!hasUnsellable && itemsSold > 0) {
            // Return empty box — check if the box itself is sellable
            EconomyManager.PriceResult boxResult = eco.getPrice(shulkerItem.getType());
            if (boxResult.sellable) {
                // Sell the empty shulker box too
                earnings = earnings.add(boxResult.price);
                itemsSold++;
                return new ShulkerResult(earnings, itemsSold, null); // nothing to return
            }
            // Box not sellable — return it empty
            shulker.getInventory().clear();
            meta.setBlockState(shulker);
            shulkerItem.setItemMeta(meta);
            return new ShulkerResult(earnings, itemsSold, shulkerItem);
        }

        if (hasUnsellable || itemsSold == 0) {
            // Put remaining contents back and return the box
            shulker.getInventory().setContents(contents);
            meta.setBlockState(shulker);
            shulkerItem.setItemMeta(meta);
            return new ShulkerResult(earnings, itemsSold, shulkerItem);
        }

        return new ShulkerResult(earnings, itemsSold, null);
    }

    /**
     * Return items to a player's inventory, dropping any that don't fit.
     */
    private void returnItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    /**
     * Check if a player has the sell GUI open (for preventing exploits).
     */
    public boolean hasGUIOpen(UUID uuid) {
        return openGUIs.contains(uuid);
    }

    /**
     * Result container for shulker box processing.
     */
    private static class ShulkerResult {
        final BigDecimal earnings;
        final int itemsSold;
        final ItemStack returnBox; // null if nothing to return

        ShulkerResult(BigDecimal earnings, int itemsSold, ItemStack returnBox) {
            this.earnings = earnings;
            this.itemsSold = itemsSold;
            this.returnBox = returnBox;
        }
    }
}
