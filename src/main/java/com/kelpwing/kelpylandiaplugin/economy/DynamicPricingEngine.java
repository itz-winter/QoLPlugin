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

public class DynamicPricingEngine {

    private final KelpylandiaPlugin plugin;

    // Core Configuration
    private boolean enabled;
    private boolean perUser;
    private int timeWindowMinutes;
    private double maxMultiplier;
    private double minMultiplier;
    private double baseMultiplierIncrement;
    private double baseMultiplierDecrement;
    private int decayIntervalMinutes;
    private double decayExponent;
    private double increaseExponent;

    // Advanced Feature Toggles
    private boolean volumeDampeningEnabled;
    private double volumeDampeningScale;
    private double volumeDampeningMinFactor;

    private boolean emaSmoothingEnabled;
    private double emaAlpha;

    private boolean supplyCurveEnabled;
    private double supplyCurveStrength;
    private long supplyCurveBaseline;

    private boolean cooldownEnabled;
    private int cooldownSeconds;

    private boolean inflationEnabled;
    private double inflationRate;
    private double maxInflationCoefficient;
    private double minInflationCoefficient;

    private boolean velocityEnabled;
    private double velocityStabilityFactor;
    private int velocityWindowMinutes;

    private boolean spreadEnabled;
    private double spreadPercent;

    private List<String> statusLabels;

    // Runtime state
    private final Map<String, Double> multipliers = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> salesLog = new ConcurrentHashMap<>();
    private final Map<Material, List<PriceSnapshot>> priceHistory = new ConcurrentHashMap<>();
    private final Map<Material, Long> circulationCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldownTracker = new ConcurrentHashMap<>();
    private final Map<Material, List<Long>> velocityLog = new ConcurrentHashMap<>();
    private volatile double inflationCoefficient = 1.0;
    private final Map<Material, Double> emaState = new ConcurrentHashMap<>();

    // Persistence
    private File dataFile;
    private FileConfiguration dataConfig;

    // Scheduled tasks
    private BukkitTask decayTask;
    private BukkitTask snapshotTask;

