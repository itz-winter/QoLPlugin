package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Warning;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileManager {
    
    private final KelpylandiaPlugin plugin;
    private File warningsFile;
    private FileConfiguration warningsConfig;
    private File punishmentsFile;
    private FileConfiguration punishmentsConfig;
    private File bansFile;
    private FileConfiguration bansConfig;
    
    public FileManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
    }
    
    private void setupFiles() {
        // Create warnings file
        warningsFile = new File(plugin.getDataFolder(), "warnings.yml");
        if (!warningsFile.exists()) {
            try {
                warningsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create warnings.yml file!");
                e.printStackTrace();
            }
        }
        warningsConfig = YamlConfiguration.loadConfiguration(warningsFile);
        
        // Create punishments file
        punishmentsFile = new File(plugin.getDataFolder(), "punishments.yml");
        if (!punishmentsFile.exists()) {
            try {
                punishmentsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create punishments.yml file!");
                e.printStackTrace();
            }
        }
        sanitizeYamlTags(punishmentsFile);
        punishmentsConfig = YamlConfiguration.loadConfiguration(punishmentsFile);
        
        // Create bans file
        bansFile = new File(plugin.getDataFolder(), "bans.yml");
        if (!bansFile.exists()) {
            try {
                bansFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create bans.yml file!");
                e.printStackTrace();
            }
        }
        bansConfig = YamlConfiguration.loadConfiguration(bansFile);
    }
    
    /**
     * Strip disallowed YAML tags like !!java.time.LocalDateTime from a file.
     * Newer Paper/SnakeYAML versions block deserialization of arbitrary Java types.
     * This rewrites the file in-place so loadConfiguration() can succeed.
     */
    private void sanitizeYamlTags(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            // Match !!java.time.LocalDateTime '...' or !!java.time.LocalDateTime {}
            // and replace with just the quoted value or empty string
            String sanitized = content.replaceAll(
                    "!!java\\.time\\.LocalDateTime '([^']*)'", "'$1'"
            ).replaceAll(
                    "!!java\\.time\\.LocalDateTime \\{}", "'unknown'"
            ).replaceAll(
                    "!!java\\.time\\.LocalDateTime \"([^\"]*)\"", "'$1'"
            );
            if (!sanitized.equals(content)) {
                Files.writeString(file.toPath(), sanitized, StandardCharsets.UTF_8);
                plugin.getLogger().info("Sanitized invalid YAML tags in " + file.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not sanitize " + file.getName() + ": " + e.getMessage());
        }
    }
    
    public void saveWarning(Warning warning) {
        String path = "warnings." + warning.getId();
        warningsConfig.set(path + ".playerUUID", warning.getPlayerUUID().toString());
        warningsConfig.set(path + ".playerName", warning.getPlayerName());
        warningsConfig.set(path + ".moderatorUUID", warning.getModeratorUUID().toString());
        warningsConfig.set(path + ".moderatorName", warning.getModeratorName());
        warningsConfig.set(path + ".reason", warning.getReason());
        warningsConfig.set(path + ".timestamp", warning.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        warningsConfig.set(path + ".active", warning.isActive());
        warningsConfig.set(path + ".server", warning.getServer());
        warningsConfig.set(path + ".expiration", warning.getExpiration());
        
        try {
            warningsConfig.save(warningsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warnings.yml file!");
            e.printStackTrace();
        }
    }
    
    /**
     * Save a warning (overloaded for command compatibility)
     */
    public void saveWarning(String playerName, String reason, String warner, long expiration) {
        // Create a Warning object with UUID and save it
        Warning warning = new Warning();
        warning.setId(UUID.randomUUID().toString());
        warning.setPlayerName(playerName);
        warning.setReason(reason);
        warning.setModeratorName(warner); // For the warner field
        warning.setTimestamp(LocalDateTime.now());
        warning.setActive(true);
        warning.setServer("Kelpylandia");
        warning.setExpiration(expiration);
        
        // Set UUIDs to random values if we don't have them
        warning.setPlayerUUID(UUID.randomUUID());
        warning.setModeratorUUID(UUID.randomUUID());
        
        saveWarning(warning);
    }
    
    public List<Warning> getActiveWarnings(String playerName) {
        List<Warning> activeWarnings = new ArrayList<>();
        
        if (warningsConfig.getConfigurationSection("warnings") == null) {
            return activeWarnings;
        }
        
        for (String key : warningsConfig.getConfigurationSection("warnings").getKeys(false)) {
            String path = "warnings." + key;
            
            // Check if this warning is for the specified player and is still active
            String configPlayerName = warningsConfig.getString(path + ".playerName");
            boolean isActive = warningsConfig.getBoolean(path + ".active", true);
            long expiration = warningsConfig.getLong(path + ".expiration", 0);
            
            if (configPlayerName != null && configPlayerName.equalsIgnoreCase(playerName) && 
                isActive && (expiration == 0 || System.currentTimeMillis() < expiration)) {
                
                try {
                    Warning warning = new Warning();
                    warning.setId(key);
                    warning.setPlayerUUID(UUID.fromString(warningsConfig.getString(path + ".playerUUID")));
                    warning.setPlayerName(configPlayerName);
                    warning.setModeratorUUID(UUID.fromString(warningsConfig.getString(path + ".moderatorUUID")));
                    warning.setModeratorName(warningsConfig.getString(path + ".moderatorName"));
                    warning.setReason(warningsConfig.getString(path + ".reason"));
                    warning.setTimestamp(LocalDateTime.parse(warningsConfig.getString(path + ".timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    warning.setActive(isActive);
                    warning.setServer(warningsConfig.getString(path + ".server"));
                    warning.setExpiration(expiration);
                    
                    activeWarnings.add(warning);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load warning " + key + ": " + e.getMessage());
                }
            }
        }
        
        return activeWarnings;
    }
    
    public List<Warning> getAllWarnings(String playerName) {
        List<Warning> allWarnings = new ArrayList<>();
        
        if (warningsConfig.getConfigurationSection("warnings") == null) {
            return allWarnings;
        }
        
        for (String key : warningsConfig.getConfigurationSection("warnings").getKeys(false)) {
            String path = "warnings." + key;
            
            String configPlayerName = warningsConfig.getString(path + ".playerName");
            if (configPlayerName != null && configPlayerName.equalsIgnoreCase(playerName)) {
                try {
                    Warning warning = new Warning();
                    warning.setId(key);
                    warning.setPlayerUUID(UUID.fromString(warningsConfig.getString(path + ".playerUUID")));
                    warning.setPlayerName(configPlayerName);
                    warning.setModeratorUUID(UUID.fromString(warningsConfig.getString(path + ".moderatorUUID")));
                    warning.setModeratorName(warningsConfig.getString(path + ".moderatorName"));
                    warning.setReason(warningsConfig.getString(path + ".reason"));
                    warning.setTimestamp(LocalDateTime.parse(warningsConfig.getString(path + ".timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    warning.setActive(warningsConfig.getBoolean(path + ".active", true));
                    warning.setServer(warningsConfig.getString(path + ".server"));
                    warning.setExpiration(warningsConfig.getLong(path + ".expiration", 0));
                    
                    allWarnings.add(warning);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load warning " + key + ": " + e.getMessage());
                }
            }
        }
        
        return allWarnings;
    }
    
    public void cleanupExpiredWarnings() {
        if (warningsConfig.getConfigurationSection("warnings") == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        List<String> expiredWarnings = new ArrayList<>();
        
        for (String key : warningsConfig.getConfigurationSection("warnings").getKeys(false)) {
            String path = "warnings." + key;
            long expiration = warningsConfig.getLong(path + ".expiration", 0);
            
            if (expiration > 0 && currentTime > expiration) {
                warningsConfig.set(path + ".active", false);
                expiredWarnings.add(key);
            }
        }
        
        if (!expiredWarnings.isEmpty()) {
            try {
                warningsConfig.save(warningsFile);
                plugin.getLogger().info("Deactivated " + expiredWarnings.size() + " expired warnings.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save warnings.yml file after cleanup!");
                e.printStackTrace();
            }
        }
    }
    
    public void removeWarning(String warningId) {
        if (warningsConfig.contains("warnings." + warningId)) {
            warningsConfig.set("warnings." + warningId + ".active", false);
            try {
                warningsConfig.save(warningsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save warnings.yml file!");
                e.printStackTrace();
            }
        }
    }
    
    public Warning getWarning(String warningId) {
        String path = "warnings." + warningId;
        if (!warningsConfig.contains(path)) {
            return null;
        }
        
        try {
            Warning warning = new Warning();
            warning.setId(warningId);
            warning.setPlayerUUID(UUID.fromString(warningsConfig.getString(path + ".playerUUID")));
            warning.setPlayerName(warningsConfig.getString(path + ".playerName"));
            warning.setModeratorUUID(UUID.fromString(warningsConfig.getString(path + ".moderatorUUID")));
            warning.setModeratorName(warningsConfig.getString(path + ".moderatorName"));
            warning.setReason(warningsConfig.getString(path + ".reason"));
            warning.setTimestamp(LocalDateTime.parse(warningsConfig.getString(path + ".timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            warning.setActive(warningsConfig.getBoolean(path + ".active", true));
            warning.setServer(warningsConfig.getString(path + ".server"));
            warning.setExpiration(warningsConfig.getLong(path + ".expiration", 0));
            
            return warning;
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load warning " + warningId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Save a punishment to the punishments file
     */
    public void savePunishment(Punishment punishment) {
        // Use UUID if available, otherwise use player name as fallback
        String identifier;
        if (punishment.getPlayerUUID() != null) {
            identifier = punishment.getPlayerUUID().toString();
        } else {
            identifier = "name_" + punishment.getPlayer(); // Use name-based identifier
        }
        
        int nextId = punishmentsConfig.getInt("punishments." + identifier + ".next-id", 1);
        
        String path = "punishments." + identifier + "." + nextId;
        punishmentsConfig.set(path + ".type", punishment.getType().toString());
        punishmentsConfig.set(path + ".reason", punishment.getReason());
        punishmentsConfig.set(path + ".punisher", punishment.getPunisher());
        punishmentsConfig.set(path + ".timestamp", punishment.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (punishment.getDuration() > 0) {
            punishmentsConfig.set(path + ".duration", punishment.getDuration());
        }
        punishmentsConfig.set(path + ".active", punishment.isActive());
        
        punishmentsConfig.set("punishments." + identifier + ".next-id", nextId + 1);
        
        try {
            punishmentsConfig.save(punishmentsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save punishment to file!");
            e.printStackTrace();
        }
    }
    
    /**
     * Save a punishment to the punishments file (overloaded for command compatibility)
     */
    public void savePunishment(String playerName, String action, String reason, String punisher, long duration) {
        // Create a Punishment object and save it
        Punishment punishment = new Punishment(playerName, punisher, reason, action, duration);
        savePunishment(punishment);
    }
    
    /**
     * Save a punishment to the punishments file (overloaded for command compatibility with int duration)
     */
    public void savePunishment(String playerName, String action, String reason, String punisher, int duration) {
        savePunishment(playerName, action, reason, punisher, (long) duration);
    }
    
    /**
     * Remove a ban from the bans file
     */
    public void removeBan(UUID playerUUID) {
        String path = "bans." + playerUUID.toString();
        bansConfig.set(path, null);
        
        try {
            bansConfig.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not remove ban from file!");
            e.printStackTrace();
        }
    }
    
    /**
     * Remove a ban from the bans file (overloaded for player name compatibility)
     */
    public boolean removeBan(String playerName) {
        // Search for bans by player name since we don't have UUID
        boolean removed = false;
        
        if (bansConfig.getConfigurationSection("bans") != null) {
            for (String key : bansConfig.getConfigurationSection("bans").getKeys(false)) {
                String path = "bans." + key;
                String configPlayerName = bansConfig.getString(path + ".playerName");
                if (configPlayerName != null && configPlayerName.equalsIgnoreCase(playerName)) {
                    bansConfig.set(path, null);
                    removed = true;
                }
            }
        }
        
        if (removed) {
            try {
                bansConfig.save(bansFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not remove ban from file!");
                e.printStackTrace();
                return false;
            }
        }
        
        return removed;
    }
    
    /**
     * Save an IP ban to the bans file
     */
    public void saveIPBan(String ipAddress, String reason, String punisher, long timestamp) {
        String path = "ip-bans." + ipAddress.replace(".", "_");
        bansConfig.set(path + ".reason", reason);
        bansConfig.set(path + ".punisher", punisher);
        bansConfig.set(path + ".timestamp", timestamp);
        
        try {
            bansConfig.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save IP ban to file!");
            e.printStackTrace();
        }
    }
    
    /**
     * Save an IP ban to the bans file (overloaded for command compatibility)
     */
    public void saveIPBan(String ipAddress, String playerName, String reason, String punisher, long duration) {
        String path = "ip-bans." + ipAddress.replace(".", "_");
        bansConfig.set(path + ".playerName", playerName);
        bansConfig.set(path + ".reason", reason);
        bansConfig.set(path + ".punisher", punisher);
        bansConfig.set(path + ".timestamp", System.currentTimeMillis());
        if (duration > 0) {
            bansConfig.set(path + ".duration", duration);
        }
        
        try {
            bansConfig.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save IP ban to file!");
            e.printStackTrace();
        }
    }
    
    /**
     * Get player history from punishments file
     */
    public List<Punishment> getPlayerHistory(UUID playerUUID) {
        List<Punishment> history = new ArrayList<>();
        String playerSection = "punishments." + playerUUID.toString();
        
        if (!punishmentsConfig.contains(playerSection)) {
            return history;
        }
        
        ConfigurationSection playerConfig = punishmentsConfig.getConfigurationSection(playerSection);
        if (playerConfig == null) return history;
        
        for (String key : playerConfig.getKeys(false)) {
            if (key.equals("next-id")) continue;
            
            ConfigurationSection punishmentSection = playerConfig.getConfigurationSection(key);
            if (punishmentSection == null) continue;
            
            try {
                String type = punishmentSection.getString("type", "UNKNOWN");
                String reason = punishmentSection.getString("reason", "No reason provided");
                String punisher = punishmentSection.getString("punisher", "Console");
                long duration = punishmentSection.getLong("duration", 0);
                boolean active = punishmentSection.getBoolean("active", true);
                
                // Use the compatible constructor with String parameters
                Punishment punishment = new Punishment(playerUUID.toString(), punisher, reason, type, duration);
                punishment.setActive(active);
                history.add(punishment);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading punishment data for player " + playerUUID + ": " + e.getMessage());
            }
        }
        
        return history;
    }
    
    /**
     * Get player history from punishments file (overloaded for player name compatibility)
     * Returns legacy Map format for backward compatibility
     */
    public List<Map<String, String>> getPlayerHistory(String playerName) {
        List<Map<String, String>> history = new ArrayList<>();
        List<Punishment> punishments = getPlayerHistoryPunishments(playerName);
        
        for (Punishment punishment : punishments) {
            Map<String, String> record = new HashMap<>();
            record.put("action", punishment.getAction());
            record.put("reason", punishment.getReason());
            record.put("punisher", punishment.getPunisher());
            record.put("player", punishment.getPlayer());
            record.put("duration", String.valueOf(punishment.getDuration()));
            record.put("timestamp", String.valueOf(punishment.getTimestamp()));
            record.put("active", String.valueOf(punishment.isActive()));
            
            history.add(record);
        }
        
        return history;
    }
    
    /**
     * Get player history as Punishment objects (for internal use)
     */
    public List<Punishment> getPlayerHistoryPunishments(String playerName) {
        List<Punishment> history = new ArrayList<>();
        
        // Search through all punishments to find ones matching the player name
        if (punishmentsConfig.getConfigurationSection("punishments") != null) {
            for (String playerKey : punishmentsConfig.getConfigurationSection("punishments").getKeys(false)) {
                ConfigurationSection playerConfig = punishmentsConfig.getConfigurationSection("punishments." + playerKey);
                if (playerConfig == null) continue;
                
                for (String key : playerConfig.getKeys(false)) {
                    if (key.equals("next-id")) continue;
                    
                    ConfigurationSection punishmentSection = playerConfig.getConfigurationSection(key);
                    if (punishmentSection == null) continue;
                    
                    try {
                        String configPlayerName = punishmentSection.getString("playerName");
                        if (configPlayerName != null && configPlayerName.equalsIgnoreCase(playerName)) {
                            String type = punishmentSection.getString("type", "UNKNOWN");
                            String reason = punishmentSection.getString("reason", "No reason provided");
                            String punisher = punishmentSection.getString("punisher", "Console");
                            long duration = punishmentSection.getLong("duration", 0);
                            boolean active = punishmentSection.getBoolean("active", true);
                            
                            Punishment punishment = new Punishment(playerName, punisher, reason, type, duration);
                            punishment.setActive(active);
                            history.add(punishment);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error loading punishment data: " + e.getMessage());
                    }
                }
            }
        }
        
        return history;
    }
    
    /**
     * Get player history from punishments file (legacy format for backward compatibility)
     */
    public List<Map<String, String>> getPlayerHistoryLegacy(String playerName) {
        List<Map<String, String>> history = new ArrayList<>();
        List<Punishment> punishments = getPlayerHistoryPunishments(playerName);
        
        for (Punishment punishment : punishments) {
            Map<String, String> record = new HashMap<>();
            record.put("action", punishment.getAction());
            record.put("reason", punishment.getReason());
            record.put("punisher", punishment.getPunisher());
            record.put("player", punishment.getPlayer());
            record.put("duration", String.valueOf(punishment.getDuration()));
            record.put("timestamp", String.valueOf(punishment.getTimestamp()));
            record.put("active", String.valueOf(punishment.isActive()));
            
            history.add(record);
        }
        
        return history;
    }
    
    /**
     * Get warnings for a player using UUID (compatible with original method signature)
     */
    public List<Warning> getWarnings(UUID playerUUID) {
        List<Warning> warnings = new ArrayList<>();
        
        if (warningsConfig.getConfigurationSection("warnings") == null) {
            return warnings;
        }
        
        for (String key : warningsConfig.getConfigurationSection("warnings").getKeys(false)) {
            String path = "warnings." + key;
            
            String configPlayerUUID = warningsConfig.getString(path + ".playerUUID");
            if (configPlayerUUID != null && configPlayerUUID.equals(playerUUID.toString())) {
                try {
                    Warning warning = new Warning();
                    warning.setId(key);
                    warning.setPlayerUUID(playerUUID);
                    warning.setPlayerName(warningsConfig.getString(path + ".playerName"));
                    warning.setModeratorUUID(UUID.fromString(warningsConfig.getString(path + ".moderatorUUID")));
                    warning.setModeratorName(warningsConfig.getString(path + ".moderatorName"));
                    warning.setReason(warningsConfig.getString(path + ".reason"));
                    warning.setTimestamp(LocalDateTime.parse(warningsConfig.getString(path + ".timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    warning.setActive(warningsConfig.getBoolean(path + ".active", true));
                    warning.setServer(warningsConfig.getString(path + ".server"));
                    warning.setExpiration(warningsConfig.getLong(path + ".expiration", 0));
                    
                    warnings.add(warning);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load warning " + key + ": " + e.getMessage());
                }
            }
        }
        
        return warnings;
    }
    
    /**
     * Remove the latest warning for a player
     */
    public boolean removeLatestWarning(UUID playerUUID) {
        List<Warning> warnings = getWarnings(playerUUID);
        if (warnings.isEmpty()) {
            return false;
        }
        
        // Find the most recent warning (highest timestamp)
        Warning latestWarning = null;
        for (Warning warning : warnings) {
            if (latestWarning == null || warning.getTimestamp().isAfter(latestWarning.getTimestamp())) {
                latestWarning = warning;
            }
        }
        
        if (latestWarning != null) {
            // Deactivate the latest warning
            warningsConfig.set("warnings." + latestWarning.getId() + ".active", false);
            
            try {
                warningsConfig.save(warningsFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save warnings to file!");
                e.printStackTrace();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Remove the latest warning for a player (overloaded for player name compatibility)
     */
    public boolean removeLatestWarning(String playerName) {
        List<Warning> warnings = getAllWarnings(playerName);
        if (warnings.isEmpty()) {
            return false;
        }
        
        // Find the most recent warning (highest timestamp)
        Warning latestWarning = null;
        for (Warning warning : warnings) {
            if (latestWarning == null || warning.getTimestamp().isAfter(latestWarning.getTimestamp())) {
                latestWarning = warning;
            }
        }
        
        if (latestWarning != null) {
            // Deactivate the latest warning
            warningsConfig.set("warnings." + latestWarning.getId() + ".active", false);
            
            try {
                warningsConfig.save(warningsFile);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save warnings to file!");
                e.printStackTrace();
                return false;
            }
        }
        
        return false;
    }
}
