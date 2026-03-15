package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VanishManager {
    
    private final KelpylandiaPlugin plugin;
    private final Map<UUID, Boolean> canPickupItems;
    private final Map<UUID, UUID> vanishTargets;
    private final Map<UUID, Scoreboard> vanishScoreboards;
    /** Saves the player's original scoreboard before vanish replaces it. */
    private final Map<UUID, Scoreboard> savedScoreboards;
    /** Maps a vanished player's UUID → their silent container copy + real inventory for sync-back. */
    private final Map<UUID, Inventory[]> silentContainers;
    
    public VanishManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.canPickupItems = new HashMap<>();
        this.vanishTargets = new HashMap<>();
        this.vanishScoreboards = new HashMap<>();
        this.savedScoreboards = new HashMap<>();
        this.silentContainers = new HashMap<>();
    }
    
    public void setCanPickupItems(Player player, boolean canPickup) {
        canPickupItems.put(player.getUniqueId(), canPickup);
    }
    
    public boolean canPickupItems(Player player) {
        return canPickupItems.getOrDefault(player.getUniqueId(), true);
    }
    
    public void setVanishTarget(Player vanishedPlayer, Player target) {
        vanishTargets.put(vanishedPlayer.getUniqueId(), target.getUniqueId());
        updateVanishScoreboard(vanishedPlayer);
    }
    
    public Player getVanishTarget(Player vanishedPlayer) {
        UUID targetUUID = vanishTargets.get(vanishedPlayer.getUniqueId());
        return targetUUID != null ? Bukkit.getPlayer(targetUUID) : null;
    }
    
    public void clearVanishTarget(Player vanishedPlayer) {
        vanishTargets.remove(vanishedPlayer.getUniqueId());
        updateVanishScoreboard(vanishedPlayer);
    }
    
    public void showVanishScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        
        // Save the player's current scoreboard so we can restore it later
        savedScoreboards.put(player.getUniqueId(), player.getScoreboard());
        
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("vanish", "dummy", 
            ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + "VANISH MODE");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        vanishScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        
        updateVanishScoreboard(player);
    }
    
    public void hideVanishScoreboard(Player player) {
        vanishScoreboards.remove(player.getUniqueId());
        // Restore the player's original scoreboard (not just the main one)
        Scoreboard saved = savedScoreboards.remove(player.getUniqueId());
        if (saved != null) {
            player.setScoreboard(saved);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
    
    private void updateVanishScoreboard(Player vanishedPlayer) {
        Scoreboard scoreboard = vanishScoreboards.get(vanishedPlayer.getUniqueId());
        if (scoreboard == null) return;
        
        Objective objective = scoreboard.getObjective("vanish");
        if (objective == null) return;
        
        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        
        int score = 15;
        
        // Basic vanish info
        setScore(objective, ChatColor.YELLOW + "Status: " + ChatColor.GREEN + "VANISHED", score--);
        setScore(objective, "", score--); // Blank line
        
        UUID targetUUID = vanishTargets.get(vanishedPlayer.getUniqueId());
        Player target = targetUUID != null ? Bukkit.getPlayer(targetUUID) : null;
        if (target != null) {
            setScore(objective, ChatColor.GOLD + "Target Info:", score--);
            setScore(objective, ChatColor.WHITE + "Name: " + ChatColor.AQUA + target.getName(), score--);
            setScore(objective, ChatColor.WHITE + "Display: " + target.getDisplayName(), score--);
            double maxHealth = 20.0;
            org.bukkit.attribute.Attribute maxHealthAttr = VersionHelper.getMaxHealthAttribute();
            if (maxHealthAttr != null && target.getAttribute(maxHealthAttr) != null) {
                maxHealth = target.getAttribute(maxHealthAttr).getValue();
            }
            setScore(objective, ChatColor.WHITE + "Health: " + ChatColor.RED + 
                String.format("%.1f/%.1f", target.getHealth(), maxHealth), score--);
            setScore(objective, ChatColor.WHITE + "Food: " + ChatColor.GREEN + target.getFoodLevel() + "/20", score--);
            setScore(objective, ChatColor.WHITE + "Level: " + ChatColor.LIGHT_PURPLE + target.getLevel(), score--);
            setScore(objective, ChatColor.WHITE + "GameMode: " + ChatColor.YELLOW + target.getGameMode().name(), score--);
            
            // Ping - using Player.getPing() (available since 1.17+)
            try {
                int ping = target.getPing();
                setScore(objective, ChatColor.WHITE + "Ping: " + ChatColor.GOLD + ping + "ms", score--);
            } catch (Exception e) {
                setScore(objective, ChatColor.WHITE + "Ping: " + ChatColor.GRAY + "Unknown", score--);
            }
            
            // Join date using first played
            long firstPlayed = target.getFirstPlayed();
            if (firstPlayed > 0) {
                String joinDate = Instant.ofEpochMilli(firstPlayed)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                setScore(objective, ChatColor.WHITE + "First Join: " + ChatColor.DARK_AQUA + joinDate, score--);
            }
            
            // Playtime using StatisticAPI if available, otherwise PlaceholderAPI
            String playtime = PlaceholderAPI.setPlaceholders(target, "%statistic_time_played%");
            if (playtime != null && !playtime.equals("%statistic_time_played%")) {
                setScore(objective, ChatColor.WHITE + "Playtime: " + ChatColor.LIGHT_PURPLE + playtime, score--);
            } else {
                // Fallback calculation
                long playTimeMs = target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50; // 50ms per tick
                long hours = playTimeMs / (1000 * 60 * 60);
                long minutes = (playTimeMs % (1000 * 60 * 60)) / (1000 * 60);
                setScore(objective, ChatColor.WHITE + "Playtime: " + ChatColor.LIGHT_PURPLE + 
                    String.format("%dh %dm", hours, minutes), score--);
            }
            
            // Try to get alts using PlaceholderAPI if LuckPerms or similar is available
            String alts = PlaceholderAPI.setPlaceholders(target, "%luckperms_prefix%");
            if (alts != null && !alts.equals("%luckperms_prefix%")) {
                setScore(objective, ChatColor.WHITE + "Rank: " + ChatColor.translateAlternateColorCodes('&', alts), score--);
            }
            
            setScore(objective, "", score--); // Blank line
        } else {
            setScore(objective, ChatColor.GRAY + "No target selected", score--);
            setScore(objective, ChatColor.GRAY + "Click a player or", score--);
            setScore(objective, ChatColor.GRAY + "get close to them", score--);
            setScore(objective, "", score--); // Blank line
        }
        
        // Instructions
        setScore(objective, ChatColor.DARK_GRAY + "Right-click to target", score--);
        setScore(objective, ChatColor.DARK_GRAY + "Shift+Right-click to clear", score--);
    }
    
    private void setScore(Objective objective, String text, int score) {
        Score scoreEntry = objective.getScore(text);
        scoreEntry.setScore(score);
    }
    
    /**
     * Track a silent container opened by a vanished player.
     * When the player closes it, the contents are synced back to the real container.
     */
    public void trackSilentContainer(Player player, Inventory copy, Inventory real) {
        silentContainers.put(player.getUniqueId(), new Inventory[]{copy, real});
    }

    /**
     * Called when a player closes an inventory. If it's a tracked silent container,
     * sync the contents back to the real container and remove the tracking.
     * @return true if this was a tracked silent container close
     */
    public boolean handleSilentContainerClose(Player player, Inventory closedInv) {
        Inventory[] pair = silentContainers.remove(player.getUniqueId());
        if (pair == null) return false;
        Inventory copy = pair[0];
        Inventory real = pair[1];
        if (closedInv.equals(copy)) {
            // Sync contents back to the real container
            real.setContents(copy.getContents());
            return true;
        }
        return false;
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        canPickupItems.remove(uuid);
        vanishTargets.remove(uuid);
        vanishScoreboards.remove(uuid);
        savedScoreboards.remove(uuid);
        silentContainers.remove(uuid);
    }
}
