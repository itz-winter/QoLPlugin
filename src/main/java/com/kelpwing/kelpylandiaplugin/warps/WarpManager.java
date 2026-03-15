package com.kelpwing.kelpylandiaplugin.warps;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server warps stored in warps.yml.
 */
public class WarpManager {

    private final KelpylandiaPlugin plugin;
    private File warpsFile;
    private FileConfiguration warpsConfig;
    private final Map<String, Location> warps = new ConcurrentHashMap<>();

    public WarpManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        setupFile();
        loadWarps();
    }

    private void setupFile() {
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                warpsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create warps.yml!");
                e.printStackTrace();
            }
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
    }

    private void loadWarps() {
        warps.clear();
        if (warpsConfig.getKeys(false).isEmpty()) return;

        for (String name : warpsConfig.getKeys(false)) {
            ConfigurationSection section = warpsConfig.getConfigurationSection(name);
            if (section == null) continue;

            String worldName = section.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Location loc = new Location(
                world,
                section.getDouble("x", 0),
                section.getDouble("y", 64),
                section.getDouble("z", 0),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0)
            );
            warps.put(name.toLowerCase(), loc);
        }
        plugin.getLogger().info("Loaded " + warps.size() + " warps.");
    }

    public boolean setWarp(String name, Location location) {
        String key = name.toLowerCase();
        warps.put(key, location.clone());

        warpsConfig.set(key + ".world", location.getWorld().getName());
        warpsConfig.set(key + ".x", location.getX());
        warpsConfig.set(key + ".y", location.getY());
        warpsConfig.set(key + ".z", location.getZ());
        warpsConfig.set(key + ".yaw", location.getYaw());
        warpsConfig.set(key + ".pitch", location.getPitch());
        return save();
    }

    public boolean deleteWarp(String name) {
        String key = name.toLowerCase();
        if (!warps.containsKey(key)) return false;
        warps.remove(key);
        warpsConfig.set(key, null);
        return save();
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public boolean warpExists(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public Set<String> getWarpNames() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    public int getWarpCount() {
        return warps.size();
    }

    private boolean save() {
        try {
            warpsConfig.save(warpsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml!");
            e.printStackTrace();
            return false;
        }
    }
}
