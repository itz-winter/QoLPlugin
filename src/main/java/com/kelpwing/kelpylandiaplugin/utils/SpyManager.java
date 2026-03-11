package com.kelpwing.kelpylandiaplugin.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SocialSpy and CommandSpy toggles.
 * SocialSpy: see all private messages (/w) and non-global channel chat.
 * CommandSpy: see all commands executed by other players.
 */
public class SpyManager {

    private final Set<UUID> socialSpies = ConcurrentHashMap.newKeySet();
    private final Set<UUID> commandSpies = ConcurrentHashMap.newKeySet();

    // ─── SocialSpy ─────────────────────────────────────────────────

    public boolean toggleSocialSpy(UUID uuid) {
        if (socialSpies.contains(uuid)) {
            socialSpies.remove(uuid);
            return false;
        }
        socialSpies.add(uuid);
        return true;
    }

    public boolean isSocialSpy(UUID uuid) {
        return socialSpies.contains(uuid);
    }

    /** Explicitly set social spy state (used by state persistence). */
    public void setSocialSpy(UUID uuid, boolean enabled) {
        if (enabled) socialSpies.add(uuid);
        else socialSpies.remove(uuid);
    }

    public Set<UUID> getSocialSpies() {
        return Collections.unmodifiableSet(socialSpies);
    }

    // ─── CommandSpy ────────────────────────────────────────────────

    public boolean toggleCommandSpy(UUID uuid) {
        if (commandSpies.contains(uuid)) {
            commandSpies.remove(uuid);
            return false;
        }
        commandSpies.add(uuid);
        return true;
    }

    public boolean isCommandSpy(UUID uuid) {
        return commandSpies.contains(uuid);
    }

    /** Explicitly set command spy state (used by state persistence). */
    public void setCommandSpy(UUID uuid, boolean enabled) {
        if (enabled) commandSpies.add(uuid);
        else commandSpies.remove(uuid);
    }

    public Set<UUID> getCommandSpies() {
        return Collections.unmodifiableSet(commandSpies);
    }

    // ─── Cleanup ───────────────────────────────────────────────────

    public void removePlayer(UUID uuid) {
        socialSpies.remove(uuid);
        commandSpies.remove(uuid);
    }
}
