package com.kelpwing.kelpylandiaplugin.economy;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Experimental dynamic pricing engine.
 * <p>
 * When enabled, sell prices fluctuate based on supply and demand:
 * <ul>
 *   <li>Every sale pushes the multiplier <b>up</b> (more supply → price drops).</li>
 *   <li>Over time the multiplier <b>decays</b> back toward 1.0.</li>
 *   <li>Multipliers are clamped between {@code min-multiplier} and {@code max-multiplier}.</li>
 *   <li>Optional <b>per-user</b> mode tracks sales individually per player.</li>
 * </ul>
 * Price history snapshots are persisted to {@code dynamic-pricing-data.yml}.
 */
public class DynamicPricingEngine {

    private final KelpylandiaPlugin plugin;

    // ── Configuration ────────────────────────────────────────────
    private boolean enabled;
    private boolean perUser;
    private int timeWindowMinutes;       // rolling window for sales tracking
    private double maxMultiplier;
    private double minMultiplier;
    private double multiplierIncrement;  // per sale
    private double multiplierDecrement;  // alias kept for reference; decay uses decay-exponent
    private int decayIntervalMinutes;
    private double decayExponent;
    private double increaseExponent;

    // ── Messages / status labels ─────────────────────────────────
    private List<String> statusLabels; // HIGH→LOW: "ALL-TIME HIGHEST" .. "ALL-TIME LOWEST"

    // ── Runtime state ────────────────────────────────────────────
    /**
     * Current price multiplier for each material.
     * Global mode: one multiplier shared by all players.
     * Per-user mode: key is "MATERIAL:uuid", value is the multiplier for that user+material.
     */
    private final Map<String, Double> multipliers = new ConcurrentHashMap<>();

    /**
     * Rolling sales count per material (or material:uuid) inside the current time window.
     * Each entry stores a list of sale timestamps.
     */
    private final Map<String, List<Long>> salesLog = new ConcurrentHashMap<>();

    /**
     * Price history: ordered list of snapshots (timestamp, effective price) per material.
     * Used by /pricehistory.
     */
    private final Map<Material, List<PriceSnapshot>> priceHistory = new ConcurrentHashMap<>();

    // ── Persistence ──────────────────────────────────────────────
    private File dataFile;
    private FileConfiguration dataConfig;

    // ── Scheduled tasks ──────────────────────────────────────────
    private BukkitTask decayTask;
    private BukkitTask snapshotTask;

    // ════════════════════════════════════════════════════════════════
    //  Inner types
    // ════════════════════════════════════════════════════════════════

    /** A single price snapshot stored in history. */
    public static class PriceSnapshot {
        public final long timestamp;
        public final BigDecimal price;

