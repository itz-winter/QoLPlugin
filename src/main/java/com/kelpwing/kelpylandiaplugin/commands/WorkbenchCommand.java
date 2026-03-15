package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.listeners.WorkbenchListener;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Opens virtual workbench inventories via commands, similar to EssentialsX.
 * Supports: Crafting Table, Ender Chest, Anvil, Grindstone, Stonecutter,
 *           Smithing Table, Cartography Table, Loom.
 *
 * On Paper servers, uses native Paper API (player.openAnvil etc.) which provides
 * full functionality including shift-click, recipes, and rename support.
 * On Spigot, falls back to Bukkit.createInventory() which opens the GUI but
 * has limited server-side processing (no shift-click, no recipes).
 *
 * Each workbench type can be individually enabled/disabled in config
 * and has its own permission node.
 */
public class WorkbenchCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;
    private WorkbenchListener workbenchListener;

    public WorkbenchCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Set the workbench listener reference so we can mark virtual opens.
     */
    public void setWorkbenchListener(WorkbenchListener listener) {
        this.workbenchListener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("workbenches.enabled", true)) {
            sender.sendMessage(ChatColor.RED + "Workbench commands are currently disabled.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        // Determine which workbench to open + optional target player
        Player target = player;
        if (args.length >= 1 && player.hasPermission("kelpylandia.workbench.others")) {
            Player found = Bukkit.getPlayer(args[0]);
            if (found != null) {
                target = found;
            } else {
                player.sendMessage(ChatColor.RED + "Player " + ChatColor.GOLD + args[0] + ChatColor.RED + " is not online.");
                return true;
            }
        }

        switch (cmd) {
            case "workbench", "wb", "craft" -> openCraftingTable(player, target);
            case "enderchest", "ec", "echest" -> openEnderChest(player, target);
            case "anvil" -> openAnvil(player, target);
            case "grindstone", "gstone" -> openGrindstone(player, target);
            case "stonecutter", "scutter" -> openStonecutter(player, target);
            case "smithingtable", "smithing" -> openSmithingTable(player, target);
            case "cartographytable", "cartography" -> openCartographyTable(player, target);
            case "loom" -> openLoom(player, target);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown workbench command.");
                return true;
            }
        }

        return true;
    }

    // ===================== Workbench Openers =====================

    private void openCraftingTable(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.craft")) return;
        if (!checkEnabled(opener, "workbenches.craft")) return;

        target.openWorkbench(target.getLocation(), true);
        sendSuccess(opener, target, "Crafting Table");
    }

    private void openEnderChest(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.enderchest")) return;
        if (!checkEnabled(opener, "workbenches.enderchest")) return;

        opener.openInventory(target.getEnderChest());
        sendSuccess(opener, target, "Ender Chest");
    }

    private void openAnvil(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.anvil")) return;
        if (!checkEnabled(opener, "workbenches.anvil")) return;

        if (!openPaperWorkbench(target, "openAnvil")) {
            markVirtual(target);
            Inventory anvil = Bukkit.createInventory(target, InventoryType.ANVIL);
            target.openInventory(anvil);
        }
        sendSuccess(opener, target, "Anvil");
    }

    private void openGrindstone(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.grindstone")) return;
        if (!checkEnabled(opener, "workbenches.grindstone")) return;

        if (!openPaperWorkbench(target, "openGrindstone")) {
            markVirtual(target);
            Inventory grindstone = Bukkit.createInventory(target, InventoryType.GRINDSTONE);
            target.openInventory(grindstone);
        }
        sendSuccess(opener, target, "Grindstone");
    }

    private void openStonecutter(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.stonecutter")) return;
        if (!checkEnabled(opener, "workbenches.stonecutter")) return;

        if (!openPaperWorkbench(target, "openStonecutter")) {
            markVirtual(target);
            Inventory stonecutter = Bukkit.createInventory(target, InventoryType.STONECUTTER);
            target.openInventory(stonecutter);
        }
        sendSuccess(opener, target, "Stonecutter");
    }

    private void openSmithingTable(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.smithing")) return;
        if (!checkEnabled(opener, "workbenches.smithing")) return;

        if (!openPaperWorkbench(target, "openSmithingTable")) {
            markVirtual(target);
            Inventory smithing = Bukkit.createInventory(target, InventoryType.SMITHING);
            target.openInventory(smithing);
        }
        sendSuccess(opener, target, "Smithing Table");
    }

    private void openCartographyTable(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.cartography")) return;
        if (!checkEnabled(opener, "workbenches.cartography")) return;

        if (!openPaperWorkbench(target, "openCartographyTable")) {
            markVirtual(target);
            Inventory cartography = Bukkit.createInventory(target, InventoryType.CARTOGRAPHY);
            target.openInventory(cartography);
        }
        sendSuccess(opener, target, "Cartography Table");
    }

    private void openLoom(Player opener, Player target) {
        if (!checkPerm(opener, "kelpylandia.workbench.loom")) return;
        if (!checkEnabled(opener, "workbenches.loom")) return;

        if (!openPaperWorkbench(target, "openLoom")) {
            markVirtual(target);
            Inventory loom = Bukkit.createInventory(target, InventoryType.LOOM);
            target.openInventory(loom);
        }
        sendSuccess(opener, target, "Loom");
    }

    // ===================== Helpers =====================

    /**
     * Attempt to open a workbench using Paper's native API via reflection.
     * Paper provides methods like player.openAnvil(Location, boolean) which
     * open REAL server-side inventories with full shift-click and recipe support.
     *
     * On Spigot (or if Paper method is unavailable), returns false so the
     * caller can fall back to the Bukkit.createInventory approach.
     *
     * @param target the player to open the workbench for
     * @param methodName the Paper method name (e.g. "openAnvil", "openGrindstone")
     * @return true if Paper API was used successfully, false to use fallback
     */
    private boolean openPaperWorkbench(Player target, String methodName) {
        if (!VersionHelper.isPaper()) return false;
        try {
            Method method = Player.class.getMethod(methodName, Location.class, boolean.class);
            method.invoke(target, null, true);
            return true;
        } catch (Exception e) {
            // Method doesn't exist on this Paper version, fall back
            return false;
        }
    }

    /**
     * Mark the target player as having a virtual workbench open,
     * so WorkbenchListener knows to return items on close.
     */
    private void markVirtual(Player target) {
        if (workbenchListener != null) {
            workbenchListener.markVirtualOpen(target.getUniqueId());
        }
    }

    private boolean checkPerm(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return false;
        }
        return true;
    }

    private boolean checkEnabled(Player player, String configKey) {
        if (!plugin.getConfig().getBoolean(configKey, true)) {
            player.sendMessage(ChatColor.RED + "This workbench command is currently disabled.");
            return false;
        }
        return true;
    }

    private void sendSuccess(Player opener, Player target, String workbenchName) {
        if (opener.equals(target)) {
            opener.sendMessage(ChatColor.GREEN + "Opened " + ChatColor.GOLD + workbenchName + ChatColor.GREEN + ".");
        } else {
            opener.sendMessage(ChatColor.GREEN + "Opened " + ChatColor.GOLD + workbenchName
                + ChatColor.GREEN + " for " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + ".");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        // Only suggest player names if they have the .others permission
        if (args.length == 1 && player.hasPermission("kelpylandia.workbench.others")) {
            String partial = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    names.add(p.getName());
                }
            }
            return names;
        }

        return Collections.emptyList();
    }
}
