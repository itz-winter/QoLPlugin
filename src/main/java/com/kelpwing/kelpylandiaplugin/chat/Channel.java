package com.kelpwing.kelpylandiaplugin.chat;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    
    private String name;
    private String displayName;
    private String format;
    private String permission;
    private boolean proximity;
    private double proximityDistance;
    private boolean discordEnabled;
    private String discordChannel;
    private boolean isDefault;
    private String prefix;
    private String suffix;
    private org.bukkit.ChatColor color;
    private List<String> allowedWorlds;
    
    // Full constructor
    public Channel(String name, String displayName, String format, String permission,
                   boolean proximity, double proximityDistance, boolean discordEnabled,
                   String discordChannel, boolean isDefault) {
        this.name = name;
        this.displayName = displayName;
        this.format = format;
        this.permission = permission;
        this.proximity = proximity;
        this.proximityDistance = proximityDistance;
        this.discordEnabled = discordEnabled;
        this.discordChannel = discordChannel;
        this.isDefault = isDefault;
        this.prefix = "";
        this.suffix = "";
        this.color = org.bukkit.ChatColor.WHITE;
        this.allowedWorlds = new ArrayList<>();
    }
    
    // Simple constructor for ChannelCommand
    public Channel(String name, String displayName) {
        this(name, displayName, "{prefix}{player}{suffix}: {message}", 
             "qol.chat." + name.toLowerCase(), false, 100.0, 
             false, null, false);
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFormat() {
        return format;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public boolean isProximity() {
        return proximity;
    }
    
    public double getProximityDistance() {
        return proximityDistance;
    }
    
    public boolean isDiscordEnabled() {
        return discordEnabled;
    }
    
    public String getDiscordChannel() {
        return discordChannel;
    }
    
    public boolean hasDiscordChannel() {
        return discordChannel != null;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getSuffix() {
        return suffix;
    }
    
    public org.bukkit.ChatColor getColor() {
        return color;
    }
    
    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    public void setProximity(boolean proximity) {
        this.proximity = proximity;
    }
    
    public void setProximityDistance(double proximityDistance) {
        this.proximityDistance = proximityDistance;
    }
    
    public void setDiscordEnabled(boolean discordEnabled) {
        this.discordEnabled = discordEnabled;
    }
    
    public void setDiscordChannel(String discordChannel) {
        this.discordChannel = discordChannel;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    
    public void setColor(org.bukkit.ChatColor color) {
        this.color = color;
    }
    
    public void setAllowedWorlds(List<String> allowedWorlds) {
        this.allowedWorlds = allowedWorlds;
    }
    
    // Convenience methods
    public String getFormattedDisplayName() {
        return color + displayName;
    }
    
    public boolean isGlobal() {
        return !isProximity();
    }
    
    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName);
    }
    
    public boolean isRangeEnabled() {
        return isProximity();
    }
    
    public double getRange() {
        return getProximityDistance();
    }
    
    public boolean isDefaultChannel() {
        return isDefault();
    }
    
    public void setGlobal(boolean global) {
        setProximity(!global);
    }
    
    public void setDefaultChannel(boolean defaultChannel) {
        setDefault(defaultChannel);
    }
    
    public void setRange(double range) {
        setProximityDistance(range);
    }
    
    public String getFormattedPrefix() {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix);
    }
    
    public String getFormattedSuffix() {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', suffix);
    }
    
    @Override
    public String toString() {
        return "Channel{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", proximity=" + proximity +
                ", discordEnabled=" + discordEnabled +
                '}';
    }
}