    public static class PriceSnapshot {
        public final long timestamp;
        public final BigDecimal price;
        public PriceSnapshot(long timestamp, BigDecimal price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    public DynamicPricingEngine(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig(FileConfiguration economyConfig) {
        enabled = economyConfig.getBoolean("dynamic-pricing.enabled", false);
        perUser = economyConfig.getBoolean("dynamic-pricing.per-user", false);
        timeWindowMinutes = economyConfig.getInt("dynamic-pricing.time-window", 10);
        maxMultiplier = economyConfig.getDouble("dynamic-pricing.max-multiplier", 2.0);
        minMultiplier = economyConfig.getDouble("dynamic-pricing.min-multiplier", 0.5);
        baseMultiplierIncrement = economyConfig.getDouble("dynamic-pricing.multiplier-increment", 0.015);
        baseMultiplierDecrement = economyConfig.getDouble("dynamic-pricing.multiplier-decrement", 0.012);
        decayIntervalMinutes = economyConfig.getInt("dynamic-pricing.multiplier-decay-interval", 10);
        decayExponent = economyConfig.getDouble("dynamic-pricing.decay-exponent", 1.5);
        increaseExponent = economyConfig.getDouble("dynamic-pricing.increase-exponent", 1.5);

        volumeDampeningEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.volume-dampening.enabled", true);
        volumeDampeningScale = economyConfig.getDouble("dynamic-pricing.advanced.volume-dampening.scale", 0.6);
        volumeDampeningMinFactor = economyConfig.getDouble("dynamic-pricing.advanced.volume-dampening.min-factor", 0.1);

        emaSmoothingEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.ema-smoothing.enabled", true);
        emaAlpha = economyConfig.getDouble("dynamic-pricing.advanced.ema-smoothing.alpha", 0.15);

        supplyCurveEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.supply-curve.enabled", true);
        supplyCurveStrength = economyConfig.getDouble("dynamic-pricing.advanced.supply-curve.strength", 0.3);
        supplyCurveBaseline = economyConfig.getLong("dynamic-pricing.advanced.supply-curve.baseline", 1000L);

        cooldownEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.cooldown.enabled", true);
        cooldownSeconds = economyConfig.getInt("dynamic-pricing.advanced.cooldown.seconds", 5);

        inflationEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.inflation.enabled", false);
        inflationRate = economyConfig.getDouble("dynamic-pricing.advanced.inflation.rate-per-cycle", 0.001);
        maxInflationCoefficient = economyConfig.getDouble("dynamic-pricing.advanced.inflation.max-coefficient", 1.5);
        minInflationCoefficient = economyConfig.getDouble("dynamic-pricing.advanced.inflation.min-coefficient", 0.8);

        velocityEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.velocity.enabled", false);
        velocityStabilityFactor = economyConfig.getDouble("dynamic-pricing.advanced.velocity.stability-factor", 0.5);
        velocityWindowMinutes = economyConfig.getInt("dynamic-pricing.advanced.velocity.window-minutes", 30);

        spreadEnabled = economyConfig.getBoolean("dynamic-pricing.advanced.spread.enabled", true);
        spreadPercent = economyConfig.getDouble("dynamic-pricing.advanced.spread.percent", 15.0);

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
            StringBuilder features = new StringBuilder();
            if (volumeDampeningEnabled) features.append(" VolDamp");
            if (emaSmoothingEnabled) features.append(" EMA");
            if (supplyCurveEnabled) features.append(" SupplyCurve");
            if (cooldownEnabled) features.append(" Cooldown");
            if (inflationEnabled) features.append(" Inflation");
            if (velocityEnabled) features.append(" Velocity");
            if (spreadEnabled) features.append(" Spread");
            plugin.getLogger().info("[Economy] Dynamic pricing engine ENABLED"
                    + (perUser ? " (per-user mode)" : " (global mode)")
                    + " | Advanced:" + features);
        }
    }

    public void shutdown() {
        if (decayTask != null) { decayTask.cancel(); decayTask = null; }
        if (snapshotTask != null) { snapshotTask.cancel(); snapshotTask = null; }
        saveData();
    }

    public void reload(FileConfiguration economyConfig) {
        shutdown();
        multipliers.clear();
        salesLog.clear();
        priceHistory.clear();
        cooldownTracker.clear();
        velocityLog.clear();
        emaState.clear();
        loadConfig(economyConfig);
    }

    public boolean isEnabled() { return enabled; }

    public double getMultiplier(Material material, UUID playerUuid) {
        if (!enabled) return 1.0;
        String key = buildKey(material, playerUuid);
        return multipliers.getOrDefault(key, 1.0);
    }

    public double getEffectiveSellMultiplier(Material material, UUID playerUuid) {
        if (!enabled) return 1.0;
        double base = getMultiplier(material, playerUuid);
        return applyAdvancedModifiers(base, material, false);
    }

    public BigDecimal applyMultiplier(BigDecimal basePrice, Material material, UUID playerUuid) {
        if (!enabled) return basePrice;
        double mult = getEffectiveSellMultiplier(material, playerUuid);
        return basePrice.multiply(BigDecimal.valueOf(mult)).setScale(
                plugin.getEconomyManager().getDecimals(), RoundingMode.HALF_UP);
    }

    public void recordSale(Material material, int amount, UUID playerUuid) {
        if (!enabled) return;

        if (cooldownEnabled && playerUuid != null) {
            String cooldownKey = material.name() + ":" + playerUuid;
            Long lastTrade = cooldownTracker.get(cooldownKey);
            long now = System.currentTimeMillis();
            if (lastTrade != null && (now - lastTrade) < cooldownSeconds * 1000L) {
                circulationCounts.merge(material, (long) -amount, Long::sum);
                if (circulationCounts.getOrDefault(material, 0L) < 0) {
                    circulationCounts.put(material, 0L);
                }
                return;
            }
            cooldownTracker.put(cooldownKey, now);
        }

        String key = buildKey(material, playerUuid);
        long now = System.currentTimeMillis();

        salesLog.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
        List<Long> log = salesLog.get(key);
        for (int i = 0; i < amount; i++) {
            log.add(now);
        }

        double dampenedAmount = computeVolumeDampenedChange(amount);
        double change = baseMultiplierIncrement * dampenedAmount;
        change *= getVelocityDampeningFactor(material);

        double current = multipliers.getOrDefault(key, 1.0);
        double newMult = Math.max(minMultiplier, current - change);
        multipliers.put(key, newMult);

        circulationCounts.merge(material, (long) -amount, Long::sum);
        if (circulationCounts.getOrDefault(material, 0L) < 0) {
            circulationCounts.put(material, 0L);
        }

        recordVelocityEvent(material);
        recordSnapshot(material);
    }

    public void recordPurchase(Material material, int amount, UUID playerUuid) {
        if (!enabled) return;

        if (cooldownEnabled && playerUuid != null) {
            String cooldownKey = material.name() + ":" + playerUuid;
            Long lastTrade = cooldownTracker.get(cooldownKey);
            long now = System.currentTimeMillis();
            if (lastTrade != null && (now - lastTrade) < cooldownSeconds * 1000L) {
                circulationCounts.merge(material, (long) amount, Long::sum);
                return;
            }
            cooldownTracker.put(cooldownKey, now);
        }

        String key = buildKey(material, playerUuid);

        double dampenedAmount = computeVolumeDampenedChange(amount);
        double change = baseMultiplierIncrement * dampenedAmount;
        change *= getVelocityDampeningFactor(material);

        double current = multipliers.getOrDefault(key, 1.0);
        double newMult = Math.min(maxMultiplier, current + change);
        multipliers.put(key, newMult);

        circulationCounts.merge(material, (long) amount, Long::sum);

        recordVelocityEvent(material);
        recordSnapshot(material);
    }

    public BigDecimal applyBuyMultiplier(BigDecimal baseBuyPrice, Material material, UUID playerUuid) {
        if (!enabled) return baseBuyPrice;

        double sellMult = getEffectiveSellMultiplier(material, playerUuid);
        double buyMult;
        if (spreadEnabled) {
            buyMult = sellMult * (1.0 + spreadPercent / 100.0);
        } else {
            buyMult = 2.0 - sellMult;
        }
        buyMult = Math.max(minMultiplier, Math.min(maxMultiplier, buyMult));

        return baseBuyPrice.multiply(BigDecimal.valueOf(buyMult)).setScale(
                plugin.getEconomyManager().getDecimals(), RoundingMode.HALF_UP);
    }

    public List<PriceSnapshot> getHistory(Material material) {
        return priceHistory.getOrDefault(material, Collections.emptyList());
    }

    public List<PriceSnapshot> getSmoothedHistory(Material material) {
        List<PriceSnapshot> raw = priceHistory.getOrDefault(material, Collections.emptyList());
        if (!emaSmoothingEnabled || raw.isEmpty()) return raw;

        List<PriceSnapshot> smoothed = new ArrayList<>(raw.size());
        double ema = raw.get(0).price.doubleValue();

        for (PriceSnapshot snap : raw) {
            ema = emaAlpha * snap.price.doubleValue() + (1.0 - emaAlpha) * ema;
            smoothed.add(new PriceSnapshot(snap.timestamp,
                    BigDecimal.valueOf(ema).setScale(4, RoundingMode.HALF_UP)));
        }
        return smoothed;
    }

    public String getStatus(Material material, UUID playerUuid) {
        if (!enabled || statusLabels.isEmpty()) return "STABLE";
        double mult = getEffectiveSellMultiplier(material, playerUuid);

        double range = maxMultiplier - minMultiplier;
        if (range <= 0) return statusLabels.get(statusLabels.size() / 2);

        double normalised = (mult - minMultiplier) / range;
        int idx = (int) ((1.0 - normalised) * (statusLabels.size() - 1));
        idx = Math.max(0, Math.min(statusLabels.size() - 1, idx));
        return statusLabels.get(idx);
    }

    public double getChangePercent(Material material, UUID playerUuid) {
        double mult = getEffectiveSellMultiplier(material, playerUuid);
        return (mult - 1.0) * 100.0;
    }

    // Advanced economy modifiers

    private double computeVolumeDampenedChange(int amount) {
        if (!volumeDampeningEnabled || amount <= 0) {
            return Math.pow(Math.min(amount, 64), 1.0 / increaseExponent);
        }
        double logFactor = 1.0 / (1.0 + volumeDampeningScale * Math.log1p(amount));
        logFactor = Math.max(volumeDampeningMinFactor, logFactor);
        return amount * logFactor;
    }

    private double applyAdvancedModifiers(double rawMult, Material material, boolean isBuy) {
        double mult = rawMult;

        if (supplyCurveEnabled) {
            long circulation = circulationCounts.getOrDefault(material, 0L);
            double ratio = (double) (circulation + 1) / (double) (supplyCurveBaseline + 1);
            double supplyPressure;
            if (!isBuy) {
                supplyPressure = 1.0 - supplyCurveStrength * Math.log1p(Math.max(0, ratio - 1.0));
            } else {
                supplyPressure = 1.0 + supplyCurveStrength * Math.log1p(Math.max(0, 1.0 - ratio));
            }
            mult *= Math.max(0.5, Math.min(1.5, supplyPressure));
        }

        if (inflationEnabled) {
            mult *= inflationCoefficient;
        }

        return Math.max(minMultiplier, Math.min(maxMultiplier, mult));
    }

    private double getVelocityDampeningFactor(Material material) {
        if (!velocityEnabled) return 1.0;

        List<Long> log = velocityLog.get(material);
        if (log == null || log.isEmpty()) return 1.0 + velocityStabilityFactor;

        long now = System.currentTimeMillis();
        long window = velocityWindowMinutes * 60_000L;

        long recentTrades = 0;
        for (Long ts : log) {
            if ((now - ts) <= window) recentTrades++;
        }

        double normalised = recentTrades / 10.0;

        if (normalised >= 1.0) {
            double dampening = 1.0 - velocityStabilityFactor * Math.log1p(normalised - 1.0);
            return Math.max(0.5, dampening);
        } else {
            double amplification = 1.0 + velocityStabilityFactor * (1.0 - normalised);
            return Math.min(1.5, amplification);
        }
    }

    private void recordVelocityEvent(Material material) {
        if (!velocityEnabled) return;
        velocityLog.computeIfAbsent(material, k -> Collections.synchronizedList(new ArrayList<>()));
        velocityLog.get(material).add(System.currentTimeMillis());
    }

    // Decay logic

    private void startTasks() {
        long decayTicks = decayIntervalMinutes * 60L * 20L;
        if (decayTicks < 20) decayTicks = 20;

        decayTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::decayMultipliers, decayTicks, decayTicks);

        long snapshotTicks = timeWindowMinutes * 60L * 20L;
        if (snapshotTicks < 20) snapshotTicks = 20;
        snapshotTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::takeSnapshots, snapshotTicks, snapshotTicks);
    }

