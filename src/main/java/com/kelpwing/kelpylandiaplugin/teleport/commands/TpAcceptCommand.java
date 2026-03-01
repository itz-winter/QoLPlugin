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
 * /tpaccept [player] - Accept a pending teleport request.
 * If no player specified, accepts the most recent request.
 */
public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public TpAcceptCommand(KelpylandiaPlugin plugin) {
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
            // Accept request from specific player
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
            // Accept most recent request
            request = tpaManager.getLatestRequest(player.getUniqueId());
            if (request == null) {
                player.sendMessage(ChatColor.RED + "You don't have any pending teleport requests.");
                return true;
            }
        }

        Player requester = Bukkit.getPlayer(request.getRequesterUUID());
        if (requester == null) {
            player.sendMessage(ChatColor.RED + "The player who sent the request is no longer online.");
            tpaManager.removeRequest(request);
            return true;
        }

        // Perform the teleport based on request type
        Player teleportee;
        Player destination;
        if (request.getType() == TpaRequest.Type.TPA) {
            // Requester teleports TO the target (accepter)
            teleportee = requester;
            destination = player;
        } else {
            // Target (accepter) teleports TO the requester
            teleportee = player;
            destination = requester;
        }

        teleportee.teleport(destination.getLocation());

        // Notify both players
        player.sendMessage(ChatColor.GREEN + "Teleport request accepted!");
        requester.sendMessage(ChatColor.GREEN + "Your teleport request was accepted by " + ChatColor.GOLD + player.getName() + ChatColor.GREEN + "!");

        if (request.getType() == TpaRequest.Type.TPA) {
            requester.sendMessage(ChatColor.GRAY + "Teleported to " + player.getName() + ".");
        } else {
            player.sendMessage(ChatColor.GRAY + "Teleported to " + requester.getName() + ".");
        }

        // Apply cooldown and invulnerability to the person who moved
        tpaManager.applyCooldown(teleportee);
        tpaManager.applyInvulnerability(teleportee);

        // Remove the request
        tpaManager.removeRequest(request);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            // Suggest names of players who have sent requests
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
