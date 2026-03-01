package com.kelpwing.kelpylandiaplugin.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a player home with name, location, and optional icon.
 */
public class Home {

    private final String name;
    private final UUID ownerUUID;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private String icon; // Material name for GUI display
    private String description;
    private final long createdAt;

    public Home(String name, UUID ownerUUID, Location location) {
        this(name, ownerUUID, location, "OAK_SIGN", "", System.currentTimeMillis());
    }

    public Home(String name, UUID ownerUUID, Location location, String icon, String description, long createdAt) {
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "world";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.icon = icon;
        this.description = description;
        this.createdAt = createdAt;
    }

    /**
     * Reconstruct from stored data (no Location object needed).
     */
    public Home(String name, UUID ownerUUID, String worldName, double x, double y, double z,
                float yaw, float pitch, String icon, String description, long createdAt) {
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.icon = icon;
        this.description = description;
        this.createdAt = createdAt;
    }

    /**
     * Gets the Bukkit Location for this home, or null if the world doesn't exist.
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getName() { return name; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getCreatedAt() { return createdAt; }
}