        public PriceSnapshot(long timestamp, BigDecimal price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════

    public DynamicPricingEngine(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Load config values from the economy.yml dynamic-pricing section. */
    public void loadConfig(FileConfiguration economyConfig) {
        enabled = economyConfig.getBoolean("dynamic-pricing.enabled", false);
        perUser = economyConfig.getBoolean("dynamic-pricing.per-user", false);
        timeWindowMinutes = economyConfig.getInt("dynamic-pricing.time-window", 10);
        maxMultiplier = economyConfig.getDouble("dynamic-pricing.max-multiplier", 2.0);
        minMultiplier = economyConfig.getDouble("dynamic-pricing.min-multiplier", 0.5);
        multiplierIncrement = economyConfig.getDouble("dynamic-pricing.multiplier-increment", 0.015);
        multiplierDecrement = economyConfig.getDouble("dynamic-pricing.multiplier-decrement", 0.012);
        decayIntervalMinutes = economyConfig.getInt("dynamic-pricing.multiplier-decay-interval", 10);
        decayExponent = economyConfig.getDouble("dynamic-pricing.decay-exponent", 1.0);
        increaseExponent = economyConfig.getDouble("dynamic-pricing.increase-exponent", 1.5);

        statusLabels = economyConfig.getStringList("dynamic-pricing.messages.status");
        if (statusLabels == null || statusLabels.isEmpty()) {
            statusLabels = Arrays.asList(
                    "ALL-TIME HIGHEST", "PEAK HIGH", "RISING", "MINIMAL INCREASE",
                    "STABLE",
                    "MINIMAL DECREASE", "FALLING", "PEAK LOW", "ALL-TIME LOWEST");
        }

        loadData();

        if (enabled) {
            startTasks();
            plugin.getLogger().info("[Economy] Dynamic pricing engine ENABLED"
                    + (perUser ? " (per-user mode)" : " (global mode)") + ".");
        }
    }

    /** Shut down tasks and persist state. */
    public void shutdown() {
        if (decayTask != null) { decayTask.cancel(); decayTask = null; }
        if (snapshotTask != null) { snapshotTask.cancel(); snapshotTask = null; }
        saveData();
    }

    /** Full reload: cancel tasks, re-read config, restart. */
    public void reload(FileConfiguration economyConfig) {
        shutdown();
        multipliers.clear();
        salesLog.clear();
        priceHistory.clear();
        loadConfig(economyConfig);
    }

    // ════════════════════════════════════════════════════════════════
    //  Core API
    // ════════════════════════════════════════════════════════════════

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the current price multiplier for a material (global mode) or per-user.
     * Returns 1.0 if dynamic pricing is disabled.
     */
    public double getMultiplier(Material material, UUID playerUuid) {
        if (!enabled) return 1.0;
        String key = buildKey(material, playerUuid);
        return multipliers.getOrDefault(key, 1.0);
    }

    /**
     * Apply the dynamic multiplier to a base price.
     */
    public BigDecimal applyMultiplier(BigDecimal basePrice, Material material, UUID playerUuid) {
        if (!enabled) return basePrice;
        double mult = getMultiplier(material, playerUuid);
        return basePrice.multiply(BigDecimal.valueOf(mult)).setScale(
                plugin.getEconomyManager().getDecimals(), RoundingMode.HALF_UP);
    }

    /**
     * Record a sale of {@code amount} units of {@code material} by {@code playerUuid}.
     * This pushes the multiplier DOWN (more supply → lower price).
     */
    public void recordSale(Material material, int amount, UUID playerUuid) {
        if (!enabled) return;
        String key = buildKey(material, playerUuid);
        long now = System.currentTimeMillis();

        // Log each unit as a sale event (capped at stack size to avoid huge lists)
        int logged = Math.min(amount, 64);
        salesLog.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
        List<Long> log = salesLog.get(key);
        for (int i = 0; i < logged; i++) {
            log.add(now);
        }

        // Adjust multiplier downward (more sales → lower price)
        double current = multipliers.getOrDefault(key, 1.0);
        double change = multiplierIncrement * Math.pow(logged, 1.0 / increaseExponent);
        double newMult = Math.max(minMultiplier, current - change);
        multipliers.put(key, newMult);

        // Record snapshot for history (global key only)
        recordSnapshot(material);
    }

    /**
     * Get the price history for a material (most recent first).
     */
    public List<PriceSnapshot> getHistory(Material material) {
        return priceHistory.getOrDefault(material, Collections.emptyList());
    }

    /**
     * Determine a human-readable status label for the current multiplier.
     */
    public String getStatus(Material material, UUID playerUuid) {
        if (!enabled || statusLabels.isEmpty()) return "STABLE";
        double mult = getMultiplier(material, playerUuid);

        // Map multiplier range [minMultiplier, maxMultiplier] → status index
        double range = maxMultiplier - minMultiplier;
        if (range <= 0) return statusLabels.get(statusLabels.size() / 2);

        // Invert: high multiplier = high price = first label, low = last
        double normalised = (mult - minMultiplier) / range; // 0..1 where 1 = max (expensive)
        int idx = (int) ((1.0 - normalised) * (statusLabels.size() - 1));
        idx = Math.max(0, Math.min(statusLabels.size() - 1, idx));
        return statusLabels.get(idx);
    }

    /**
     * Percentage change from base (multiplier = 1.0).
     * Positive = price above base, negative = below base.
     */
    public double getChangePercent(Material material, UUID playerUuid) {
        double mult = getMultiplier(material, playerUuid);
        return (mult - 1.0) * 100.0;
    }

    // ════════════════════════════════════════════════════════════════
    //  Decay logic
    // ════════════════════════════════════════════════════════════════

    private void startTasks() {
        long decayTicks = decayIntervalMinutes * 60L * 20L; // minutes → ticks
        if (decayTicks < 20) decayTicks = 20;

        decayTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::decayMultipliers, decayTicks, decayTicks);

        // Take a price snapshot every time-window
        long snapshotTicks = timeWindowMinutes * 60L * 20L;
        if (snapshotTicks < 20) snapshotTicks = 20;
        snapshotTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::takeSnapshots, snapshotTicks, snapshotTicks);
    }

