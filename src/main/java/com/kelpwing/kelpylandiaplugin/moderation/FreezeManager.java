package com.kelpwing.kelpylandiaplugin.moderation;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages frozen players.
 * Frozen players cannot move, take damage, interact, or do anything until unfrozen.
 * State persists through relogs (stored in memory, but checked on join).
 * No actions are logged to Discord.
 */
public class FreezeManager {

    /** Set of frozen player UUIDs. Persists through relog. */
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    public boolean freeze(UUID uuid) {
        return frozenPlayers.add(uuid);
    }

    public boolean unfreeze(UUID uuid) {
        return frozenPlayers.remove(uuid);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public Set<UUID> getFrozenPlayers() {
        return Collections.unmodifiableSet(frozenPlayers);
    }
}
