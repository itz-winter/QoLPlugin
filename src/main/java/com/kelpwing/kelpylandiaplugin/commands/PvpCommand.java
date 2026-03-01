package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PvpCommand implements CommandExecutor, Listener {
    private final KelpylandiaPlugin plugin;
    private final Set<UUID> pvpDisabledPlayers;

    public PvpCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.pvpDisabledPlayers = new HashSet<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Toggle PvP status
            togglePvP(player);
        } else if (args.length == 1) {
            String action = args[0].toLowerCase();
            switch (action) {
                case "on":
                case "enable":
                case "true":
                    enablePvP(player);
                    break;
                case "off":
                case "disable":
                case "false":
                    disablePvP(player);
                    break;
                case "status":
                case "check":
                    checkPvPStatus(player);
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Usage: /pvp [on|off|status]");
                    return true;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /pvp [on|off|status]");
            return true;
        }

        return true;
    }

    private void togglePvP(Player player) {
        if (isPvPDisabled(player)) {
            enablePvP(player);
        } else {
            disablePvP(player);
        }
    }

    private void enablePvP(Player player) {
        pvpDisabledPlayers.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "PvP enabled! You can now engage in combat with other players.");
        
        // Log the change
        plugin.getLogger().info(player.getName() + " enabled their PvP");
    }

    private void disablePvP(Player player) {
        pvpDisabledPlayers.add(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "PvP disabled! You are now protected from combat with other players.");
        player.sendMessage(ChatColor.YELLOW + "Note: You also cannot attack other players while PvP is disabled.");
        
        // Log the change
        plugin.getLogger().info(player.getName() + " disabled their PvP");
    }

    private void checkPvPStatus(Player player) {
        if (isPvPDisabled(player)) {
            player.sendMessage(ChatColor.RED + "Your PvP is currently DISABLED.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Your PvP is currently ENABLED.");
        }
    }

    public boolean isPvPDisabled(Player player) {
        return pvpDisabledPlayers.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Must be a player being damaged
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = null;

        // Check for direct player attack
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Check for projectile attack (arrows, snowballs, eggs, tridents, etc.)
        else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                attacker = (Player) shooter;
            }
        }

        // If no player attacker found, let the event proceed
        if (attacker == null) {
            return;
        }

        // Check if either player has PvP disabled
        if (isPvPDisabled(victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.YELLOW + victim.getName() + " has PvP disabled and cannot be attacked.");
            return;
        }

        if (isPvPDisabled(attacker)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.YELLOW + "You cannot attack other players while your PvP is disabled.");
            attacker.sendMessage(ChatColor.GRAY + "Use /pvp on to enable PvP.");
            return;
        }
    }

    // Method to clean up when player leaves
    public void cleanupPlayer(UUID playerUUID) {
        pvpDisabledPlayers.remove(playerUUID);
    }

    // Method to get all players with PvP disabled (for administrative purposes)
    public Set<UUID> getPvPDisabledPlayers() {
        return new HashSet<>(pvpDisabledPlayers);
    }
}
