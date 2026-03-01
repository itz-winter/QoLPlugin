package com.kelpwing.kelpylandiaplugin.moderation.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Punishment {
    private String id;
    private UUID playerUUID;
    private String playerName;
    private UUID moderatorUUID;
    private String moderatorName;
    private String reason;
    private PunishmentType type;
    private LocalDateTime timestamp;
    private LocalDateTime expires;
    private boolean active;
    private String server;
    private String action; // For compatibility
    private String punisher; // For compatibility
    private String player; // For compatibility
    private long duration; // For compatibility

    public enum PunishmentType {
        BAN, KICK, MUTE, WARN, IPBAN
    }

    // Constructors
    public Punishment() {}

    public Punishment(UUID playerUUID, String playerName, UUID moderatorUUID, String moderatorName, 
                     String reason, PunishmentType type) {
        this.id = UUID.randomUUID().toString();
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.moderatorUUID = moderatorUUID;
        this.moderatorName = moderatorName;
        this.reason = reason;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.active = true;
        this.server = "Kelpylandia";
        
        // Set compatibility fields
        this.action = type.name();
        this.punisher = moderatorName;
        this.player = playerName;
        this.duration = 0;
    }

    public Punishment(UUID playerUUID, String playerName, UUID moderatorUUID, String moderatorName, 
                     String reason, PunishmentType type, LocalDateTime expires) {
        this(playerUUID, playerName, moderatorUUID, moderatorName, reason, type);
        this.expires = expires;
        if (expires != null) {
            this.duration = java.time.Duration.between(timestamp, expires).toMinutes();
        }
    }

    // Constructor for backward compatibility with old calls
    public Punishment(String player, String punisher, String reason, String action, long duration) {
        this.id = UUID.randomUUID().toString();
        this.player = player;
        this.playerName = player;
        this.punisher = punisher;
        this.moderatorName = punisher;
        this.reason = reason;
        this.action = action;
        this.duration = duration;
        this.timestamp = LocalDateTime.now();
        this.active = true;
        this.server = "Kelpylandia";
        
        // Try to get UUID from online player
        try {
            org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayerExact(player);
            if (onlinePlayer != null) {
                this.playerUUID = onlinePlayer.getUniqueId();
            } else {
                // If player is offline, try to get UUID from offline player
                org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(player);
                if (offlinePlayer.hasPlayedBefore()) {
                    this.playerUUID = offlinePlayer.getUniqueId();
                }
            }
        } catch (Exception e) {
            // If UUID lookup fails, we'll have to work without it
            this.playerUUID = null;
        }
        
        // Try to parse type from action
        try {
            this.type = PunishmentType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = PunishmentType.WARN;
        }
        
        // Set expiration based on duration
        if (duration > 0) {
            this.expires = timestamp.plusMinutes(duration);
        }
    }

    // Constructor for int duration (backward compatibility)
    public Punishment(String player, String punisher, String reason, String action, int duration) {
        this(player, punisher, reason, action, (long) duration);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public UUID getModeratorUUID() { return moderatorUUID; }
    public void setModeratorUUID(UUID moderatorUUID) { this.moderatorUUID = moderatorUUID; }

    public String getModeratorName() { return moderatorName; }
    public void setModeratorName(String moderatorName) { this.moderatorName = moderatorName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public PunishmentType getType() { return type; }
    public void setType(PunishmentType type) { this.type = type; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public LocalDateTime getExpires() { return expires; }
    public void setExpires(LocalDateTime expires) { this.expires = expires; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    // Compatibility methods
    public String getAction() { return action != null ? action : (type != null ? type.name() : "UNKNOWN"); }
    public void setAction(String action) { this.action = action; }

    public String getPunisher() { return punisher != null ? punisher : moderatorName; }
    public void setPunisher(String punisher) { this.punisher = punisher; }

    public String getPlayer() { return player != null ? player : playerName; }
    public void setPlayer(String player) { this.player = player; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public boolean isPermanent() {
        return expires == null;
    }

    public boolean isExpired() {
        return expires != null && LocalDateTime.now().isAfter(expires);
    }

    public long getDurationMinutes() {
        if (expires == null) return -1;
        return java.time.Duration.between(timestamp, expires).toMinutes();
    }
}
