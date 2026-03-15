package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.List;

/**
 * /hat - Place the item in your hand on your head.
 * If you're already wearing a helmet, it will be swapped with the item in your hand.
 */
public class HatCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public HatCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("kelpylandia.hat")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack hand = inv.getItemInMainHand();

        if (hand.getType() == Material.AIR) {
            // If hand is empty, remove the helmet and give it to the player
            ItemStack helmet = inv.getHelmet();
            if (helmet == null || helmet.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You must be holding an item to use as a hat.");
                return true;
            }
            inv.setHelmet(null);
            inv.setItemInMainHand(helmet);
            player.sendMessage(ChatColor.GREEN + "Removed your hat.");
            return true;
        }

        // Swap: put hand item on head, put current helmet in hand
        ItemStack currentHelmet = inv.getHelmet();
        
        // Clone the hand item and set amount to 1 for the hat
        ItemStack hatItem = hand.clone();
        hatItem.setAmount(1);

        // Place hat on head
        inv.setHelmet(hatItem);

        // Handle the hand item and old helmet
        if (hand.getAmount() > 1) {
            // Reduce hand stack by 1
            hand.setAmount(hand.getAmount() - 1);
            inv.setItemInMainHand(hand);
            // Return old helmet to inventory if there was one
            if (currentHelmet != null && !currentHelmet.getType().isAir()) {
                for (ItemStack leftover : inv.addItem(currentHelmet).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        } else {
            // Hand only had 1 item; put old helmet (or air) in hand
            if (currentHelmet != null && !currentHelmet.getType().isAir()) {
                inv.setItemInMainHand(currentHelmet);
            } else {
                inv.setItemInMainHand(new ItemStack(Material.AIR));
            }
        }

        player.sendMessage(ChatColor.GREEN + "You are now wearing " + ChatColor.GOLD 
                + formatItemName(hatItem) + ChatColor.GREEN + " as a hat!");
        return true;
    }

    /**
     * Format an item name for display.
     */
    private String formatItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        // Convert MATERIAL_NAME to Material Name
        String name = item.getType().name().replace('_', ' ').toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
