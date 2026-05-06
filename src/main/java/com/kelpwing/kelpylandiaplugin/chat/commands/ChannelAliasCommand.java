package com.kelpwing.kelpylandiaplugin.chat.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import com.kelpwing.kelpylandiaplugin.chat.ChatFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// channel alias commands (e.g. /l for local channel)
public class ChannelAliasCommand implements CommandExecutor {

    private final KelpylandiaPlugin plugin;
    private final String channelName;

    public ChannelAliasCommand(KelpylandiaPlugin plugin, String channelName) {
        this.plugin = plugin;
        this.channelName = channelName;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Channel channel = plugin.getChannelManager().getChannel(channelName);
        if (channel == null) {
            player.sendMessage(ChatColor.RED + "Channel '" + channelName + "' no longer exists.");
            return true;
        }

        if (!ChatFormatUtils.hasPermission(player, channel.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to join this channel.");
            return true;
        }

        if (args.length > 0) {
            // quicksend: send message to channel w/o changing active channel
            String message = String.join(" ", args);
            String previousChannel = plugin.getChannelManager().getPlayerChannel(player.getUniqueId());
            plugin.getChannelManager().setPlayerChannel(player.getUniqueId(), channel.getName());
            try {
                player.chat(message);
            } finally {
                // always restore, even if chat() threw
                plugin.getChannelManager().setPlayerChannel(player.getUniqueId(), previousChannel);
            }
        } else {
            // no message -> toggle to this channel
            plugin.getChannelManager().setPlayerChannel(player.getUniqueId(), channel.getName());
            player.sendMessage(ChatColor.GREEN + "Switched to channel: " + channel.getFormattedDisplayName());
        }
        return true;
    }
}
