package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.commands.VanishCommand;
import com.kelpwing.kelpylandiaplugin.utils.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;

public class VanishListener implements Listener {

    private final KelpylandiaPlugin plugin;
    private final VanishManager vanishManager;
    private final VanishCommand vanishCommand;

    public VanishListener(KelpylandiaPlugin plugin, VanishCommand vanishCommand) {
        this.plugin = plugin;
        this.vanishManager = plugin.getVanishManager();
        this.vanishCommand = vanishCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (vanishCommand.isVanished(player) && !vanishManager.canPickupItems(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (vanishCommand.isVanished(player)) {
                // Suppress sounds and animations for vanished players
                // This is handled by making the interaction silent
                event.getInventory().getViewers().clear();
                event.getInventory().getViewers().add(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (vanishCommand.isVanished(player) && event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            
            if (player.isSneaking()) {
                // Shift+Right click to clear target
                vanishManager.clearVanishTarget(player);
                player.sendMessage("§cTarget cleared!");
            } else {
                // Right click to set target
                vanishManager.setVanishTarget(player, target);
                player.sendMessage("§aTarget set to: " + target.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (vanishCommand.isVanished(player)) {
            // Check for nearby players to auto-target
            Player currentTarget = vanishManager.getVanishTarget(player);
            
            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer != player && !vanishCommand.isVanished(nearbyPlayer)) {
                    double distance = player.getLocation().distance(nearbyPlayer.getLocation());
                    
                    // Auto-target if within 3 blocks and no current target
                    if (distance <= 3.0 && currentTarget == null) {
                        vanishManager.setVanishTarget(player, nearbyPlayer);
                        player.sendMessage("§eAuto-targeted: " + nearbyPlayer.getName());
                        break;
                    }
                    // Clear target if current target is too far away
                    else if (currentTarget == nearbyPlayer && distance > 10.0) {
                        vanishManager.clearVanishTarget(player);
                        player.sendMessage("§7Target cleared (too far away)");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save vanished state BEFORE cleanup so we can suppress the quit message
        boolean wasVanished = vanishCommand.isVanished(player);
        
        // Clean up vanish data
        if (wasVanished) {
            vanishCommand.removeVanishedPlayer(player);
        }
        vanishManager.cleanup(player);
        
        // Clean up PvP data
        if (plugin.getPvpCommand() != null) {
            plugin.getPvpCommand().cleanupPlayer(player.getUniqueId());
        }
        
        // Hide quit message if player was vanished
        if (wasVanished) {
            event.setQuitMessage(null);
        }
    }
    
    /**
     * Send fake join message when player unvanishes
     */
    public void sendFakeJoinMessage(Player player) {
        // Get join message format from config
        String joinMessage = plugin.getConfig().getString("join-leave.join-message", "&7[&a+&7]&r {player}");
        joinMessage = ChatColor.translateAlternateColorCodes('&', joinMessage.replace("{player}", player.getName()));

        // Broadcast to all players including the vanished player
        plugin.getServer().broadcastMessage(joinMessage);

        // Send to DiscordSRV global channel
        String discordJoinFormat = plugin.getConfig().getString("discord.formats.join", "**{player}** joined Kelpy Land!");
        String discordMessage = discordJoinFormat.replace("{player}", player.getName());
        try {
            if (DiscordSRV.getPlugin() != null) {
                TextChannel mainChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");
                if (mainChannel != null) {
                    DiscordUtil.sendMessage(mainChannel, discordMessage);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send fake join to DiscordSRV: " + e.getMessage());
        }

        // Log fake join
        plugin.getLogger().info("[FAKE JOIN] " + player.getName() + " (unvanished)");
    }
    
    /**
     * Send fake leave message when player vanishes
     */
    public void sendFakeLeaveMessage(Player player) {
        // Get leave message format from config
        String leaveMessage = plugin.getConfig().getString("join-leave.leave-message", "&7[&c-&7]&r {player}");
        leaveMessage = ChatColor.translateAlternateColorCodes('&', leaveMessage.replace("{player}", player.getName()));

        // Broadcast to all players including the vanished player
        plugin.getServer().broadcastMessage(leaveMessage);

        // Send to DiscordSRV global channel
        String discordLeaveFormat = plugin.getConfig().getString("discord.formats.leave", "**{player}** left Kelpy Land!");
        String discordMessage = discordLeaveFormat.replace("{player}", player.getName());
        try {
            if (DiscordSRV.getPlugin() != null) {
                TextChannel mainChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");
                if (mainChannel != null) {
                    DiscordUtil.sendMessage(mainChannel, discordMessage);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send fake leave to DiscordSRV: " + e.getMessage());
        }

        // Log fake leave
        plugin.getLogger().info("[FAKE LEAVE] " + player.getName() + " (vanished)");
    }
}
