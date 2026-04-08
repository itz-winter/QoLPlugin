package com.kelpwing.kelpylandiaplugin.teleport.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.teleport.TpaManager;
import com.kelpwing.kelpylandiaplugin.teleport.TpaRequest;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
 * /tpahere <player> - Request another player to teleport to you.
 */
public class TpaHereCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public TpaHereCommand(KelpylandiaPlugin plugin) {
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

        if (!player.hasPermission("qol.tpa.here")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /tpahere <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + ChatColor.GOLD + args[0] + ChatColor.RED + " is not online.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You can't send a teleport request to yourself!");
            return true;
        }

        TpaManager tpaManager = plugin.getTpaManager();

        // Check cooldown
        if (tpaManager.isOnCooldown(player)) {
            long remaining = tpaManager.getCooldownRemaining(player);
            player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.GOLD +
                String.format("%.1f", remaining / 1000.0) + "s" + ChatColor.RED + " before sending another request.");
            return true;
        }

        TpaRequest request = tpaManager.sendRequest(player, target, TpaRequest.Type.TPA_HERE);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "You already have a pending request to " + ChatColor.GOLD + target.getName() + ChatColor.RED + ".");
            return true;
        }

        // Notify requester
        player.sendMessage(ChatColor.GREEN + "Teleport request sent to " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + ".");
        player.sendMessage(ChatColor.GRAY + "You asked them to teleport to your location.");
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/tpcancel" + ChatColor.GRAY + " to cancel.");

        // Notify target with clickable buttons
        target.sendMessage("");
        target.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage(ChatColor.GREEN + " " + player.getName() + ChatColor.YELLOW + " has requested that you teleport to them.");
        target.sendMessage(ChatColor.GRAY + " Expires in " + request.getSecondsRemaining() + " seconds.");
        target.sendMessage("");

        TextComponent accept = new TextComponent("  ✔ Accept  ");
        accept.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        accept.setBold(true);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + player.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to accept - you will teleport to " + player.getName()).color(net.md_5.bungee.api.ChatColor.GREEN).create()));

        TextComponent deny = new TextComponent("  ✘ Deny  ");
        deny.setColor(net.md_5.bungee.api.ChatColor.RED);
        deny.setBold(true);
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + player.getName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to deny the teleport request").color(net.md_5.bungee.api.ChatColor.RED).create()));

        TextComponent buttons = new TextComponent("");
        buttons.addExtra(accept);
        buttons.addExtra(new TextComponent("  "));
        buttons.addExtra(deny);
        target.spigot().sendMessage(buttons);

        target.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != sender && p.getName().toLowerCase().startsWith(partial)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
