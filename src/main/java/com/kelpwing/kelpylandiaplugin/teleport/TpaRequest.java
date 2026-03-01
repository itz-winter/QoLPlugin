package com.kelpwing.kelpylandiaplugin.teleport;

import java.util.UUID;

/**
 * Represents a pending TPA request.
 */
public class TpaRequest {

    public enum Type {
        /** Requester wants to teleport TO the target. */
        TPA,
        /** Requester wants the target to teleport TO them. */
        TPA_HERE
    }

    private final UUID requesterUUID;
    private final UUID targetUUID;
    private final Type type;
    private final long createdAt;
    private final long expiresAt;

    public TpaRequest(UUID requesterUUID, UUID targetUUID, Type type, long timeoutSeconds) {
        this.requesterUUID = requesterUUID;
        this.targetUUID = targetUUID;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + (timeoutSeconds * 1000L);
    }

    public UUID getRequesterUUID() { return requesterUUID; }
    public UUID getTargetUUID() { return targetUUID; }
    public Type getType() { return type; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    /**
     * Seconds remaining until this request expires.
     */
    public int getSecondsRemaining() {
        long remaining = expiresAt - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }
}
