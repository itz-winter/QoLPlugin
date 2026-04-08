package com.kelpwing.kelpylandiaplugin.teleport.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.teleport.BackManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /back - Teleport to your previous location (before your last teleport).
 */
public class BackCommand implements CommandExecutor {

    private final KelpylandiaPlugin plugin;

    public BackCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("qol.back")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        BackManager backManager = plugin.getBackManager();
        if (backManager == null) {
            player.sendMessage(ChatColor.RED + "The back system is currently disabled.");
            return true;
        }

        if (!backManager.hasPreviousLocation(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have a previous location to return to.");
            return true;
        }

        // Check cooldown
        if (!player.hasPermission("qol.back.bypass.cooldown") && backManager.isOnCooldown(player.getUniqueId())) {
            int remaining = backManager.getRemainingCooldown(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.GOLD + remaining + "s" + ChatColor.RED + " before using /back again.");
            return true;
        }

        Location previousLocation = backManager.getPreviousLocation(player.getUniqueId());

        // Save current location as the new "previous" so they can /back again to swap
        backManager.savePreviousLocation(player.getUniqueId(), player.getLocation());

        // Teleport
        player.teleport(previousLocation);
        backManager.applyCooldown(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Teleported to your previous location.");
        return true;
    }
}
