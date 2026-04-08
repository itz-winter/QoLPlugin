package com.kelpwing.kelpylandiaplugin.moderation.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InvseeCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;
    private final Map<UUID, UUID> viewingInventories; // viewer UUID -> target UUID
    private final Map<UUID, String> inventoryTypes; // viewer UUID -> inventory type
    
    public InvseeCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.viewingInventories = new HashMap<>();
        this.inventoryTypes = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player admin = (Player) sender;
        
        if (!admin.hasPermission("qol.invsee")) {
            admin.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            admin.sendMessage(ChatColor.RED + "Usage: /invsee <player> [inv|armor|ender]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            admin.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        String inventoryType = "inv";
        if (args.length >= 2) {
            inventoryType = args[1].toLowerCase();
        }

        switch (inventoryType) {
            case "inv":
            case "inventory":
                openPlayerInventory(admin, target);
                break;
            case "armor":
            case "armour":
                openPlayerArmor(admin, target);
                break;
            case "ender":
            case "enderchest":
                openPlayerEnderChest(admin, target);
                break;
            default:
                admin.sendMessage(ChatColor.RED + "Invalid inventory type. Use: inv, armor, or ender");
                return true;
        }

        return true;
    }

    private void openPlayerInventory(Player admin, Player target) {
        // Create a custom inventory that mirrors the player's inventory
        Inventory customInv = Bukkit.createInventory(null, 54, 
            ChatColor.DARK_PURPLE + target.getName() + "'s Inventory");
        
        // Copy main inventory (slots 0-35)
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (i < 36) {
                customInv.setItem(i, contents[i]);
            }
        }
        
        // Copy hotbar (slots 0-8 in player inventory become 36-44 in custom inventory)
        for (int i = 0; i < 9; i++) {
            customInv.setItem(36 + i, target.getInventory().getItem(i));
        }
        
        // Add armor in slots 45-48
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack armorPiece = armor[i];
            if (armorPiece != null) {
                ItemMeta meta = armorPiece.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                    lore.add(0, ChatColor.GRAY + "Armor Slot: " + getArmorSlotName(i));
                    meta.setLore(lore);
                    armorPiece.setItemMeta(meta);
                }
            }
            customInv.setItem(45 + i, armorPiece);
        }
        
        // Add offhand in slot 49
        ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                lore.add(0, ChatColor.GRAY + "Offhand Slot");
                meta.setLore(lore);
                offhand.setItemMeta(meta);
            }
        }
        customInv.setItem(49, offhand);
        
        // Fill empty slots with barrier blocks for clarity
        for (int i = 50; i < 54; i++) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Empty Slot");
            barrier.setItemMeta(meta);
            customInv.setItem(i, barrier);
        }
        
        viewingInventories.put(admin.getUniqueId(), target.getUniqueId());
        inventoryTypes.put(admin.getUniqueId(), "inventory");
        
        admin.openInventory(customInv);
        admin.sendMessage(ChatColor.GREEN + "Opened " + target.getName() + "'s inventory. Changes will be applied in real-time!");
    }

    private void openPlayerArmor(Player admin, Player target) {
        Inventory armorInv = Bukkit.createInventory(null, 9, 
            ChatColor.DARK_RED + target.getName() + "'s Armor");
        
        // Set armor pieces
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            armorInv.setItem(i, armor[i]);
        }
        
        // Add offhand
        armorInv.setItem(4, target.getInventory().getItemInOffHand());
        
        // Fill remaining slots with barriers
        for (int i = 5; i < 9; i++) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Empty Slot");
            barrier.setItemMeta(meta);
            armorInv.setItem(i, barrier);
        }
        
        viewingInventories.put(admin.getUniqueId(), target.getUniqueId());
        inventoryTypes.put(admin.getUniqueId(), "armor");
        
        admin.openInventory(armorInv);
        admin.sendMessage(ChatColor.GREEN + "Opened " + target.getName() + "'s armor. Changes will be applied in real-time!");
    }

    private void openPlayerEnderChest(Player admin, Player target) {
        Inventory enderInv = target.getEnderChest();
        
        viewingInventories.put(admin.getUniqueId(), target.getUniqueId());
        inventoryTypes.put(admin.getUniqueId(), "ender");
        
        admin.openInventory(enderInv);
        admin.sendMessage(ChatColor.GREEN + "Opened " + target.getName() + "'s ender chest. Changes will be applied in real-time!");
    }

    private String getArmorSlotName(int slot) {
        switch (slot) {
            case 0: return "Boots";
            case 1: return "Leggings";
            case 2: return "Chestplate";
            case 3: return "Helmet";
            default: return "Unknown";
        }
    }

    public void handleInventoryClick(Player admin, Inventory inventory, int slot, ItemStack currentItem, ItemStack cursor) {
        UUID targetUUID = viewingInventories.get(admin.getUniqueId());
        String invType = inventoryTypes.get(admin.getUniqueId());
        
        if (targetUUID == null || invType == null) return;
        
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) {
            admin.sendMessage(ChatColor.RED + "Target player is no longer online!");
            admin.closeInventory();
            return;
        }

        // Schedule the update for next tick to ensure the GUI click is processed first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack newItem = inventory.getItem(slot);
            
            switch (invType) {
                case "inventory":
                    handleInventoryUpdate(target, slot, newItem);
                    break;
                case "armor":
                    handleArmorUpdate(target, slot, newItem);
                    break;
                case "ender":
                    // Ender chest updates automatically since we're directly editing it
                    break;
            }

            // Log the action
            plugin.getLogger().info(String.format("Admin %s modified %s's %s inventory (slot %d)", 
                admin.getName(), target.getName(), invType, slot));
        }, 1L);
    }

    private void handleInventoryUpdate(Player target, int slot, ItemStack newItem) {
        if (slot < 36) {
            // Main inventory slots (9-35 are the main inventory in Minecraft)
            // But our custom inventory has them in order 0-35
            if (slot >= 9) {
                target.getInventory().setItem(slot, newItem);
            } else {
                // Slots 0-8 in our GUI are actually the hotbar (0-8) in player inventory
                target.getInventory().setItem(slot, newItem);
            }
        } else if (slot >= 36 && slot < 45) {
            // Hotbar slots (36-44 in custom inventory = 0-8 in player inventory)
            target.getInventory().setItem(slot - 36, newItem);
        } else if (slot >= 45 && slot < 49) {
            // Armor slots
            ItemStack[] armor = target.getInventory().getArmorContents();
            if (newItem != null && newItem.getType() != Material.AIR) {
                // Remove lore we added for display
                ItemStack cleanItem = newItem.clone();
                ItemMeta meta = cleanItem.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    lore.removeIf(line -> line.contains("Armor Slot:"));
                    if (lore.isEmpty()) {
                        meta.setLore(null);
                    } else {
                        meta.setLore(lore);
                    }
                    cleanItem.setItemMeta(meta);
                }
                armor[slot - 45] = cleanItem;
            } else {
                armor[slot - 45] = newItem;
            }
            target.getInventory().setArmorContents(armor);
        } else if (slot == 49) {
            // Offhand slot
            ItemStack cleanItem = newItem;
            if (newItem != null && newItem.getType() != Material.AIR) {
                // Remove lore we added for display
                cleanItem = newItem.clone();
                ItemMeta meta = cleanItem.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    lore.removeIf(line -> line.contains("Offhand Slot"));
                    if (lore.isEmpty()) {
                        meta.setLore(null);
                    } else {
                        meta.setLore(lore);
                    }
                    cleanItem.setItemMeta(meta);
                }
            }
            target.getInventory().setItemInOffHand(cleanItem);
        }
        
        target.updateInventory();
    }

    private void handleArmorUpdate(Player target, int slot, ItemStack newItem) {
        if (slot < 4) {
            // Armor slots
            ItemStack[] armor = target.getInventory().getArmorContents();
            armor[slot] = newItem;
            target.getInventory().setArmorContents(armor);
        } else if (slot == 4) {
            // Offhand slot
            target.getInventory().setItemInOffHand(newItem);
        }
        
        target.updateInventory();
    }

    public void handleInventoryClose(Player admin) {
        viewingInventories.remove(admin.getUniqueId());
        inventoryTypes.remove(admin.getUniqueId());
    }

    public boolean isViewingInventory(Player admin) {
        return viewingInventories.containsKey(admin.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // Inventory types
            String[] types = {"inv", "inventory", "armor", "armour", "ender", "enderchest"};
            for (String type : types) {
                if (type.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(type);
                }
            }
        }
        
        return completions;
    }
}
