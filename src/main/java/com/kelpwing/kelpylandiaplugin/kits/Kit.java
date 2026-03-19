package com.kelpwing.kelpylandiaplugin.kits;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a kit with a name, items, cooldown, permission, and display properties.
 */
public class Kit {

    private final String name;
    private List<ItemStack> items;
    private long cooldownSeconds;     // 0 = one-time, -1 = no cooldown
    private String permission;        // null = no permission needed
    private Material displayIcon;
    private String description;
    private boolean giveOnFirstJoin;  // automatically given on first join
    private boolean oneTime;          // can only ever be claimed once

    public Kit(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.cooldownSeconds = 0;
        this.permission = null;
        this.displayIcon = Material.CHEST;
        this.description = "";
        this.giveOnFirstJoin = false;
        this.oneTime = false;
    }

    public Kit(String name, List<ItemStack> items, long cooldownSeconds, String permission,
               Material displayIcon, String description, boolean giveOnFirstJoin, boolean oneTime) {
        this.name = name;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.cooldownSeconds = cooldownSeconds;
        this.permission = permission;
        this.displayIcon = displayIcon != null ? displayIcon : Material.CHEST;
        this.description = description != null ? description : "";
        this.giveOnFirstJoin = giveOnFirstJoin;
        this.oneTime = oneTime;
    }

    public String getName() { return name; }

    public List<ItemStack> getItems() { return items; }
    public void setItems(List<ItemStack> items) { this.items = items != null ? new ArrayList<>(items) : new ArrayList<>(); }

    public long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public Material getDisplayIcon() { return displayIcon; }
    public void setDisplayIcon(Material displayIcon) { this.displayIcon = displayIcon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isGiveOnFirstJoin() { return giveOnFirstJoin; }
    public void setGiveOnFirstJoin(boolean giveOnFirstJoin) { this.giveOnFirstJoin = giveOnFirstJoin; }

    public boolean isOneTime() { return oneTime; }
    public void setOneTime(boolean oneTime) { this.oneTime = oneTime; }

    /**
     * Whether this kit has no cooldown (can be claimed any time).
     */
    public boolean hasNoCooldown() {
        return cooldownSeconds < 0;
    }

    /**
     * Get a formatted cooldown string for display.
     */
    public String getCooldownDisplay() {
        if (oneTime) return "One-time";
        if (cooldownSeconds < 0) return "No cooldown";
        if (cooldownSeconds == 0) return "No cooldown";

        long seconds = cooldownSeconds;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 && days == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
