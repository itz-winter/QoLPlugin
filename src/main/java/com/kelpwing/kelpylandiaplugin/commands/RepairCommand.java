package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * /repair [hand|helmet|chestplate|pants|boots|offhand|all] [user]
 * Defaults to "hand" if no slot specified.
 */
public class RepairCommand implements CommandExecutor, TabCompleter {

    private static final Set<String> VALID_SLOTS = new LinkedHashSet<>(
        Arrays.asList("hand", "helmet", "chestplate", "pants", "boots", "offhand", "all")
    );

    private final KelpylandiaPlugin plugin;

    public RepairCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        String slot = args.length >= 1 ? args[0].toLowerCase() : "hand";
        if (!VALID_SLOTS.contains(slot)) {
            sender.sendMessage(ChatColor.RED + "Invalid slot. Valid: hand, helmet, chestplate, pants, boots, offhand, all");
            return true;
        }

        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("kelpylandia.repair.others")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to repair other players' items.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else {
            target = senderPlayer;
        }

        PlayerInventory inv = target.getInventory();
        int repaired = 0;

        if (slot.equals("all")) {
            // Repair everything: main inventory (hotbar + storage), armor, and offhand
            for (ItemStack item : inv.getContents()) {
                repaired += repairItem(item);
            }
            for (ItemStack item : inv.getArmorContents()) {
                repaired += repairItem(item);
            }
            repaired += repairItem(inv.getItemInOffHand());
        } else {
            ItemStack item = getSlotItem(inv, slot);
            repaired += repairItem(item);
        }

        if (repaired == 0) {
            senderPlayer.sendMessage(ChatColor.YELLOW + "No repairable items found in the specified slot(s).");
        } else {
            String targetName = target.equals(senderPlayer) ? "your" : target.getName() + "'s";
            senderPlayer.sendMessage(ChatColor.GREEN + "Repaired " + repaired + " item(s) in " + targetName + " " + (slot.equals("all") ? "inventory" : slot) + ".");
            if (!target.equals(senderPlayer)) {
                target.sendMessage(ChatColor.GREEN + senderPlayer.getName() + " repaired " + repaired + " of your item(s).");
            }
        }
        return true;
    }

    private ItemStack getSlotItem(PlayerInventory inv, String slot) {
        switch (slot) {
            case "hand": return inv.getItemInMainHand();
            case "helmet": return inv.getHelmet();
            case "chestplate": return inv.getChestplate();
            case "pants": return inv.getLeggings();
            case "boots": return inv.getBoots();
            case "offhand": return inv.getItemInOffHand();
            default: return null;
        }
    }

    /**
     * Repairs a single item. Returns 1 if repaired, 0 if not applicable.
     */
    private int repairItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            if (damageable.getDamage() > 0) {
                damageable.setDamage(0);
                item.setItemMeta(meta);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String s : VALID_SLOTS) {
                if (s.startsWith(prefix)) matches.add(s);
            }
            return matches;
        }
        if (args.length == 2 && sender.hasPermission("kelpylandia.repair.others")) {
            String prefix = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) names.add(p.getName());
            }
            Collections.sort(names);
            return names;
        }
        return Collections.emptyList();
    }
}
