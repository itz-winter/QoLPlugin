package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
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
        if (plugin.getVanishCommand() != null) {
            cfg.set("vanish", plugin.getVanishCommand().isVanished(player));
        }

        // SocialSpy / CommandSpy
        if (plugin.getSpyManager() != null) {
            cfg.set("socialspy", plugin.getSpyManager().isSocialSpy(uuid));
            cfg.set("commandspy", plugin.getSpyManager().isCommandSpy(uuid));
        }

        // Whisper target
        if (plugin.getMsgCommand() != null) {
            UUID wt = plugin.getMsgCommand().getWhisperTarget(uuid);
            cfg.set("whisper-target", wt != null ? wt.toString() : null);
        }

        try {
            cfg.save(file);
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

        // Vanish — re-vanish the player silently (no fake leave message)
        if (cfg.contains("vanish") && cfg.getBoolean("vanish") && plugin.getVanishCommand() != null) {
            plugin.getVanishCommand().silentVanish(player);
        }

        // SocialSpy / CommandSpy
        if (plugin.getSpyManager() != null) {
            if (cfg.contains("socialspy") && cfg.getBoolean("socialspy")) {
                plugin.getSpyManager().setSocialSpy(uuid, true);
            }
            if (cfg.contains("commandspy") && cfg.getBoolean("commandspy")) {
                plugin.getSpyManager().setCommandSpy(uuid, true);
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
