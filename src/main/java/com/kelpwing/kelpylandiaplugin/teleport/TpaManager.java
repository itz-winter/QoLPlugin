package com.kelpwing.kelpylandiaplugin.teleport;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TPA requests, cooldowns, and post-teleport invulnerability.
 */
public class TpaManager {

    private final KelpylandiaPlugin plugin;

    // Pending requests: targetUUID -> list of requests sent TO that target
    private final Map<UUID, List<TpaRequest>> pendingRequests = new ConcurrentHashMap<>();

    // Cooldowns: playerUUID -> cooldown expiry timestamp
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // Invulnerability: playerUUID -> invulnerability expiry timestamp
    private final Map<UUID, Long> invulnerable = new ConcurrentHashMap<>();

    // Cleanup task
    private BukkitTask cleanupTask;

    public TpaManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        // Clean up expired requests every 5 seconds
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            // Clean expired requests
            pendingRequests.values().forEach(list -> list.removeIf(TpaRequest::isExpired));
            pendingRequests.entrySet().removeIf(e -> e.getValue().isEmpty());

            // Clean expired cooldowns
            cooldowns.entrySet().removeIf(e -> now >= e.getValue());

            // Clean expired invulnerability
            invulnerable.entrySet().removeIf(e -> now >= e.getValue());
        }, 100L, 100L); // Every 5 seconds
    }

    // ===================== Requests =====================

    /**
     * Send a TPA request from requester to target.
     * Returns the created request, or null if one already exists.
     */
    public TpaRequest sendRequest(Player requester, Player target, TpaRequest.Type type) {
        UUID targetUUID = target.getUniqueId();
        UUID requesterUUID = requester.getUniqueId();

        List<TpaRequest> targetRequests = pendingRequests.computeIfAbsent(targetUUID, k -> new ArrayList<>());

        // Check if there's already a pending request from this requester
        for (TpaRequest req : targetRequests) {
            if (req.getRequesterUUID().equals(requesterUUID) && !req.isExpired()) {
                return null; // Already has a pending request
            }
        }

        long timeout = plugin.getConfig().getLong("teleport.request-timeout", 120);
        TpaRequest request = new TpaRequest(requesterUUID, targetUUID, type, timeout);
        targetRequests.add(request);

        // Schedule expiry notification
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (request.isCompleted()) return; // Already accepted/denied/cancelled
            if (!request.isExpired()) return; // Not yet expired
            Player req = Bukkit.getPlayer(requesterUUID);
            Player tgt = Bukkit.getPlayer(targetUUID);
            if (req != null) {
                req.sendMessage("§cYour teleport request to §e" + (tgt != null ? tgt.getName() : "that player") + " §chas expired.");
            }
        }, timeout * 20L);

        return request;
    }

    /**
     * Get the most recent pending request sent TO a target player.
     */
    public TpaRequest getLatestRequest(UUID targetUUID) {
        List<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests == null || requests.isEmpty()) return null;

        // Remove expired and get latest
        requests.removeIf(TpaRequest::isExpired);
        if (requests.isEmpty()) return null;

        return requests.get(requests.size() - 1);
    }

    /**
     * Check if the target has any requests that existed but are now expired.
     * This is used by /tpaccept and /tpdeny to show an [expired] message
     * instead of "no pending requests" when the request timed out.
     */
    public boolean hasExpiredRequests(UUID targetUUID) {
        List<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests == null || requests.isEmpty()) return false;
        return requests.stream().anyMatch(TpaRequest::isExpired);
    }

    /**
     * Check if the target has an expired request from a specific requester.
     */
    public boolean hasExpiredRequestFrom(UUID targetUUID, UUID requesterUUID) {
        List<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests == null) return false;
        return requests.stream()
                .anyMatch(r -> r.getRequesterUUID().equals(requesterUUID) && r.isExpired());
    }

    /**
     * Remove all expired requests for a target player.
     */
    public void clearExpiredRequests(UUID targetUUID) {
        List<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests != null) {
            requests.removeIf(TpaRequest::isExpired);
            if (requests.isEmpty()) {
                pendingRequests.remove(targetUUID);
            }
        }
    }

    /**
     * Get a specific request from a requester to a target.
     */
    public TpaRequest getRequest(UUID targetUUID, UUID requesterUUID) {
        List<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests == null) return null;

        for (TpaRequest req : requests) {
            if (req.getRequesterUUID().equals(requesterUUID) && !req.isExpired()) {
                return req;
            }
        }
        return null;
    }

    /**
     * Get all pending requests sent TO a target.
     */
    public List<TpaRequest> getRequestsFor(UUID targetUUID) {
        List<TpaRequest> requests = pendingRequests.get(targetUUID);
        if (requests == null) return Collections.emptyList();
        requests.removeIf(TpaRequest::isExpired);
        return new ArrayList<>(requests);
    }

    /**
     * Get the outgoing request FROM a requester (most recent).
     */
    public TpaRequest getOutgoingRequest(UUID requesterUUID) {
        for (List<TpaRequest> requests : pendingRequests.values()) {
            for (int i = requests.size() - 1; i >= 0; i--) {
                TpaRequest req = requests.get(i);
                if (req.getRequesterUUID().equals(requesterUUID) && !req.isExpired()) {
                    return req;
                }
            }
        }
        return null;
    }

    /**
     * Remove a specific request.
     */
    public void removeRequest(TpaRequest request) {
        List<TpaRequest> requests = pendingRequests.get(request.getTargetUUID());
        if (requests != null) {
            requests.remove(request);
            if (requests.isEmpty()) {
                pendingRequests.remove(request.getTargetUUID());
            }
        }
    }

    /**
     * Remove all requests FROM a requester.
     */
    public void removeAllFrom(UUID requesterUUID) {
        for (List<TpaRequest> requests : pendingRequests.values()) {
            requests.removeIf(r -> r.getRequesterUUID().equals(requesterUUID));
        }
        pendingRequests.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Remove all requests TO a target.
     */
    public void removeAllTo(UUID targetUUID) {
        pendingRequests.remove(targetUUID);
    }

    // ===================== Cooldowns =====================

    public boolean isOnCooldown(Player player) {
        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public long getCooldownRemaining(Player player) {
        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void applyCooldown(Player player) {
        // Bypass for players with bypass permission
        if (player.hasPermission("qol.teleport.bypass.cooldown")) return;

        long cooldownMs = plugin.getConfig().getLong("teleport.cooldown", 2) * 1000L;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMs);
    }

    // ===================== Invulnerability =====================

    public boolean isInvulnerable(Player player) {
        Long expiry = invulnerable.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            invulnerable.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void applyInvulnerability(Player player) {
        long invulnMs = plugin.getConfig().getLong("teleport.invulnerability", 4) * 1000L;
        if (invulnMs <= 0) return;
        invulnerable.put(player.getUniqueId(), System.currentTimeMillis() + invulnMs);
    }

    // ===================== Cleanup =====================

    public void cleanup() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        pendingRequests.clear();
        cooldowns.clear();
        invulnerable.clear();
    }

    public void cleanupPlayer(UUID uuid) {
        removeAllFrom(uuid);
        removeAllTo(uuid);
        cooldowns.remove(uuid);
        invulnerable.remove(uuid);
    }
}
