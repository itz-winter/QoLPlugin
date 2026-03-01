package com.kelpwing.kelpylandiaplugin.homes.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.homes.Home;
import com.kelpwing.kelpylandiaplugin.homes.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /sethome [name] - Sets a home at current location.
 * If no name is given, uses "home" as the default name.
 */
public class SetHomeCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public SetHomeCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("homes.enabled", true)) {
            sender.sendMessage(ChatColor.RED + "Homes are currently disabled.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("kelpylandia.homes.set")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to set homes.");
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "home";

        // Validate home name
        if (!homeName.matches("[a-zA-Z0-9_-]+")) {
            player.sendMessage(ChatColor.RED + "Home name can only contain letters, numbers, hyphens, and underscores.");
            return true;
        }

        if (homeName.length() > 32) {
            player.sendMessage(ChatColor.RED + "Home name cannot be longer than 32 characters.");
            return true;
        }

        HomeManager homeManager = plugin.getHomeManager();
        Location location = player.getLocation();
        boolean isOverwrite = homeManager.hasHome(player.getUniqueId(), homeName);

        boolean success = homeManager.setHome(player, homeName, location);
        if (!success) {
            int max = homeManager.getMaxHomes(player);
            player.sendMessage(ChatColor.RED + "You have reached your maximum number of homes! (" + max + ")");
            player.sendMessage(ChatColor.GRAY + "Delete a home with " + ChatColor.YELLOW + "/delhome <name>" + ChatColor.GRAY + " to make room.");
            return true;
        }

        if (isOverwrite) {
            player.sendMessage(ChatColor.GREEN + "Home " + ChatColor.GOLD + homeName + ChatColor.GREEN + " has been updated to your current location.");
        } else {
            int count = homeManager.getHomeCount(player.getUniqueId());
            int max = homeManager.getMaxHomes(player);
            player.sendMessage(ChatColor.GREEN + "Home " + ChatColor.GOLD + homeName + ChatColor.GREEN + " has been set! " +
                ChatColor.GRAY + "(" + count + "/" + max + ")");
        }

        // Show coordinates
        player.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.WHITE +
            String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()) +
            ChatColor.GRAY + " in " + ChatColor.WHITE + location.getWorld().getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            // Suggest existing home names (for overwrite) and a default
            List<String> suggestions = new ArrayList<>(plugin.getHomeManager().getHomeNames(player.getUniqueId()));
            if (!suggestions.contains("home")) suggestions.add(0, "home");

            String partial = args[0].toLowerCase();
            suggestions.removeIf(s -> !s.toLowerCase().startsWith(partial));
            return suggestions;
        }
        return Collections.emptyList();
    }
}
