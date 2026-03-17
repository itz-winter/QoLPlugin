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
 * across player relogs. Data is stored in plugins/&lt;pluginFolder&gt;/playerdata/&lt;uuid&gt;.yml.
 *
 * <p><b>Key design:</b> Every toggle change is written to disk immediately via
 * {@link #saveToggle(UUID, String, boolean)} so that persistence does not depend
 * on event priority ordering during quit. The full {@link #saveState(Player)} call
 * on quit is kept as a safety net for states that are not individually toggled (fly etc.).</p>
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

    // ─── Immediate per-key save (called from toggle commands) ──────

    /**
     * Immediately persist a single boolean toggle to the player's data file.
     * This is the primary persistence mechanism — called by VanishCommand,
     * SpyManager toggles, GodCommand, etc. at the moment the state changes.
     */
    public void saveToggle(UUID uuid, String key, boolean value) {
        if (!plugin.getConfig().getBoolean("state-persistence.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("state-persistence." + key, true)) return;

        File file = getPlayerFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set(key, value);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[StateManager] Could not save " + key + " for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Read a boolean toggle from the player's data file. Returns the
     * fallback value if the key does not exist or the file is missing.
     */
    public boolean readToggle(UUID uuid, String key, boolean fallback) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return fallback;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return cfg.getBoolean(key, fallback);
    }

    // ─── Full save on quit (safety net) ────────────────────────────

    /**
     * Save all relevant toggle states for a player. Called on quit as a
     * safety net — the individual toggles should already be written.
     */
    public void saveState(Player player) {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Fly (only saved here — no toggle-time save for fly)
        cfg.set("fly", player.getAllowFlight());

        // God — also written at toggle time, but write again as safety
        if (plugin.getGodCommand() != null) {
            cfg.set("god", plugin.getGodCommand().isGod(player));
        }

        // Vanish — already written at toggle time, write again as safety
        if (plugin.getVanishCommand() != null) {
            cfg.set("vanish", plugin.getVanishCommand().isVanished(player));
        }

        // SocialSpy / CommandSpy — already written at toggle time
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

    // ─── Restore on join ───────────────────────────────────────────

    /**
     * Restore all relevant toggle states for a player on join.
     */
    public void restoreState(Player player) {
        if (!plugin.getConfig().getBoolean("state-persistence.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid);
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Fly
        if (plugin.getConfig().getBoolean("state-persistence.fly", true)
                && cfg.getBoolean("fly", false)) {
            player.setAllowFlight(true);
        }

        // God
        if (plugin.getConfig().getBoolean("state-persistence.god", true)
                && cfg.getBoolean("god", false) && plugin.getGodCommand() != null) {
            player.setInvulnerable(true);
            plugin.getGodCommand().restoreGod(uuid);
        }

        // Vanish — add to vanished set immediately (so join message is suppressed),
        // then re-apply hidePlayer 1 tick later so vanilla join doesn't override it.
        boolean shouldVanish = plugin.getConfig().getBoolean("state-persistence.vanish", true)
                && cfg.getBoolean("vanish", false) && plugin.getVanishCommand() != null;
        if (shouldVanish) {
            plugin.getVanishCommand().silentVanish(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.equals(player)) continue;
                    if (!onlinePlayer.hasPermission("kelpylandia.vanish.see")) {
                        onlinePlayer.hidePlayer(plugin, player);
                    }
                }
            }, 1L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "You are still vanished from your previous session.");
                }
            }, 5L);
            plugin.getLogger().info("[StateManager] Restored vanish for " + player.getName());
        }

        // SocialSpy / CommandSpy
        if (plugin.getSpyManager() != null) {
            boolean ssRestored = plugin.getConfig().getBoolean("state-persistence.socialspy", true)
                    && cfg.getBoolean("socialspy", false);
            boolean csRestored = plugin.getConfig().getBoolean("state-persistence.commandspy", true)
                    && cfg.getBoolean("commandspy", false);
            if (ssRestored) plugin.getSpyManager().setSocialSpy(uuid, true);
            if (csRestored) plugin.getSpyManager().setCommandSpy(uuid, true);
            if (ssRestored || csRestored) {
                plugin.getLogger().info("[StateManager] Restored spy for " + player.getName()
                    + " — socialspy=" + ssRestored + " commandspy=" + csRestored);
                final boolean ss = ssRestored;
                final boolean cs = csRestored;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (ss) player.sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SS" + ChatColor.GRAY + "] " + ChatColor.YELLOW + "SocialSpy is still enabled from your previous session.");
                    if (cs) player.sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "CS" + ChatColor.GRAY + "] " + ChatColor.YELLOW + "CommandSpy is still enabled from your previous session.");
                }, 5L);
            }
        }

        // Whisper target
        if (plugin.getMsgCommand() != null) {
            String targetStr = cfg.getString("whisper-target");
            if (targetStr != null) {
                try {
                    UUID targetUUID = UUID.fromString(targetStr);
                    plugin.getMsgCommand().setWhisperTarget(uuid, targetUUID);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