    /** Decay all multipliers back toward 1.0. */
    private void decayMultipliers() {
        long now = System.currentTimeMillis();
        long window = timeWindowMinutes * 60_000L;

        for (Map.Entry<String, Double> entry : multipliers.entrySet()) {
            String key = entry.getKey();
            double current = entry.getValue();

            // Purge old sales outside the window
            List<Long> log = salesLog.get(key);
            if (log != null) {
                log.removeIf(ts -> (now - ts) > window);
            }
            int recentSales = (log != null) ? log.size() : 0;

            // If no recent sales, decay toward 1.0
            if (recentSales == 0) {
                double decayAmount = multiplierDecrement * Math.pow(1.0, decayExponent);
                if (current < 1.0) {
                    current = Math.min(1.0, current + decayAmount);
                } else if (current > 1.0) {
                    current = Math.max(1.0, current - decayAmount);
                }
                entry.setValue(current);

                // If fully decayed, clean up
                if (Math.abs(current - 1.0) < 0.001) {
                    multipliers.remove(key);
                    salesLog.remove(key);
                }
            }
        }
    }

    /** Take a history snapshot for all materials that have active multipliers. */
    private void takeSnapshots() {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) return;

        Set<Material> seen = new HashSet<>();
        for (String key : multipliers.keySet()) {
            Material mat = materialFromKey(key);
            if (mat != null && seen.add(mat)) {
                recordSnapshot(mat);
            }
        }
        // Also persist periodically
        saveDataAsync();
    }

    private void recordSnapshot(Material material) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) return;
        EconomyManager.PriceResult base = eco.getBasePrice(material);
        if (!base.sellable) return;

        double mult = multipliers.getOrDefault(material.name(), 1.0);
        BigDecimal effective = base.price.multiply(BigDecimal.valueOf(mult))
                .setScale(eco.getDecimals(), RoundingMode.HALF_UP);

        List<PriceSnapshot> history = priceHistory.computeIfAbsent(material,
                k -> Collections.synchronizedList(new ArrayList<>()));
        history.add(new PriceSnapshot(System.currentTimeMillis(), effective));

        // Keep history bounded (max ~1000 entries per material)
        while (history.size() > 1000) {
            history.remove(0);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Persistence (dynamic-pricing-data.yml)
    // ════════════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "dynamic-pricing-data.yml");
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load multipliers
        ConfigurationSection multSec = dataConfig.getConfigurationSection("multipliers");
        if (multSec != null) {
            for (String key : multSec.getKeys(false)) {
                multipliers.put(key, multSec.getDouble(key, 1.0));
            }
        }

        // Load price history
        ConfigurationSection histSec = dataConfig.getConfigurationSection("history");
        if (histSec != null) {
            for (String matName : histSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName);
                    List<Map<?, ?>> entries = histSec.getMapList(matName);
                    List<PriceSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
                    for (Map<?, ?> entry : entries) {
                        long ts = ((Number) entry.get("t")).longValue();
                        double price = ((Number) entry.get("p")).doubleValue();
                        snapshots.add(new PriceSnapshot(ts, BigDecimal.valueOf(price)));
                    }
                    priceHistory.put(mat, snapshots);
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveData() {
        if (dataConfig == null) dataConfig = new YamlConfiguration();

        // Save multipliers
        dataConfig.set("multipliers", null);
        for (Map.Entry<String, Double> entry : multipliers.entrySet()) {
            dataConfig.set("multipliers." + entry.getKey(), entry.getValue());
        }

        // Save price history
        dataConfig.set("history", null);
        for (Map.Entry<Material, List<PriceSnapshot>> entry : priceHistory.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (PriceSnapshot snap : entry.getValue()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("t", snap.timestamp);
                map.put("p", snap.price.doubleValue());
                list.add(map);
            }
            dataConfig.set("history." + entry.getKey().name(), list);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save dynamic-pricing-data.yml!", e);
        }
    }

    private void saveDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveData);
    }

    // ════════════════════════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════════════════════════

    private String buildKey(Material material, UUID playerUuid) {
        if (perUser && playerUuid != null) {
            return material.name() + ":" + playerUuid;
        }
        return material.name();
    }

    private Material materialFromKey(String key) {
        String matName = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ── Getters for commands ─────────────────────────────────────

    public boolean isPerUser() { return perUser; }
    public double getMaxMultiplier() { return maxMultiplier; }
    public double getMinMultiplier() { return minMultiplier; }
    public int getTimeWindowMinutes() { return timeWindowMinutes; }
    public List<String> getStatusLabels() { return Collections.unmodifiableList(statusLabels); }
}
