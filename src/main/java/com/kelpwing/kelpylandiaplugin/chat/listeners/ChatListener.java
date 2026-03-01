package com.kelpwing.kelpylandiaplugin.chat.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import com.kelpwing.kelpylandiaplugin.chat.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;

import java.util.Set;

public class ChatListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public ChatListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Check if player is muted
        if (ChatUtils.isPlayerMuted(player)) {
            player.sendMessage(ChatColor.RED + "You are currently muted and cannot speak.");
            event.setCancelled(true);
            return;
        }
        
        // Get player's current channel
        Channel playerChannel = plugin.getChannelManager().getPlayerChannel(player);
        
        if (playerChannel == null) {
            // Use default channel
            playerChannel = plugin.getChannelManager().getDefaultChannel();
        }
        
        // Check if player has permission to speak in this channel
        if (!ChatUtils.hasPermission(player, playerChannel.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to speak in this channel.");
            event.setCancelled(true);
            return;
        }
        
        // Format the message
        String formattedMessage = ChatUtils.formatMessage(plugin, player, playerChannel, message);
        
        // Set the formatted message
        event.setFormat(formattedMessage);
        
        // Handle channel-specific recipients
        if (!playerChannel.isGlobal()) {
            Set<Player> recipients = event.getRecipients();
            recipients.clear();
            
            // Add players in range or with permission
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (shouldReceiveMessage(onlinePlayer, player, playerChannel)) {
                    recipients.add(onlinePlayer);
                }
            }
        }
        
        // Send to Discord if enabled
        if (playerChannel.isDiscordEnabled() && plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            plugin.getDiscordIntegration().sendChatMessage(player, message, playerChannel.getDiscordChannel());
        }
        
        // Log the message
        plugin.getLogger().info("[" + playerChannel.getName() + "] " + player.getName() + ": " + message);
    }
    
    private boolean shouldReceiveMessage(Player recipient, Player sender, Channel channel) {
        // Check if recipient has permission to see this channel
        if (!ChatUtils.hasPermission(recipient, channel.getPermission())) {
            return false;
        }
        
        // Check world restrictions
        if (!channel.isWorldAllowed(recipient.getWorld().getName())) {
            return false;
        }
        
        // Check range restrictions
        if (channel.isRangeEnabled()) {
            // Players must be in the same world for distance calculations
            if (!recipient.getWorld().equals(sender.getWorld())) {
                return false;
            }
            double distance = recipient.getLocation().distance(sender.getLocation());
            return distance <= channel.getRange();
        }
        
        return true;
    }
}
