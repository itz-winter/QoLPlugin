package com.kelpwing.kelpylandiaplugin.homes;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player homes - loading, saving, and CRUD operations.
 * Homes are stored in homes.yml keyed by player UUID.
 */
public class HomeManager {

    private final KelpylandiaPlugin plugin;
    private File homesFile;
    private FileConfiguration homesConfig;

    // Cache: UUID -> (homeName -> Home)
    private final Map<UUID, Map<String, Home>> homeCache = new ConcurrentHashMap<>();

    public HomeManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        setupFile();
        loadAllHomes();
    }

    private void setupFile() {
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml!");
                e.printStackTrace();
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void loadAllHomes() {
        homeCache.clear();
        if (homesConfig.getKeys(false).isEmpty()) return;

        for (String uuidStr : homesConfig.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection playerSection = homesConfig.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            Map<String, Home> playerHomes = new LinkedHashMap<>();
            for (String homeName : playerSection.getKeys(false)) {
                ConfigurationSection homeSection = playerSection.getConfigurationSection(homeName);
                if (homeSection == null) continue;

                Home home = new Home(
                    homeName,
                    uuid,
                    homeSection.getString("world", "world"),
                    homeSection.getDouble("x", 0),
                    homeSection.getDouble("y", 64),
                    homeSection.getDouble("z", 0),
                    (float) homeSection.getDouble("yaw", 0),
                    (float) homeSection.getDouble("pitch", 0),
                    homeSection.getString("icon", "OAK_SIGN"),
                    homeSection.getString("description", ""),
                    homeSection.getLong("created-at", System.currentTimeMillis())
                );
                playerHomes.put(homeName.toLowerCase(), home);
            }
            homeCache.put(uuid, playerHomes);
        }

        plugin.getLogger().info("Loaded homes for " + homeCache.size() + " players.");
    }

    private void saveHomes(UUID uuid) {
        Map<String, Home> playerHomes = homeCache.get(uuid);
        String uuidStr = uuid.toString();

        // Clear the section first
        homesConfig.set(uuidStr, null);

        if (playerHomes != null && !playerHomes.isEmpty()) {
            for (Map.Entry<String, Home> entry : playerHomes.entrySet()) {
                Home home = entry.getValue();
                String path = uuidStr + "." + entry.getKey();
                homesConfig.set(path + ".world", home.getWorldName());
                homesConfig.set(path + ".x", home.getX());
                homesConfig.set(path + ".y", home.getY());
                homesConfig.set(path + ".z", home.getZ());
                homesConfig.set(path + ".yaw", home.getYaw());
                homesConfig.set(path + ".pitch", home.getPitch());
                homesConfig.set(path + ".icon", home.getIcon());
                homesConfig.set(path + ".description", home.getDescription());
                homesConfig.set(path + ".created-at", home.getCreatedAt());
            }
        }

        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml!");
            e.printStackTrace();
        }
    }

    // ===================== Public API =====================

    /**
     * Get the maximum number of homes a player can have.
     * Checks permission nodes kelpylandia.homes.max.<n> first, then config default.
     */
    public int getMaxHomes(Player player) {
        // Check permission-based limits (higher = better)
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("qol.homes.max." + i)) {
                return i;
            }
        }
        return plugin.getConfig().getInt("homes.max-homes", 12);
    }

    /**
     * Get all homes for a player.
     */
    public Map<String, Home> getHomes(UUID uuid) {
        return homeCache.getOrDefault(uuid, Collections.emptyMap());
    }

    /**
     * Get all homes for a player as an ordered list.
     */
    public List<Home> getHomeList(UUID uuid) {
        return new ArrayList<>(getHomes(uuid).values());
    }

    /**
     * Get a specific home by name.
     */
    public Home getHome(UUID uuid, String name) {
        return getHomes(uuid).get(name.toLowerCase());
    }

    /**
     * Check if a player has a home with the given name.
     */
    public boolean hasHome(UUID uuid, String name) {
        return getHomes(uuid).containsKey(name.toLowerCase());
    }

    /**
     * Set (create or overwrite) a home for a player.
     * Returns true on success, false if at max limit.
     */
    public boolean setHome(Player player, String name, Location location) {
        UUID uuid = player.getUniqueId();
        Map<String, Home> playerHomes = homeCache.computeIfAbsent(uuid, k -> new LinkedHashMap<>());

        String key = name.toLowerCase();
        boolean isOverwrite = playerHomes.containsKey(key);

        if (!isOverwrite && playerHomes.size() >= getMaxHomes(player)) {
            return false; // At max homes
        }

        Home home = new Home(name, uuid, location);
        playerHomes.put(key, home);
        saveHomes(uuid);
        return true;
    }

    /**
     * Delete a home by name.
     * Returns true if the home existed and was removed.
     */
    public boolean deleteHome(UUID uuid, String name) {
        Map<String, Home> playerHomes = homeCache.get(uuid);
        if (playerHomes == null) return false;

        Home removed = playerHomes.remove(name.toLowerCase());
        if (removed != null) {
            saveHomes(uuid);
            return true;
        }
        return false;
    }

    /**
     * Update the icon for a home.
     */
    public void setHomeIcon(UUID uuid, String homeName, String materialName) {
        Home home = getHome(uuid, homeName);
        if (home != null) {
            home.setIcon(materialName);
            saveHomes(uuid);
        }
    }

    /**
     * Update the description for a home.
     */
    public void setHomeDescription(UUID uuid, String homeName, String description) {
        Home home = getHome(uuid, homeName);
        if (home != null) {
            home.setDescription(description);
            saveHomes(uuid);
        }
    }

    /**
     * Get the number of homes a player has.
     */
    public int getHomeCount(UUID uuid) {
        return getHomes(uuid).size();
    }

    /**
     * Get all home names for a player.
     */
    public List<String> getHomeNames(UUID uuid) {
        return new ArrayList<>(getHomes(uuid).keySet());
    }
}
