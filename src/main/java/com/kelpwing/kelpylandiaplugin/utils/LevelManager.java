package com.kelpwing.kelpylandiaplugin.utils;

import org.bukkit.entity.Player;

/**
 * Manages staff level hierarchy (1-10) for vanish, socialspy, and commandspy.
 * Higher-level staff can see lower-level staff; lower-level staff cannot see higher.
 * 
 * Permission nodes:
 *   kelpylandia.vanish.level.<1-10>
 *   kelpylandia.socialspy.level.<1-10>
 *   kelpylandia.commandspy.level.<1-10>
 * 
 * A player's effective level is the highest matching node.
 * Default level (no node) is 1.
 */
public class LevelManager {

    /**
     * Get the highest level a player has for a given feature.
     * @param player the player
     * @param feature "vanish", "socialspy", or "commandspy"
     * @return level 1-10, defaults to 1 if no level nodes are set
     */
    public static int getLevel(Player player, String feature) {
        int highest = 1;
        for (int i = 10; i >= 2; i--) {
            if (player.hasPermission("qol." + feature + ".level." + i)) {
                highest = i;
                break;
            }
        }
        return highest;
    }

    /**
     * Check if observer can see/spy on the target based on their levels.
     * An observer can see a target only if the observer's level >= the target's level.
     * @param observer the staff member observing
     * @param target the player being observed
     * @param feature "vanish", "socialspy", or "commandspy"
     * @return true if observer can see/spy on target
     */
    public static boolean canObserve(Player observer, Player target, String feature) {
        int observerLevel = getLevel(observer, feature);
        int targetLevel = getLevel(target, feature);
        return observerLevel >= targetLevel;
    }
}
