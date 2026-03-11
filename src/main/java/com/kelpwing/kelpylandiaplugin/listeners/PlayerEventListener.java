package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.integrations.DiscordIntegration;
import com.kelpwing.kelpylandiaplugin.chat.ChannelManager;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import com.kelpwing.kelpylandiaplugin.moderation.commands.VanishCommand;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.Statistic;

public class PlayerEventListener implements Listener {

    private final KelpylandiaPlugin plugin;
    private final DiscordIntegration discord;
    private final ChannelManager channelManager;

    public PlayerEventListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.discord = plugin.getDiscordIntegration();
        this.channelManager = plugin.getChannelManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Skip vanished players — VanishCommand fires fake join events for DiscordSRV;
        // VanishCommand handles the in-game broadcast itself, so we must not duplicate it.
        VanishCommand vc = plugin.getVanishCommand();
        if (vc != null && vc.isVanished(player)) {
            // Still suppress default join message
            if (plugin.getConfig().getBoolean("join-leave.hide-default", true)) {
                event.setJoinMessage(null);
            }
            return;
        }
        
        // Handle custom join messages
        if (plugin.getConfig().getBoolean("join-leave.enabled", true)) {
            // Hide default message
            if (plugin.getConfig().getBoolean("join-leave.hide-default", true)) {
                event.setJoinMessage(null);
            }
            
            // Send custom join message to global channel
            String joinMessage = plugin.getConfig().getString("join-leave.join-message", "&e{player} &ajoined the server!");
            joinMessage = joinMessage.replace("{player}", player.getName());
            joinMessage = joinMessage.replace("{displayname}", player.getDisplayName());
            
            // Route through global chat channel
            sendToGlobalChannel(org.bukkit.ChatColor.translateAlternateColorCodes('&', joinMessage));
        }
        
