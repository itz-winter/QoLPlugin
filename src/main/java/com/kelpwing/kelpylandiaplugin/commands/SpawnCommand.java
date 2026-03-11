package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /spawn [player] - Teleport to the world spawn point.
 * With a target argument and kelpylandia.spawn.others permission, teleport another player to spawn.
 */
public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public SpawnCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /spawn [player]
        if (args.length == 0) {
            // Teleport self
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            if (!player.hasPermission("kelpylandia.spawn")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            teleportToSpawn(player);
            return true;
        }

        // Teleport another player
        if (!sender.hasPermission("kelpylandia.spawn.others")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to teleport other players to spawn.");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player " + ChatColor.GOLD + args[0] + ChatColor.RED + " is not online.");
            return true;
        }

        teleportToSpawn(target);
        target.sendMessage(ChatColor.GREEN + "You have been teleported to spawn.");
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Teleported " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + " to spawn.");
        }
        return true;
    }

    private void teleportToSpawn(Player player) {
        World world = plugin.getServer().getWorlds().get(0); // Main world
        Location spawn = world.getSpawnLocation();
        // Center on block and add 0.5 for cleaner positioning
        spawn = spawn.add(0.5, 0, 0.5);

        // Save previous location for /back
        if (plugin.getBackManager() != null) {
            plugin.getBackManager().savePreviousLocation(player.getUniqueId(), player.getLocation());
        }

        player.teleport(spawn);
        player.sendMessage(ChatColor.GREEN + "Teleported to spawn!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("kelpylandia.spawn.others")) {
            List<String> names = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
