package com.kelpwing.kelpylandiaplugin.moderation.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Warning {
    private String id;
    private UUID playerUUID;
    private String playerName;
    private UUID moderatorUUID;
    private String moderatorName;
    private String reason;
    private LocalDateTime timestamp;
    private boolean active;
    private String server;
    private long expiration;

    // Default constructor
    public Warning() {
        this.active = true;
        this.server = "Kelpylandia";
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public Warning(UUID playerUUID, String playerName, UUID moderatorUUID, String moderatorName, String reason) {
        this();
        this.id = UUID.randomUUID().toString();
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.moderatorUUID = moderatorUUID;
        this.moderatorName = moderatorName;
        this.reason = reason;
        this.expiration = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L); // 7 days default
    }

    // All getters and setters
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

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public long getExpiration() { return expiration; }
    public void setExpiration(long expiration) { this.expiration = expiration; }

    // Compatibility methods
    public String getPunisher() { return moderatorName; }
    public void setPunisher(String punisher) { this.moderatorName = punisher; }

    public String getFormattedTimestamp() {
        return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public boolean isExpired() {
        return expiration > 0 && System.currentTimeMillis() > expiration;
    }
}
