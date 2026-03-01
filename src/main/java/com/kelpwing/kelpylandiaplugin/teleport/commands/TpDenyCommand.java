package com.kelpwing.kelpylandiaplugin.teleport.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.teleport.TpaManager;
import com.kelpwing.kelpylandiaplugin.teleport.TpaRequest;
import org.bukkit.Bukkit;
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
 * /tpdeny [player] - Deny a pending teleport request.
 * If no player specified, denies the most recent request.
 */
public class TpDenyCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public TpDenyCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("teleport.enabled", true)) {
            sender.sendMessage(ChatColor.RED + "Teleport requests are currently disabled.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        TpaManager tpaManager = plugin.getTpaManager();
        TpaRequest request;

        if (args.length > 0) {
            Player requester = Bukkit.getPlayer(args[0]);
            if (requester == null) {
                player.sendMessage(ChatColor.RED + "Player " + ChatColor.GOLD + args[0] + ChatColor.RED + " is not online.");
                return true;
            }
            request = tpaManager.getRequest(player.getUniqueId(), requester.getUniqueId());
            if (request == null) {
                player.sendMessage(ChatColor.RED + "You don't have a pending request from " + ChatColor.GOLD + requester.getName() + ChatColor.RED + ".");
                return true;
            }
        } else {
            request = tpaManager.getLatestRequest(player.getUniqueId());
            if (request == null) {
                player.sendMessage(ChatColor.RED + "You don't have any pending teleport requests.");
                return true;
            }
        }

        Player requester = Bukkit.getPlayer(request.getRequesterUUID());

        // Remove the request
        tpaManager.removeRequest(request);

        player.sendMessage(ChatColor.RED + "Teleport request denied.");

        if (requester != null) {
            requester.sendMessage(ChatColor.RED + "Your teleport request to " + ChatColor.GOLD + player.getName() + ChatColor.RED + " was denied.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            TpaManager tpaManager = plugin.getTpaManager();
            List<TpaRequest> requests = tpaManager.getRequestsFor(player.getUniqueId());
            List<String> names = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (TpaRequest req : requests) {
                Player p = Bukkit.getPlayer(req.getRequesterUUID());
                if (p != null && p.getName().toLowerCase().startsWith(partial)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
