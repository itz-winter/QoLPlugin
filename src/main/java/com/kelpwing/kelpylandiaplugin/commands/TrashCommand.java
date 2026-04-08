package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /trash - Opens a disposable inventory (virtual trash can).
 * All items left in the inventory when closed are permanently destroyed.
 */
public class TrashCommand implements CommandExecutor, TabCompleter, Listener {

    private final KelpylandiaPlugin plugin;

    /** Track which players have the trash GUI open */
    private final Set<UUID> trashUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final String TRASH_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Trash";

    public TrashCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("qol.trash")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Open a 4-row (36 slot) chest inventory as a trash can
        Inventory trash = Bukkit.createInventory(null, 36, TRASH_TITLE);
        trashUsers.add(player.getUniqueId());
        player.openInventory(trash);
        player.sendMessage(ChatColor.GREEN + "Opened " + ChatColor.GOLD + "Trash" + ChatColor.GREEN
                + ". Items will be " + ChatColor.RED + "destroyed" + ChatColor.GREEN + " when you close this menu.");
        return true;
    }

    /**
     * When the trash inventory is closed, all items inside are simply discarded.
     * The inventory contents are not returned to the player.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!trashUsers.remove(player.getUniqueId())) return;

        Inventory inv = event.getInventory();
        // Count how many items were trashed
        int trashed = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null && !inv.getItem(i).getType().isAir()) {
                trashed++;
            }
        }
        inv.clear();

        if (trashed > 0) {
            player.sendMessage(ChatColor.RED + "Destroyed " + ChatColor.GOLD + trashed
                    + ChatColor.RED + " item stack(s).");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