    private void decayMultipliers() {
        long now = System.currentTimeMillis();
        long window = timeWindowMinutes * 60_000L;

        for (Iterator<Map.Entry<String, Double>> it = multipliers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Double> entry = it.next();
            String key = entry.getKey();
            double current = entry.getValue();

            List<Long> log = salesLog.get(key);
            if (log != null) {
                log.removeIf(ts -> (now - ts) > window);
            }
            int recentSales = (log != null) ? log.size() : 0;

            double distanceFrom1 = Math.abs(current - 1.0);
            if (distanceFrom1 < 0.001) {
                it.remove();
                salesLog.remove(key);
                continue;
            }

            double salesDampen = 1.0;
            if (recentSales > 0) {
                salesDampen = 1.0 / (1.0 + recentSales * 0.1);
            }

            double decayAmount = baseMultiplierDecrement
                    * Math.pow(distanceFrom1, decayExponent)
                    * salesDampen;

            if (current < 1.0) {
                current = Math.min(1.0, current + decayAmount);
            } else {
                current = Math.max(1.0, current - decayAmount);
            }
            entry.setValue(current);
        }

        if (cooldownEnabled) {
            long cutoff = now - (cooldownSeconds * 2000L);
            cooldownTracker.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        if (velocityEnabled) {
            long velocityWindow = velocityWindowMinutes * 60_000L;
            for (List<Long> vLog : velocityLog.values()) {
                vLog.removeIf(ts -> (now - ts) > velocityWindow);
            }
            velocityLog.entrySet().removeIf(e -> e.getValue().isEmpty());
        }

        if (inflationEnabled) {
            long totalCirculation = 0;
            for (long c : circulationCounts.values()) {
                totalCirculation += c;
            }
            double circulationFactor = 1.0 / (1.0 + totalCirculation / 100_000.0);
            inflationCoefficient += inflationRate * circulationFactor;
            inflationCoefficient = Math.max(minInflationCoefficient,
                    Math.min(maxInflationCoefficient, inflationCoefficient));
        }

        if (perUser) {
            for (Iterator<Map.Entry<String, Double>> it2 = multipliers.entrySet().iterator(); it2.hasNext(); ) {
                Map.Entry<String, Double> entry = it2.next();
                String key = entry.getKey();
                if (!key.contains(":")) continue;
                if (Math.abs(entry.getValue() - 1.0) < 0.001) {
                    List<Long> userLog = salesLog.get(key);
                    if (userLog == null || userLog.isEmpty()) {
                        it2.remove();
                        salesLog.remove(key);
                    }
                }
            }
        }
    }

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
        saveDataAsync();
    }

