package com.kelpwing.kelpylandiaplugin.teleport;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages previous locations and death locations for /back and /dback commands.
 * Stores the last location a player was at before teleporting, and
 * the last location a player died at.
 */
public class BackManager {

    private final KelpylandiaPlugin plugin;

    // Previous location before last teleport: playerUUID -> location
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();

    // Last death location: playerUUID -> location
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    // Cooldowns: playerUUID -> cooldown expiry timestamp
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BackManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    // ===================== Previous Location =====================

    /**
     * Save a player's previous location (called before teleport).
     */
    public void savePreviousLocation(UUID playerUUID, Location location) {
        if (location != null) {
            previousLocations.put(playerUUID, location.clone());
        }
    }

    /**
     * Get a player's previous location.
     */
    public Location getPreviousLocation(UUID playerUUID) {
        return previousLocations.get(playerUUID);
    }

    /**
     * Check if a player has a previous location saved.
     */
    public boolean hasPreviousLocation(UUID playerUUID) {
        return previousLocations.containsKey(playerUUID);
    }

    // ===================== Death Location =====================

    /**
     * Save a player's death location.
     */
    public void saveDeathLocation(UUID playerUUID, Location location) {
        if (location != null) {
            deathLocations.put(playerUUID, location.clone());
        }
    }

    /**
     * Get a player's last death location.
     */
    public Location getDeathLocation(UUID playerUUID) {
        return deathLocations.get(playerUUID);
    }

    /**
     * Check if a player has a death location saved.
     */
    public boolean hasDeathLocation(UUID playerUUID) {
        return deathLocations.containsKey(playerUUID);
    }

    // ===================== Cooldowns =====================

    /**
     * Check if a player is on cooldown.
     */
    public boolean isOnCooldown(UUID playerUUID) {
        Long expiry = cooldowns.get(playerUUID);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(playerUUID);
            return false;
        }
        return true;
    }

    /**
     * Get remaining cooldown time in seconds.
     */
    public int getRemainingCooldown(UUID playerUUID) {
        Long expiry = cooldowns.get(playerUUID);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /**
     * Apply cooldown to a player.
     */
    public void applyCooldown(UUID playerUUID) {
        int cooldownSeconds = plugin.getConfig().getInt("back.cooldown", 3);
        if (cooldownSeconds > 0) {
            cooldowns.put(playerUUID, System.currentTimeMillis() + (cooldownSeconds * 1000L));
        }
    }

    // ===================== Cleanup =====================

    /**
     * Remove all data for a player (called on quit to prevent memory leaks).
     */
    public void cleanup(UUID playerUUID) {
        previousLocations.remove(playerUUID);
        deathLocations.remove(playerUUID);
        cooldowns.remove(playerUUID);
    }
}
