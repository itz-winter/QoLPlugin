package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persists toggle states (fly, god, vanish, socialspy, commandspy, whisper target)
 * across player relogs. Data is stored in plugins/KelpylandiaPlugin/playerdata/<uuid>.yml.
 */
public class PlayerStateManager {

    private final KelpylandiaPlugin plugin;
    private final File dataFolder;

    public PlayerStateManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    /**
     * Save all relevant toggle states for a player.
     */
    public void saveState(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Fly
        cfg.set("fly", player.getAllowFlight());

        // God
        if (plugin.getGodCommand() != null) {
            cfg.set("god", plugin.getGodCommand().isGod(player));
        }

        // Vanish
        boolean vanished = false;
        if (plugin.getVanishCommand() != null) {
            vanished = plugin.getVanishCommand().isVanished(player);
            cfg.set("vanish", vanished);
        }

        // SocialSpy / CommandSpy
        boolean ss = false, cs = false;
        if (plugin.getSpyManager() != null) {
            ss = plugin.getSpyManager().isSocialSpy(uuid);
            cs = plugin.getSpyManager().isCommandSpy(uuid);
            cfg.set("socialspy", ss);
            cfg.set("commandspy", cs);
        }

        // Whisper target
        if (plugin.getMsgCommand() != null) {
            UUID wt = plugin.getMsgCommand().getWhisperTarget(uuid);
            cfg.set("whisper-target", wt != null ? wt.toString() : null);
        }

        try {
            cfg.save(file);
            if (vanished || ss || cs) {
                plugin.getLogger().info("[StateManager] Saved state for " + player.getName()
                    + " — vanish=" + vanished + " socialspy=" + ss + " commandspy=" + cs);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save state for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Restore all relevant toggle states for a player on join.
     */
    public void restoreState(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Fly
        if (cfg.contains("fly") && cfg.getBoolean("fly")) {
            player.setAllowFlight(true);
        }

        // God
        if (cfg.contains("god") && cfg.getBoolean("god") && plugin.getGodCommand() != null) {
            player.setInvulnerable(true);
            plugin.getGodCommand().restoreGod(uuid);
        }

        // Vanish — add to vanished set immediately (so join message is suppressed),
        // but do the full vanish setup 1 tick later so hidePlayer() is not overridden
        // by vanilla join logic.
        boolean shouldVanish = cfg.contains("vanish") && cfg.getBoolean("vanish") && plugin.getVanishCommand() != null;
        if (shouldVanish) {
            // Immediately mark as vanished so other listeners (join message, etc.) see it
            plugin.getVanishCommand().silentVanish(player);
            // Re-apply hidePlayer 1 tick later in case vanilla join re-shows the player
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.equals(player)) continue;
                    if (!onlinePlayer.hasPermission("kelpylandia.vanish.see")) {
                        onlinePlayer.hidePlayer(plugin, player);
                    }
                }
            }, 1L);
            // Delay notification so it shows after join sequence
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "You are still vanished from your previous session.");
                }
            }, 5L);
            plugin.getLogger().info("[StateManager] Restored vanish for " + player.getName());
        }

        // SocialSpy / CommandSpy
        if (plugin.getSpyManager() != null) {
            boolean ssRestored = false;
            boolean csRestored = false;
            if (cfg.contains("socialspy") && cfg.getBoolean("socialspy")) {
                plugin.getSpyManager().setSocialSpy(uuid, true);
                ssRestored = true;
            }
            if (cfg.contains("commandspy") && cfg.getBoolean("commandspy")) {
                plugin.getSpyManager().setCommandSpy(uuid, true);
                csRestored = true;
            }
            // Delay notifications so they show after join sequence
            final boolean ss = ssRestored;
            final boolean cs = csRestored;
            if (ss || cs) {
                plugin.getLogger().info("[StateManager] Restored spy for " + player.getName()
                    + " — socialspy=" + ss + " commandspy=" + cs);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (ss) player.sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SS" + ChatColor.GRAY + "] " + ChatColor.YELLOW + "SocialSpy is still enabled from your previous session.");
                    if (cs) player.sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "CS" + ChatColor.GRAY + "] " + ChatColor.YELLOW + "CommandSpy is still enabled from your previous session.");
                }, 5L);
            }
        }

        // Whisper target
        if (cfg.contains("whisper-target") && plugin.getMsgCommand() != null) {
            String targetStr = cfg.getString("whisper-target");
            if (targetStr != null) {
                try {
                    UUID targetUUID = UUID.fromString(targetStr);
                    plugin.getMsgCommand().setWhisperTarget(uuid, targetUUID);
                } catch (IllegalArgumentException ignored) {
                    // Invalid UUID, ignore
                }
            }
        }
    }
}