    private void recordSnapshot(Material material) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) return;
        EconomyManager.PriceResult base = eco.getBasePrice(material);
        if (!base.sellable) return;

        double rawMult = multipliers.getOrDefault(material.name(), 1.0);
        double effectiveMult = applyAdvancedModifiers(rawMult, material, false);
        BigDecimal rawPrice = base.price.multiply(BigDecimal.valueOf(effectiveMult))
                .setScale(eco.getDecimals(), RoundingMode.HALF_UP);

        BigDecimal snapshotPrice;
        if (emaSmoothingEnabled) {
            double rawVal = rawPrice.doubleValue();
            Double prevEma = emaState.get(material);
            double ema;
            if (prevEma == null) {
                ema = rawVal;
            } else {
                ema = emaAlpha * rawVal + (1.0 - emaAlpha) * prevEma;
            }
            emaState.put(material, ema);
            snapshotPrice = BigDecimal.valueOf(ema).setScale(eco.getDecimals(), RoundingMode.HALF_UP);
        } else {
            snapshotPrice = rawPrice;
        }

        List<PriceSnapshot> history = priceHistory.computeIfAbsent(material,
                k -> Collections.synchronizedList(new ArrayList<>()));
        history.add(new PriceSnapshot(System.currentTimeMillis(), snapshotPrice));

        while (history.size() > 1000) {
            history.remove(0);
        }
    }

    // Persistence

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "dynamic-pricing-data.yml");
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection multSec = dataConfig.getConfigurationSection("multipliers");
        if (multSec != null) {
            for (String key : multSec.getKeys(false)) {
                multipliers.put(key, multSec.getDouble(key, 1.0));
            }
        }

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

        ConfigurationSection circSec = dataConfig.getConfigurationSection("circulation");
        if (circSec != null) {
            for (String matName : circSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName);
                    circulationCounts.put(mat, Math.max(0L, circSec.getLong(matName, 0L)));
                } catch (Exception ignored) {}
            }
        }

        inflationCoefficient = dataConfig.getDouble("inflation-coefficient", 1.0);

        ConfigurationSection emaSec = dataConfig.getConfigurationSection("ema-state");
        if (emaSec != null) {
            for (String matName : emaSec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName);
                    emaState.put(mat, emaSec.getDouble(matName));
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveData() {
        if (dataConfig == null) dataConfig = new YamlConfiguration();

        dataConfig.set("multipliers", null);
        for (Map.Entry<String, Double> entry : multipliers.entrySet()) {
            dataConfig.set("multipliers." + entry.getKey(), entry.getValue());
        }

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

        dataConfig.set("circulation", null);
        for (Map.Entry<Material, Long> entry : circulationCounts.entrySet()) {
            dataConfig.set("circulation." + entry.getKey().name(), Math.max(0L, entry.getValue()));
        }

        dataConfig.set("inflation-coefficient", inflationCoefficient);

        dataConfig.set("ema-state", null);
        for (Map.Entry<Material, Double> entry : emaState.entrySet()) {
            dataConfig.set("ema-state." + entry.getKey().name(), entry.getValue());
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

    // Utility

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

    // Getters for commands
    public boolean isPerUser() { return perUser; }
    public double getMaxMultiplier() { return maxMultiplier; }
    public double getMinMultiplier() { return minMultiplier; }
    public int getTimeWindowMinutes() { return timeWindowMinutes; }
    public List<String> getStatusLabels() { return Collections.unmodifiableList(statusLabels); }
    public long getCirculation(Material material) { return circulationCounts.getOrDefault(material, 0L); }
    public double getInflationCoefficient() { return inflationCoefficient; }
    public boolean isSpreadEnabled() { return spreadEnabled; }
    public double getSpreadPercent() { return spreadPercent; }
    public boolean isEmaSmoothingEnabled() { return emaSmoothingEnabled; }
    public boolean isVolumeDampeningEnabled() { return volumeDampeningEnabled; }
    public boolean isSupplyCurveEnabled() { return supplyCurveEnabled; }
    public boolean isInflationEnabled() { return inflationEnabled; }
    public boolean isVelocityEnabled() { return velocityEnabled; }
    public boolean isCooldownEnabled() { return cooldownEnabled; }
}
