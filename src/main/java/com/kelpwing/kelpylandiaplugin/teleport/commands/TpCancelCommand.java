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

import java.util.Collections;
import java.util.List;

/**
 * /tpcancel - Cancel your outgoing teleport request.
 */
public class TpCancelCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public TpCancelCommand(KelpylandiaPlugin plugin) {
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
        TpaRequest request = tpaManager.getOutgoingRequest(player.getUniqueId());

        if (request == null) {
            player.sendMessage(ChatColor.RED + "You don't have any outgoing teleport requests.");
            return true;
        }

        Player target = Bukkit.getPlayer(request.getTargetUUID());

        tpaManager.removeRequest(request);

        player.sendMessage(ChatColor.YELLOW + "Your teleport request has been cancelled.");

        if (target != null) {
            target.sendMessage(ChatColor.YELLOW + "" + ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " cancelled their teleport request.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
