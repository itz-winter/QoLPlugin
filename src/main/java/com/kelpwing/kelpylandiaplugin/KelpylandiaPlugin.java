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
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
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
        } else {
            unregisterCommands("sethome", "delhome", "home", "homes");
            getLogger().info("Homes system disabled — commands released to other plugins.");
        }
        
        // Initialize teleport system
        if (getConfig().getBoolean("teleport.enabled", true)) {
            tpaManager = new TpaManager(this);
            getLogger().info("Teleport request system enabled!");
        } else {
            unregisterCommands("tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel");
            getLogger().info("Teleport system disabled — commands released to other plugins.");
        }
        
        // Initialize back system
        if (getConfig().getBoolean("back.enabled", true)) {
            backManager = new BackManager(this);
            getLogger().info("Back system enabled!");
        } else {
            unregisterCommands("back", "dback");
            getLogger().info("Back system disabled — commands released to other plugins.");
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
            
            getCommand("sethome").setExecutor(setHomeCmd);
            getCommand("sethome").setTabCompleter(setHomeCmd);
            getCommand("delhome").setExecutor(delHomeCmd);
            getCommand("delhome").setTabCompleter(delHomeCmd);
            getCommand("home").setExecutor(homeCmd);
            getCommand("home").setTabCompleter(homeCmd);
            getCommand("homes").setExecutor(homesCmd);
            
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
            
            getCommand("tpa").setExecutor(tpaCmd);
            getCommand("tpa").setTabCompleter(tpaCmd);
            getCommand("tpahere").setExecutor(tpaHereCmd);
            getCommand("tpahere").setTabCompleter(tpaHereCmd);
            getCommand("tpaccept").setExecutor(tpAcceptCmd);
            getCommand("tpaccept").setTabCompleter(tpAcceptCmd);
            getCommand("tpdeny").setExecutor(tpDenyCmd);
            getCommand("tpdeny").setTabCompleter(tpDenyCmd);
            getCommand("tpcancel").setExecutor(tpCancelCmd);
            getCommand("tpcancel").setTabCompleter(tpCancelCmd);
            
            // Register teleport listener for invulnerability + quit cleanup
            getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        }
        
        // Register back commands
        if (backManager != null) {
            BackCommand backCmd = new BackCommand(this);
            DeathBackCommand dbackCmd = new DeathBackCommand(this);
            
            getCommand("back").setExecutor(backCmd);
            getCommand("dback").setExecutor(dbackCmd);
            
            // Register back listener for teleport/death tracking
            getServer().getPluginManager().registerEvents(new BackListener(this), this);
        }
        
        // Register workbench commands
        if (getConfig().getBoolean("workbenches.enabled", true)) {
            WorkbenchCommand wbCmd = new WorkbenchCommand(this);
            String[] wbCommands = {"workbench", "enderchest", "anvil", "grindstone", "stonecutter", "smithingtable", "cartographytable", "loom"};
            for (String cmd : wbCommands) {
                if (getCommand(cmd) != null) {
                    getCommand(cmd).setExecutor(wbCmd);
                    getCommand(cmd).setTabCompleter(wbCmd);
                }
            }
            getLogger().info("Workbench commands enabled!");
        } else {
            unregisterCommands("workbench", "enderchest", "anvil", "grindstone", "stonecutter", "smithingtable", "cartographytable", "loom");
            getLogger().info("Workbench commands disabled — commands released to other plugins.");
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
     * Unregisters commands from the Bukkit command map so other plugins can claim them.
     * Used when a feature is disabled in config to avoid intercepting commands meant for other plugins.
     */
    private void unregisterCommands(String... commandNames) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            
            Field knownCommandsField = commandMap.getClass().getSuperclass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);
            
            String pluginPrefix = getDescription().getName().toLowerCase();
            for (String name : commandNames) {
                String lowerName = name.toLowerCase();
                
                // Look up the command object BEFORE removing anything
                org.bukkit.command.Command cmd = knownCommands.get(lowerName);
                if (cmd == null) {
                    cmd = knownCommands.get(pluginPrefix + ":" + lowerName);
                }
                
                // Collect all keys to remove: main name, prefixed name, and all aliases
                knownCommands.remove(lowerName);
                knownCommands.remove(pluginPrefix + ":" + lowerName);
                
                if (cmd != null) {
                    // Remove all aliases from the map
                    for (String alias : cmd.getAliases()) {
                        knownCommands.remove(alias.toLowerCase());
                        knownCommands.remove(pluginPrefix + ":" + alias.toLowerCase());
                    }
                    // Formally unregister so the command is fully released
                    cmd.unregister(commandMap);
                }
            }
        } catch (Exception e) {
            getLogger().warning("Could not unregister commands: " + e.getMessage());
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
