package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.ChatUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages automatic server broadcasts loaded from broadcasts.yml.
 * 
 * Messages are broadcasted to all online players at random intervals
 * within a configurable min–max range. Supports color codes and
 * PlaceholderAPI placeholders.
 */
public class BroadcastManager {

    private final KelpylandiaPlugin plugin;

    private final List<String> messages = new ArrayList<>();
    private int minInterval; // seconds
    private int maxInterval; // seconds
    private String prefix;
    private boolean randomOrder;

    private BukkitTask broadcastTask;
    private int currentIndex = 0;

    public BroadcastManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        startBroadcastCycle();
    }

    // ===================== Config Loading =====================

    /**
     * Load (or reload) broadcast settings from broadcasts.yml.
     */
    public void loadConfig() {
        messages.clear();
        currentIndex = 0;

        // Save default if it doesn't exist
        File file = new File(plugin.getDataFolder(), "broadcasts.yml");
        if (!file.exists()) {
            plugin.saveResource("broadcasts.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        minInterval = config.getInt("min-interval", 60);
        maxInterval = config.getInt("max-interval", 120);
        prefix = config.getString("prefix", "&6&l[Broadcast] &r");
        randomOrder = config.getBoolean("random-order", true);

        // Sanity check intervals
        if (minInterval < 1) minInterval = 1;
        if (maxInterval < minInterval) maxInterval = minInterval;

        List<String> loaded = config.getStringList("messages");
        if (!loaded.isEmpty()) {
            messages.addAll(loaded);
        }

        plugin.getLogger().info("Loaded " + messages.size() + " auto-broadcast messages (interval: " + minInterval + "-" + maxInterval + "s).");
    }

    // ===================== Broadcast Scheduling =====================

    /**
     * Start the broadcast cycle. Schedules the next broadcast after a random delay.
     */
    public void startBroadcastCycle() {
        if (messages.isEmpty()) {
            plugin.getLogger().info("No auto-broadcast messages configured, skipping.");
            return;
        }
        scheduleNext();
    }

    /**
     * Schedule the next broadcast after a random delay between min and max interval.
     */
    private void scheduleNext() {
        int delaySec = ThreadLocalRandom.current().nextInt(minInterval, maxInterval + 1);
        long delayTicks = delaySec * 20L; // 20 ticks per second

        broadcastTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNextMessage();
            scheduleNext(); // chain the next one
        }, delayTicks);
    }

    /**
     * Broadcast the next message to all online players.
     */
    private void broadcastNextMessage() {
        if (messages.isEmpty()) return;

        // No one online? Skip.
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        String template;
        if (randomOrder) {
            template = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        } else {
            template = messages.get(currentIndex);
            currentIndex = (currentIndex + 1) % messages.size();
        }

        String formatted = formatMessage(prefix + template);

        // If the message contains [/command] patterns, send as clickable components
        if (ChatUtils.containsCommand(formatted)) {
            BaseComponent[] components = ChatUtils.parseClickableCommands(formatted);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(components);
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(formatted);
            }
        }
    }

    /**
     * Format a broadcast message: apply color codes and PlaceholderAPI (player-independent).
     */
    private String formatMessage(String message) {
        // Translate color codes
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    // ===================== Lifecycle =====================

    /**
     * Reload the broadcast config and restart the cycle.
     */
    public void reload() {
        stop();
        loadConfig();
        startBroadcastCycle();
    }

    /**
     * Cancel the scheduled broadcast task.
     */
    public void stop() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
    }

    // ===================== Getters =====================

    public int getMinInterval() {
        return minInterval;
    }

    public int getMaxInterval() {
        return maxInterval;
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
