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
 * /dback - Teleport to your last death location.
 */
public class DeathBackCommand implements CommandExecutor {

    private final KelpylandiaPlugin plugin;

    public DeathBackCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("qol.dback")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        BackManager backManager = plugin.getBackManager();
        if (backManager == null) {
            player.sendMessage(ChatColor.RED + "The back system is currently disabled.");
            return true;
        }

        if (!backManager.hasDeathLocation(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have a death location to return to.");
            return true;
        }

        // Check cooldown (shared with /back)
        if (!player.hasPermission("qol.back.bypass.cooldown") && backManager.isOnCooldown(player.getUniqueId())) {
            int remaining = backManager.getRemainingCooldown(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.GOLD + remaining + "s" + ChatColor.RED + " before using /dback again.");
            return true;
        }

        Location deathLocation = backManager.getDeathLocation(player.getUniqueId());

        // Save current location as previous so /back works after /dback
        backManager.savePreviousLocation(player.getUniqueId(), player.getLocation());

        // Teleport
        player.teleport(deathLocation);
        backManager.applyCooldown(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Teleported to your last death location.");
        return true;
    }
}
