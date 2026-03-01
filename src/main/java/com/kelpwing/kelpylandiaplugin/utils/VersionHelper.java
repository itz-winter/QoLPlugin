package com.kelpwing.kelpylandiaplugin.utils;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.inventory.InventoryType;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Utility class for detecting the Minecraft server version at runtime
 * and providing version-aware API helpers.
 * 
 * This allows the plugin to auto-adjust based on the detected Paper/Spigot version.
 */
public class VersionHelper {

    private static int majorVersion = -1;
    private static int minorVersion = -1;
    private static int patchVersion = -1;
    private static String fullVersionString = "Unknown";
    private static ServerPlatform platform = ServerPlatform.UNKNOWN;

    /**
     * Initialize version detection. Should be called once during plugin startup.
     */
    public static void init(Logger logger) {
        try {
            fullVersionString = Bukkit.getVersion();
            String bukkitVersion = Bukkit.getBukkitVersion(); // e.g. "1.21.11-R0.1-SNAPSHOT"

            // Detect platform
            String serverName = Bukkit.getName().toLowerCase();
            if (serverName.contains("paper") || serverName.contains("folia")) {
                platform = ServerPlatform.PAPER;
            } else if (serverName.contains("spigot")) {
                platform = ServerPlatform.SPIGOT;
            } else if (serverName.contains("craftbukkit")) {
                platform = ServerPlatform.CRAFTBUKKIT;
            } else {
                platform = ServerPlatform.UNKNOWN;
            }

            // Parse version from Bukkit version string (e.g. "1.21.11-R0.1-SNAPSHOT")
            String versionPart = bukkitVersion.split("-")[0]; // "1.21.11"
            String[] parts = versionPart.split("\\.");
            
            if (parts.length >= 2) {
                majorVersion = Integer.parseInt(parts[0]);  // 1
                minorVersion = Integer.parseInt(parts[1]);  // 21
            }
            if (parts.length >= 3) {
                patchVersion = Integer.parseInt(parts[2]);   // 11
            } else {
                patchVersion = 0;
            }

            logger.info("Detected server: " + platform.name() + " (" + fullVersionString + ")");
            logger.info("Minecraft version: " + majorVersion + "." + minorVersion + "." + patchVersion);
            logger.info("Bukkit API version: " + bukkitVersion);
        } catch (Exception e) {
            logger.warning("Failed to detect server version: " + e.getMessage());
            logger.warning("Some version-specific features may not work correctly.");
        }
    }

    /**
     * Get the Minecraft major version (e.g., 1 in "1.21.11").
     */
    public static int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Get the Minecraft minor version (e.g., 21 in "1.21.11").
     */
    public static int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Get the Minecraft patch version (e.g., 11 in "1.21.11").
     */
    public static int getPatchVersion() {
        return patchVersion;
    }

    /**
     * Get the full version string reported by Bukkit.getVersion().
     */
    public static String getFullVersionString() {
        return fullVersionString;
    }

    /**
     * Get the detected server platform (Paper, Spigot, etc.).
     */
    public static ServerPlatform getPlatform() {
        return platform;
    }

    /**
     * Check if the server is running Paper (or a Paper fork like Folia).
     */
    public static boolean isPaper() {
        return platform == ServerPlatform.PAPER;
    }

    /**
     * Check if the server is at least the given Minecraft version.
     * Example: isAtLeast(1, 21, 4) returns true for 1.21.4+ servers.
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (majorVersion > major) return true;
        if (majorVersion < major) return false;
        if (minorVersion > minor) return true;
        if (minorVersion < minor) return false;
        return patchVersion >= patch;
    }

    /**
     * Check if the server is at least the given Minecraft version (major.minor).
     * Example: isAtLeast(1, 21) returns true for 1.21+ servers.
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    /**
     * Returns a human-readable summary string for logging.
     */
    public static String getVersionSummary() {
        return String.format("Platform: %s | MC Version: %d.%d.%d | Full: %s",
                platform.name(), majorVersion, minorVersion, patchVersion, fullVersionString);
    }

    /**
     * Represents known server platforms.
     */
    public enum ServerPlatform {
        PAPER,
        SPIGOT,
        CRAFTBUKKIT,
        UNKNOWN
    }

    // ==================== Version Compatibility Helpers ====================

    /**
     * Gets the correct Attribute constant for max health across versions.
     * 1.21.4+: Attribute.MAX_HEALTH
     * 1.16 - 1.21.3: Attribute.GENERIC_MAX_HEALTH
     */
    @SuppressWarnings("deprecation")
    public static Attribute getMaxHealthAttribute() {
        // Try the new name first (1.21.4+), fall back to old name
        try {
            return Attribute.valueOf("MAX_HEALTH");
        } catch (IllegalArgumentException e) {
            try {
                return Attribute.valueOf("GENERIC_MAX_HEALTH");
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    /**
     * Gets the correct BanList.Type for name-based bans across versions.
     * 1.20.4+: BanList.Type.PROFILE
     * 1.16 - 1.20.3: BanList.Type.NAME
     */
    public static BanList.Type getNameBanListType() {
        // Try the new name first (1.20.4+), fall back to old name
        try {
            return BanList.Type.valueOf("PROFILE");
        } catch (IllegalArgumentException e) {
            try {
                return BanList.Type.valueOf("NAME");
            } catch (IllegalArgumentException e2) {
                return BanList.Type.NAME;
            }
        }
    }

    /**
     * Gets the display title of an advancement, handling API differences.
     * Spigot 1.18+: Advancement.getDisplay().getTitle()
     * Spigot 1.16-1.17: getDisplay() does not exist, use reflection
     * Returns null if the advancement has no display (recipes, etc.)
     */
    public static String getAdvancementTitle(Advancement advancement) {
        try {
            Method getDisplay = advancement.getClass().getMethod("getDisplay");
            Object display = getDisplay.invoke(advancement);
            if (display == null) return null;
            Method getTitle = display.getClass().getMethod("getTitle");
            return (String) getTitle.invoke(display);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the display description of an advancement, handling API differences.
     * Returns null if the advancement has no display.
     */
    public static String getAdvancementDescription(Advancement advancement) {
        try {
            Method getDisplay = advancement.getClass().getMethod("getDisplay");
            Object display = getDisplay.invoke(advancement);
            if (display == null) return null;
            Method getDescription = display.getClass().getMethod("getDescription");
            return (String) getDescription.invoke(display);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if an advancement has a display (is visible to players).
     * Uses reflection to support all server versions.
     */
    public static boolean hasAdvancementDisplay(Advancement advancement) {
        try {
            Method getDisplay = advancement.getClass().getMethod("getDisplay");
            return getDisplay.invoke(advancement) != null;
        } catch (Exception e) {
            // If getDisplay() doesn't exist at all, fall back to key-based check
            return !advancement.getKey().getKey().startsWith("recipes/");
        }
    }

    /**
     * Gets an InventoryType by name safely, returning null if it doesn't exist
     * on this server version. Useful for SMITHING (1.20+) etc.
     */
    public static InventoryType getInventoryType(String name) {
        try {
            return InventoryType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
