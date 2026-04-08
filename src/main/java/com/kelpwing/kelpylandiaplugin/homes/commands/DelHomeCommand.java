package com.kelpwing.kelpylandiaplugin.homes.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.homes.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /delhome <name> - Deletes a home.
 */
public class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public DelHomeCommand(KelpylandiaPlugin plugin) {
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

        if (!player.hasPermission("qol.homes.delete")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to delete homes.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /delhome <name>");
            return true;
        }

        String homeName = args[0];
        HomeManager homeManager = plugin.getHomeManager();

        if (!homeManager.hasHome(player.getUniqueId(), homeName)) {
            player.sendMessage(ChatColor.RED + "You don't have a home named " + ChatColor.GOLD + homeName + ChatColor.RED + ".");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/homes" + ChatColor.GRAY + " to see your homes.");
            return true;
        }

        homeManager.deleteHome(player.getUniqueId(), homeName);
        int remaining = homeManager.getHomeCount(player.getUniqueId());
        int max = homeManager.getMaxHomes(player);
        player.sendMessage(ChatColor.GREEN + "Home " + ChatColor.GOLD + homeName + ChatColor.GREEN + " has been deleted. " +
            ChatColor.GRAY + "(" + remaining + "/" + max + " homes)");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<>(plugin.getHomeManager().getHomeNames(player.getUniqueId()));
            String partial = args[0].toLowerCase();
            names.removeIf(s -> !s.toLowerCase().startsWith(partial));
            return names;
        }
        return Collections.emptyList();
    }
}
