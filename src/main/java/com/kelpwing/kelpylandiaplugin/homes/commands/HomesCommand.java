package com.kelpwing.kelpylandiaplugin.homes.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.homes.Home;
import com.kelpwing.kelpylandiaplugin.homes.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /homes - Lists all homes or opens the homes GUI.
 */
public class HomesCommand implements CommandExecutor {

    private final KelpylandiaPlugin plugin;

    public HomesCommand(KelpylandiaPlugin plugin) {
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

        if (!player.hasPermission("kelpylandia.homes.list")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to list homes.");
            return true;
        }

        HomeManager homeManager = plugin.getHomeManager();
        List<Home> homes = homeManager.getHomeList(player.getUniqueId());

        if (homes.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You don't have any homes set.");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/sethome [name]" + ChatColor.GRAY + " to create one.");
            return true;
        }

        // Check if GUI mode is preferred (default)
        if (plugin.getConfig().getBoolean("homes.use-gui", true)) {
            plugin.getHomeGUI().openGUI(player);
            return true;
        }

        // Fallback: text-based list
        int max = homeManager.getMaxHomes(player);
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GOLD + " 🏠 Your Homes " + ChatColor.GRAY + "(" + homes.size() + "/" + max + ")");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (Home home : homes) {
            String worldName = home.getWorldName();
            String coords = String.format("%.0f, %.0f, %.0f", home.getX(), home.getY(), home.getZ());
            player.sendMessage(ChatColor.YELLOW + " ▸ " + ChatColor.WHITE + home.getName() +
                ChatColor.GRAY + " - " + coords + " (" + worldName + ")");
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Click a home or use " + ChatColor.YELLOW + "/home <name>" + ChatColor.GRAY + " to teleport.");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return true;
    }
}