        // Handle Discord broadcasting
        if (discord != null && discord.isEnabled()) {
            // Broadcast to global Discord chat if enabled
            if (plugin.getConfig().getBoolean("discord.events.broadcast-joins", true)) {
                String discordMessage = plugin.getConfig().getString("discord.formats.join", "**{player}** joined Kelpy Land!");
                discordMessage = discordMessage.replace("{player}", player.getName());
                discord.sendToGlobalChat(discordMessage);
            }
            
            // Send to console channel if enabled
            if (plugin.getConfig().getBoolean("discord.events.console-logging", true)) {
                String consoleMessage = String.format("Player `%s` joined the server (IP: %s)", 
                    player.getName(), 
                    player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "Unknown");
                discord.sendToConsole(consoleMessage);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Skip vanished players — VanishCommand fires fake quit events for DiscordSRV,
        // and VanishListener handles suppressing the real quit when a vanished player logs out.
        VanishCommand vc = plugin.getVanishCommand();
        if (vc != null && vc.isVanished(player)) {
            // Suppress default quit message
            if (plugin.getConfig().getBoolean("join-leave.hide-default", true)) {
                event.setQuitMessage(null);
            }
            return;
        }
        
        // Handle custom leave messages
        if (plugin.getConfig().getBoolean("join-leave.enabled", true)) {
            // Hide default message
            if (plugin.getConfig().getBoolean("join-leave.hide-default", true)) {
                event.setQuitMessage(null);
            }
            
            // Send custom leave message to global channel
            String leaveMessage = plugin.getConfig().getString("join-leave.leave-message", "&e{player} &cleft the server!");
            leaveMessage = leaveMessage.replace("{player}", player.getName());
            leaveMessage = leaveMessage.replace("{displayname}", player.getDisplayName());
            
            // Route through global chat channel
            sendToGlobalChannel(org.bukkit.ChatColor.translateAlternateColorCodes('&', leaveMessage));
        }
        
        // Handle Discord broadcasting
        if (discord != null && discord.isEnabled()) {
            // Broadcast to global Discord chat if enabled
            if (plugin.getConfig().getBoolean("discord.events.broadcast-leaves", true)) {
                String discordMessage = plugin.getConfig().getString("discord.formats.leave", "**{player}** left Kelpy Land!");
                discordMessage = discordMessage.replace("{player}", player.getName());
                discord.sendToGlobalChat(discordMessage);
            }
            
            // Send to console channel if enabled
            if (plugin.getConfig().getBoolean("discord.events.console-logging", true)) {
                String consoleMessage = String.format("Player `%s` left the server", player.getName());
                discord.sendToConsole(consoleMessage);
            }
        }
    }

    @SuppressWarnings("deprecation") // getKey() deprecated since 1.21.4, but still functional across all versions
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        
        // Skip vanished players — don't broadcast their advancements
        VanishCommand vc = plugin.getVanishCommand();
        if (vc != null && vc.isVanished(player)) {
            return;
        }
        
        // Skip recipe advancements and other hidden ones
        if (advancement.getKey().getNamespace().equals("minecraft") && 
            (advancement.getKey().getKey().startsWith("recipes/") ||
             !VersionHelper.hasAdvancementDisplay(advancement))) {
            return;
        }
        
        // Handle Discord broadcasting
        if (discord != null && discord.isEnabled()) {
            String advancementTitle = VersionHelper.getAdvancementTitle(advancement);
            if (advancementTitle == null) {
                advancementTitle = advancement.getKey().getKey();
            }
            String advancementDescription = VersionHelper.getAdvancementDescription(advancement);
            
            // Broadcast advancements to global Discord chat if enabled
            if (plugin.getConfig().getBoolean("discord.events.broadcast-advancements", true)) {
                if (plugin.getConfig().getBoolean("discord.events.use-embeds", true)) {
                    discord.sendAdvancementEmbed(player, advancementTitle, advancementDescription);
                } else {
                    // Fallback to text message
                    String discordAdvancementMessage = plugin.getConfig().getString("discord.format.advancement-message", 
                        ":star: **{player}** has made the advancement **{advancement}**");
                    discordAdvancementMessage = PlaceholderAPI.setPlaceholders(player, discordAdvancementMessage);
                    discordAdvancementMessage = discordAdvancementMessage.replace("{player}", player.getName());
                    discordAdvancementMessage = discordAdvancementMessage.replace("{displayname}", player.getDisplayName());
                    discordAdvancementMessage = discordAdvancementMessage.replace("{advancement}", advancementTitle);
                    discord.sendToGlobalChat(discordAdvancementMessage);
                }
            }
            
            // Send to console channel if enabled
            if (plugin.getConfig().getBoolean("discord.events.console-logging", true)) {
                String consoleMessage = String.format("Player `%s` completed advancement: %s", 
                    player.getName(), advancementTitle);
                discord.sendToConsole(consoleMessage);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        Statistic statistic = event.getStatistic();
        
        // Skip vanished players
        VanishCommand vc2 = plugin.getVanishCommand();
        if (vc2 != null && vc2.isVanished(player)) {
            return;
        }
        
        // Handle certain milestone achievements
        if (discord != null && discord.isEnabled() && 
            plugin.getConfig().getBoolean("discord.events.broadcast-achievements", true)) {
            
            String achievementMessage = null;
            
            // Check for specific milestone achievements
            switch (statistic) {
                case PLAYER_KILLS:
                    if (event.getNewValue() == 1) {
                        achievementMessage = "First Player Kill";
                    } else if (event.getNewValue() % 10 == 0) {
                        achievementMessage = event.getNewValue() + " Player Kills";
                    }
                    break;
                case MOB_KILLS:
                    if (event.getNewValue() == 100) {
                        achievementMessage = "Monster Hunter (100 kills)";
                    } else if (event.getNewValue() == 1000) {
                        achievementMessage = "Monster Slayer (1000 kills)";
                    }
                    break;
                case DEATHS:
                    if (event.getNewValue() == 1) {
                        achievementMessage = "First Death";
                    }
                    break;
                case PLAY_ONE_MINUTE:
                    // Convert ticks to hours (20 ticks per second, 3600 seconds per hour)
                    long hours = event.getNewValue() / (20 * 3600);
                    if (hours > 0 && hours % 100 == 0) {
                        achievementMessage = hours + " Hours Played";
                    }
                    break;
                default:
                    break;
            }
            
            if (achievementMessage != null) {
                if (plugin.getConfig().getBoolean("discord.events.use-embeds", true)) {
                    discord.sendAchievementEmbed(player, achievementMessage, 
                        "Milestone achievement completed!");
                } else {
                    // Fallback to text message
                    String discordAchievementMessage = plugin.getConfig().getString("discord.format.achievement-message", 
                        ":trophy: **{player}** has completed the achievement **{achievement}**");
                    discordAchievementMessage = PlaceholderAPI.setPlaceholders(player, discordAchievementMessage);
                    discordAchievementMessage = discordAchievementMessage.replace("{player}", player.getName());
                    discordAchievementMessage = discordAchievementMessage.replace("{displayname}", player.getDisplayName());
                    discordAchievementMessage = discordAchievementMessage.replace("{achievement}", achievementMessage);
                    discord.sendToGlobalChat(discordAchievementMessage);
                }
                
                // Also send to console if enabled
                if (plugin.getConfig().getBoolean("discord.events.console-logging", true)) {
                    String consoleMessage = String.format("Player `%s` achieved: %s", 
                        player.getName(), achievementMessage);
                    discord.sendToConsole(consoleMessage);
                }
            }
        }
    }
    
    private void sendToGlobalChannel(String message) {
        Channel globalChannel = channelManager.getChannel("global");
        if (globalChannel != null) {
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Channel playerChannel = channelManager.getPlayerChannel(onlinePlayer);
                if (playerChannel != null && (playerChannel.getName().equals("global") || 
                    plugin.getConfig().getBoolean("broadcast-join-leave-to-all-channels", true))) {
                    onlinePlayer.sendMessage(message);
                }
            }
        } else {
            // Fallback to regular broadcast if global channel doesn't exist
            plugin.getServer().broadcastMessage(message);
        }
    }
}
