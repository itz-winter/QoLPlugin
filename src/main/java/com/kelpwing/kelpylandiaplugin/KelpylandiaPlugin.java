package com.kelpwing.kelpylandiaplugin;

import com.kelpwing.kelpylandiaplugin.chat.ChannelManager;
import com.kelpwing.kelpylandiaplugin.chat.commands.ChatCommand;
import com.kelpwing.kelpylandiaplugin.chat.commands.ChannelCommand;
import com.kelpwing.kelpylandiaplugin.chat.listeners.ChatListener;
import com.kelpwing.kelpylandiaplugin.commands.PvpCommand;
import com.kelpwing.kelpylandiaplugin.commands.WorkbenchCommand;
import com.kelpwing.kelpylandiaplugin.homes.HomeGUI;
import com.kelpwing.kelpylandiaplugin.homes.HomeManager;
import com.kelpwing.kelpylandiaplugin.homes.commands.DelHomeCommand;
import com.kelpwing.kelpylandiaplugin.homes.commands.HomeCommand;
import com.kelpwing.kelpylandiaplugin.homes.commands.HomesCommand;
import com.kelpwing.kelpylandiaplugin.homes.commands.SetHomeCommand;
import com.kelpwing.kelpylandiaplugin.moderation.commands.*;
import com.kelpwing.kelpylandiaplugin.moderation.listeners.PlayerListener;
import com.kelpwing.kelpylandiaplugin.listeners.ConsoleListener;
import com.kelpwing.kelpylandiaplugin.integrations.DiscordIntegration;
import com.kelpwing.kelpylandiaplugin.integrations.LuckPermsIntegration;
import com.kelpwing.kelpylandiaplugin.integrations.PlaceholderAPIIntegration;
import com.kelpwing.kelpylandiaplugin.config.ConfigManager;
import com.kelpwing.kelpylandiaplugin.teleport.TeleportListener;
import com.kelpwing.kelpylandiaplugin.teleport.TpaManager;
import com.kelpwing.kelpylandiaplugin.teleport.BackManager;
import com.kelpwing.kelpylandiaplugin.teleport.BackListener;
import com.kelpwing.kelpylandiaplugin.teleport.commands.*;
import com.kelpwing.kelpylandiaplugin.utils.FileManager;
import com.kelpwing.kelpylandiaplugin.utils.VanishManager;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import com.kelpwing.kelpylandiaplugin.placeholders.KpauPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KelpylandiaPlugin extends JavaPlugin {
    
    private static KelpylandiaPlugin instance;
    
    // Chat-related components
    private ChannelManager channelManager;
    private LuckPermsIntegration luckPermsIntegration;
    private PlaceholderAPIIntegration placeholderAPIIntegration;
    private DiscordIntegration discordIntegration;
    
    // Moderation-related components
    private ConfigManager configManager;
    private FileManager fileManager;
    private VanishManager vanishManager;
    private Map<UUID, Long> mutedPlayers;
    private PvpCommand pvpCommand;
    
    // Homes & Teleport components
    private HomeManager homeManager;
    private HomeGUI homeGUI;
    private TpaManager tpaManager;
    private BackManager backManager;
    
    @Override
    public void onEnable() {
        instance = this;
        mutedPlayers = new HashMap<>();
        
        // Detect server version and platform
        VersionHelper.init(getLogger());
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize file manager
        fileManager = new FileManager(this);
        
        // Initialize vanish manager
        vanishManager = new VanishManager(this);
        
        // Initialize channel manager
        channelManager = new ChannelManager(this);
        
        // Initialize homes system
        if (getConfig().getBoolean("homes.enabled", true)) {
            homeManager = new HomeManager(this);
            homeGUI = new HomeGUI(this);
            getLogger().info("Homes system enabled!");
        }
        
        // Initialize teleport system
        if (getConfig().getBoolean("teleport.enabled", true)) {
            tpaManager = new TpaManager(this);
            getLogger().info("Teleport request system enabled!");
        }
        
        // Initialize back system
        if (getConfig().getBoolean("back.enabled", true)) {
            backManager = new BackManager(this);
            getLogger().info("Back system enabled!");
        }
        
        // Initialize integrations
        initializeIntegrations();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kelpwing.kelpylandiaplugin.listeners.PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kelpwing.kelpylandiaplugin.listeners.ConsoleEventListener(this), this);
        getServer().getPluginManager().registerEvents(new com.kelpwing.kelpylandiaplugin.listeners.AdvancementListener(this), this);
        
        // Register console listener for Discord logging
        ConsoleListener consoleListener = new ConsoleListener(this);
        getServer().getPluginManager().registerEvents(consoleListener, this);
        
        // Register chat commands
        getCommand("channel").setExecutor(new ChatCommand(this));
        getCommand("multichat").setExecutor(new ChannelCommand(this));
        
        // Register moderation commands
        VanishCommand vanishCommand = new VanishCommand(this);
        InvseeCommand invseeCommand = new InvseeCommand(this);
        MuteCommand muteCommand = new MuteCommand(this);
        BanCommand banCommand = new BanCommand(this);
        pvpCommand = new PvpCommand(this);
        
        getCommand("mute").setExecutor(muteCommand);
        getCommand("mute").setTabCompleter(muteCommand);
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("ban").setExecutor(banCommand);
        getCommand("ban").setTabCompleter(banCommand);
        getCommand("ipban").setExecutor(new IPBanCommand(this));
        getCommand("warns").setExecutor(new WarnsCommand(this));
        getCommand("hist").setExecutor(new HistoryCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("unwarn").setExecutor(new UnwarnCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("vanish").setExecutor(vanishCommand);
        getCommand("invsee").setExecutor(invseeCommand);
        getCommand("invsee").setTabCompleter(invseeCommand);
        getCommand("pvp").setExecutor(pvpCommand);
        getCommand("kelpylandiaReload").setExecutor(new ReloadCommand(this));
        
        // Register vanish listener
        getServer().getPluginManager().registerEvents(new com.kelpwing.kelpylandiaplugin.listeners.VanishListener(this, vanishCommand), this);
        
        // Register invsee listener
        getServer().getPluginManager().registerEvents(new com.kelpwing.kelpylandiaplugin.listeners.InvseeListener(this, invseeCommand), this);
        
        // Register PvP command as listener (for damage events)
        getServer().getPluginManager().registerEvents(pvpCommand, this);
        
        // Register home commands
        if (homeManager != null) {
            SetHomeCommand setHomeCmd = new SetHomeCommand(this);
            DelHomeCommand delHomeCmd = new DelHomeCommand(this);
            HomeCommand homeCmd = new HomeCommand(this);
            HomesCommand homesCmd = new HomesCommand(this);
            
            registerCommand("sethome", setHomeCmd, "Set a home at your current location.", "/sethome [name]", "kelpylandia.homes");
            registerCommand("delhome", delHomeCmd, "Delete a home.", "/delhome <name>", "kelpylandia.homes", "removehome", "remhome");
            registerCommand("home", homeCmd, "Teleport to a home.", "/home [name]", "kelpylandia.homes");
            registerCommand("homes", homesCmd, "List or browse your homes.", "/homes", "kelpylandia.homes");
            
            // Register HomeGUI as listener for inventory clicks
            getServer().getPluginManager().registerEvents(homeGUI, this);
        }
        
        // Register TPA commands
        if (tpaManager != null) {
            TpaCommand tpaCmd = new TpaCommand(this);
            TpaHereCommand tpaHereCmd = new TpaHereCommand(this);
            TpAcceptCommand tpAcceptCmd = new TpAcceptCommand(this);
            TpDenyCommand tpDenyCmd = new TpDenyCommand(this);
            TpCancelCommand tpCancelCmd = new TpCancelCommand(this);
            
            registerCommand("tpa", tpaCmd, "Send a teleport request to a player.", "/tpa <player>", "kelpylandia.tpa", "tpask");
            registerCommand("tpahere", tpaHereCmd, "Request a player to teleport to you.", "/tpahere <player>", "kelpylandia.tpa.here");
            registerCommand("tpaccept", tpAcceptCmd, "Accept a pending teleport request.", "/tpaccept [player]", "kelpylandia.tpa", "tpyes");
            registerCommand("tpdeny", tpDenyCmd, "Deny a pending teleport request.", "/tpdeny [player]", "kelpylandia.tpa", "tpno");
            registerCommand("tpcancel", tpCancelCmd, "Cancel your outgoing teleport request.", "/tpcancel", "kelpylandia.tpa");
            
            // Register teleport listener for invulnerability + quit cleanup
            getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        }
        
        // Register back commands
        if (backManager != null) {
            BackCommand backCmd = new BackCommand(this);
            DeathBackCommand dbackCmd = new DeathBackCommand(this);
            
            registerCommand("back", backCmd, "Teleport to your previous location.", "/back", "kelpylandia.back", "return");
            registerCommand("dback", dbackCmd, "Teleport to your last death location.", "/dback", "kelpylandia.dback", "deathback", "dreturn");
            
            // Register back listener for teleport/death tracking
            getServer().getPluginManager().registerEvents(new BackListener(this), this);
        }
        
        // Register workbench commands
        if (getConfig().getBoolean("workbenches.enabled", true)) {
            WorkbenchCommand wbCmd = new WorkbenchCommand(this);
            registerCommand("workbench", wbCmd, "Open a virtual crafting table.", "/workbench [player]", "kelpylandia.workbench.craft", "wb", "craft");
            registerCommand("enderchest", wbCmd, "Open your ender chest (or another player's).", "/enderchest [player]", "kelpylandia.workbench.enderchest", "ec", "echest");
            registerCommand("anvil", wbCmd, "Open a virtual anvil.", "/anvil [player]", "kelpylandia.workbench.anvil");
            registerCommand("grindstone", wbCmd, "Open a virtual grindstone.", "/grindstone [player]", "kelpylandia.workbench.grindstone", "gstone");
            registerCommand("stonecutter", wbCmd, "Open a virtual stonecutter.", "/stonecutter [player]", "kelpylandia.workbench.stonecutter", "scutter");
            registerCommand("smithingtable", wbCmd, "Open a virtual smithing table.", "/smithingtable [player]", "kelpylandia.workbench.smithing", "smithing");
            registerCommand("cartographytable", wbCmd, "Open a virtual cartography table.", "/cartographytable [player]", "kelpylandia.workbench.cartography", "cartography");
            registerCommand("loom", wbCmd, "Open a virtual loom.", "/loom [player]", "kelpylandia.workbench.loom");
            getLogger().info("Workbench commands enabled!");
        }
        
        // Initialize Discord integration if enabled
        if (getConfig().getBoolean("discord.enabled", true)) {
            discordIntegration = new DiscordIntegration(this);
            getLogger().info("Discord integration enabled!");
        }
        
        // Schedule automatic cleanup of expired warnings every hour
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            fileManager.cleanupExpiredWarnings();
        }, 20L * 60L * 60L, 20L * 60L * 60L);
        
        getLogger().info("KelpylandiaPlugin v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Chat and moderation systems are now active.");
        getLogger().info("Server: " + VersionHelper.getVersionSummary());
        
        // Send Discord server start message
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (discordIntegration != null && getConfig().getBoolean("discord.server-lifecycle-events.enabled", true)) {
                discordIntegration.sendServerStartEmbed();
            }
        }, 20L); // Wait 1 second to ensure Discord is fully initialized
    }
    
    @Override
    public void onDisable() {
        // Send Discord server stop message
        if (discordIntegration != null && getConfig().getBoolean("discord.server-lifecycle-events.enabled", true)) {
            discordIntegration.sendServerStopEmbed();
            // Wait a moment for the message to send
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (discordIntegration != null) {
            discordIntegration.disable();
        }
        
        if (tpaManager != null) {
            tpaManager.cleanup();
        }
        
        if (placeholderAPIIntegration != null) {
            placeholderAPIIntegration.unregister();
        }
        
        getLogger().info("KelpylandiaPlugin has been disabled!");
    }
    
    private void initializeIntegrations() {
        // LuckPerms integration
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsIntegration = new LuckPermsIntegration();
            getLogger().info("LuckPerms integration enabled!");
        }
        
        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIIntegration = new PlaceholderAPIIntegration(this);
            placeholderAPIIntegration.register();
            
            // Also register the moderation placeholders
            new KpauPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
    }
    
    /**
     * Dynamically registers a command at runtime via the Bukkit command map.
     * Used for optional features so commands are only registered when the feature is enabled,
     * avoiding conflicts with other plugins that provide the same commands.
     */
    private void registerCommand(String name, CommandExecutor executor, String description, String usage, String permission, String... aliases) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand cmd = constructor.newInstance(name, this);
            
            cmd.setExecutor(executor);
            if (executor instanceof TabCompleter) {
                cmd.setTabCompleter((TabCompleter) executor);
            }
            cmd.setDescription(description);
            cmd.setUsage(usage);
            cmd.setPermission(permission);
            if (aliases.length > 0) {
                cmd.setAliases(Arrays.asList(aliases));
            }
            
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            commandMap.register(getDescription().getName().toLowerCase(), cmd);
        } catch (Exception e) {
            getLogger().warning("Could not register command /" + name + ": " + e.getMessage());
        }
    }
    
    // Static getter
    public static KelpylandiaPlugin getInstance() {
        return instance;
    }
    
    // Chat-related getters
    public ChannelManager getChannelManager() {
        return channelManager;
    }
    
    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }
    
    public PlaceholderAPIIntegration getPlaceholderAPIIntegration() {
        return placeholderAPIIntegration;
    }
    
    public DiscordIntegration getDiscordIntegration() {
        return discordIntegration;
    }
    
    // Moderation-related getters
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public FileManager getFileManager() {
        return fileManager;
    }
    
    public VanishManager getVanishManager() {
        return vanishManager;
    }
    
    public Map<UUID, Long> getMutedPlayers() {
        return mutedPlayers;
    }
    
    public PvpCommand getPvpCommand() {
        return pvpCommand;
    }
    
    // Homes & Teleport getters
    public HomeManager getHomeManager() {
        return homeManager;
    }
    
    public HomeGUI getHomeGUI() {
        return homeGUI;
    }
    
    public TpaManager getTpaManager() {
        return tpaManager;
    }
    
    public BackManager getBackManager() {
        return backManager;
    }
}
