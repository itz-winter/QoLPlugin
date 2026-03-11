package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
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
import java.util.UUID;

/**
 * /wt <player> — Lock your whisper target so /w <msg> goes to them without specifying a name.
 * /wt          — Clear the whisper target.
 */
public class WhisperToggleCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public WhisperToggleCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        MsgCommand msgCmd = plugin.getMsgCommand();
        if (msgCmd == null) {
            player.sendMessage(ChatColor.RED + "Messaging system is not enabled.");
            return true;
        }

        // No args = clear the whisper target
        if (args.length == 0) {
            UUID current = msgCmd.getWhisperTarget(player.getUniqueId());
            if (current == null) {
                player.sendMessage(ChatColor.YELLOW + "You don't have a whisper target set. Use /wt <player> to set one.");
            } else {
                msgCmd.clearWhisperTarget(player.getUniqueId());
                String name;
                if (current.equals(MsgCommand.CONSOLE_UUID)) {
                    name = "Console";
                } else {
                    Player target = Bukkit.getPlayer(current);
                    name = target != null ? target.getName() : "offline player";
                }
                player.sendMessage(ChatColor.GREEN + "Whisper target cleared (" + name + "). Your chat will go to the channel again.");
            }
            return true;
        }

        // /wt Console = set whisper target to console
        if (args[0].equalsIgnoreCase("console")) {
            msgCmd.setWhisperTarget(player.getUniqueId(), MsgCommand.CONSOLE_UUID);
            player.sendMessage(ChatColor.GREEN + "Whisper target set to " + ChatColor.GOLD + "Console" + ChatColor.GREEN + ". All your chat will now go to the console. Use /wt to clear.");
            return true;
        }

        // /wt <player> = set the whisper target
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You can't set yourself as a whisper target.");
            return true;
        }

        msgCmd.setWhisperTarget(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Whisper target set to " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + ". All your chat will now go to them. Use /wt to clear.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            if ("console".startsWith(prefix)) {
                names.add("Console");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(sender)) continue;
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    names.add(p.getName());
                }
            }
            Collections.sort(names);
            return names;
        }
        return Collections.emptyList();
    }
}
