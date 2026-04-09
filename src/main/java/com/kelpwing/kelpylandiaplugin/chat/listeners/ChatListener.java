package com.kelpwing.kelpylandiaplugin.chat.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import com.kelpwing.kelpylandiaplugin.chat.ChatUtils;
import com.kelpwing.kelpylandiaplugin.commands.MsgCommand;
import com.kelpwing.kelpylandiaplugin.utils.LevelManager;
import com.kelpwing.kelpylandiaplugin.utils.SpyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;

import java.util.Set;
import java.util.UUID;

public class ChatListener implements Listener {

    private final KelpylandiaPlugin plugin;

    public ChatListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Sticky whisper target: if the player has a /wt target set,
        // redirect ALL chat to that target as a private message.
        MsgCommand msgCmd = plugin.getMsgCommand();
        if (msgCmd != null) {
            UUID targetUUID = msgCmd.getWhisperTarget(player.getUniqueId());
            if (targetUUID != null) {
                // Target is console
                if (targetUUID.equals(MsgCommand.CONSOLE_UUID)) {
                    event.setCancelled(true);
                    Bukkit.getScheduler().runTask(plugin, () -> msgCmd.sendPrivateMessage(player, Bukkit.getConsoleSender(), message));
                    return;
                }
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    // Cancel the public chat event and send as private message
                    event.setCancelled(true);
                    // Must run sync since sendPrivateMessage may trigger Bukkit API
                    Bukkit.getScheduler().runTask(plugin, () -> msgCmd.sendPrivateMessage(player, target, message));
                    return;
                } else {
                    // Target went offline — notify and auto-clear
                    msgCmd.clearWhisperTarget(player.getUniqueId());
                    player.sendMessage(ChatColor.RED + "Your whisper target is no longer online. Target cleared.");
                }
            }
        }
        
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
            
            // SocialSpy: let staff with socialspy see non-global channel messages they wouldn't normally see
            SpyManager spyManager = plugin.getSpyManager();
            if (spyManager != null) {
                String spyMsg = ChatColor.DARK_GRAY + "[SS] " + ChatColor.GRAY + "[" + playerChannel.getDisplayName() + "] "
                        + player.getName() + ": " + ChatColor.WHITE + message;
                for (java.util.UUID spyUUID : spyManager.getSocialSpies()) {
                    if (spyUUID.equals(player.getUniqueId())) continue;
                    Player spy = Bukkit.getPlayer(spyUUID);
                    if (spy != null && spy.isOnline() && !recipients.contains(spy)) {
                        // Level check: spy must have >= level than the sender
                        if (!LevelManager.canObserve(spy, player, "socialspy")) continue;
                        spy.sendMessage(spyMsg);
                    }
                }
            }
        }
        
        
        // Send to Discord if enabled.
        // When InteractiveChat is present and its integration is active, we defer
        // the Discord relay to InteractiveChatIntegration (which runs at MONITOR,
        // after IC has processed the message).  Otherwise we send the raw message now.
        com.kelpwing.kelpylandiaplugin.integrations.InteractiveChatIntegration icInt =
                plugin.getInteractiveChatIntegration();
        boolean icHandlesDiscord = icInt != null && icInt.isEnabled();
        if (!icHandlesDiscord
                && playerChannel.isDiscordEnabled()
                && plugin.getDiscordIntegration() != null
                && plugin.getDiscordIntegration().isEnabled()) {
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
