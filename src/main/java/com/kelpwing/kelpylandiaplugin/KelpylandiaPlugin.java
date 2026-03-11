package com.kelpwing.kelpylandiaplugin;

import com.kelpwing.kelpylandiaplugin.chat.ChannelManager;
import com.kelpwing.kelpylandiaplugin.chat.commands.ChatCommand;
import com.kelpwing.kelpylandiaplugin.chat.commands.ChannelCommand;
import com.kelpwing.kelpylandiaplugin.chat.commands.ChannelAliasCommand;
import com.kelpwing.kelpylandiaplugin.chat.listeners.ChatListener;
import com.kelpwing.kelpylandiaplugin.commands.GamemodeCommand;
import com.kelpwing.kelpylandiaplugin.commands.NickCommand;
import com.kelpwing.kelpylandiaplugin.commands.PvpCommand;
import com.kelpwing.kelpylandiaplugin.commands.SpawnCommand;
import com.kelpwing.kelpylandiaplugin.commands.WorkbenchCommand;
import com.kelpwing.kelpylandiaplugin.commands.EnchantCommand;
import com.kelpwing.kelpylandiaplugin.commands.AfkCommand;
import com.kelpwing.kelpylandiaplugin.commands.MsgCommand;
import com.kelpwing.kelpylandiaplugin.commands.ReplyCommand;
import com.kelpwing.kelpylandiaplugin.commands.WhisperToggleCommand;
import com.kelpwing.kelpylandiaplugin.commands.SuicideCommand;
import com.kelpwing.kelpylandiaplugin.commands.SkullCommand;
import com.kelpwing.kelpylandiaplugin.commands.RepairCommand;
import com.kelpwing.kelpylandiaplugin.commands.SudoCommand;
import com.kelpwing.kelpylandiaplugin.commands.SocialSpyCommand;
import com.kelpwing.kelpylandiaplugin.commands.CommandSpyCommand;
import com.kelpwing.kelpylandiaplugin.commands.FlyCommand;
import com.kelpwing.kelpylandiaplugin.commands.GodCommand;
import com.kelpwing.kelpylandiaplugin.commands.HealCommand;
import com.kelpwing.kelpylandiaplugin.commands.StarveCommand;
import com.kelpwing.kelpylandiaplugin.commands.FeedCommand;
import com.kelpwing.kelpylandiaplugin.commands.FlySpeedCommand;
import com.kelpwing.kelpylandiaplugin.listeners.SpyListener;
import com.kelpwing.kelpylandiaplugin.homes.HomeGUI;
import com.kelpwing.kelpylandiaplugin.homes.HomeManager;
import com.kelpwing.kelpylandiaplugin.homes.commands.DelHomeCommand;
import com.kelpwing.kelpylandiaplugin.homes.commands.HomeCommand;
import com.kelpwing.kelpylandiaplugin.homes.commands.HomesCommand;
import com.kelpwing.kelpylandiaplugin.homes.commands.SetHomeCommand;
import com.kelpwing.kelpylandiaplugin.moderation.commands.*;
import com.kelpwing.kelpylandiaplugin.moderation.listeners.PlayerListener;
import com.kelpwing.kelpylandiaplugin.listeners.ConsoleListener;
import com.kelpwing.kelpylandiaplugin.listeners.NickListener;
import com.kelpwing.kelpylandiaplugin.listeners.AfkListener;
import com.kelpwing.kelpylandiaplugin.listeners.MsgListener;
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
import com.kelpwing.kelpylandiaplugin.utils.NickManager;
import com.kelpwing.kelpylandiaplugin.utils.AfkManager;
import com.kelpwing.kelpylandiaplugin.utils.VanishManager;
import com.kelpwing.kelpylandiaplugin.utils.SpyManager;
import com.kelpwing.kelpylandiaplugin.utils.PlayerStateManager;
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
    private NickManager nickManager;
    private AfkManager afkManager;
    private MsgCommand msgCommand;
    private SpyManager spyManager;
    private GodCommand godCommand;
    private VanishCommand vanishCommand;
    private PlayerStateManager playerStateManager;
    
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
        
        // Initialize player state manager (state persistence)
        playerStateManager = new PlayerStateManager(this);
        
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
        
        // Register channel alias commands (e.g. /l for local, /g for global)
        for (Map.Entry<String, String> entry : channelManager.getChannelAliases().entrySet()) {
            String alias = entry.getKey();
            String channelName = entry.getValue();
            registerCommand(alias, new ChannelAliasCommand(this, channelName), 
                "Switch to the " + channelName + " channel.", "/" + alias, 
                "kelpylandia.channel.use");
            getLogger().info("Registered channel alias: /" + alias + " -> " + channelName);
        }
        
        // Register moderation commands
        vanishCommand = new VanishCommand(this);
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
        
        // Register spawn command
        if (getConfig().getBoolean("spawn.enabled", true)) {
            SpawnCommand spawnCmd = new SpawnCommand(this);
            registerCommand("spawn", spawnCmd, "Teleport to the world spawn point.", "/spawn [player]", "kelpylandia.spawn");
            getLogger().info("Spawn command enabled!");
        }
        
        // Register gamemode commands
        if (getConfig().getBoolean("gamemode.enabled", true)) {
            GamemodeCommand gmCmd = new GamemodeCommand(this);
            registerCommand("gm", gmCmd, "Change your gamemode.", "/gm <mode> [player]", "kelpylandia.gamemode.*", "gamemode");
            registerCommand("gmc", gmCmd, "Switch to creative mode.", "/gmc [player]", "kelpylandia.gamemode.creative");
            registerCommand("gms", gmCmd, "Switch to survival mode.", "/gms [player]", "kelpylandia.gamemode.survival");
            registerCommand("gma", gmCmd, "Switch to adventure mode.", "/gma [player]", "kelpylandia.gamemode.adventure");
            registerCommand("gmsp", gmCmd, "Switch to spectator mode.", "/gmsp [player]", "kelpylandia.gamemode.spectator");
            getLogger().info("Gamemode commands enabled!");
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
        
        // Register nickname command
        if (getConfig().getBoolean("nickname.enabled", true)) {
            nickManager = new NickManager(this);
            NickCommand nickCmd = new NickCommand(this);
            registerCommand("nick", nickCmd, "Set or reset your nickname.", "/nick [nickname] OR /nick <player> [nickname]", "kelpylandia.nickname", "nickname");
            getServer().getPluginManager().registerEvents(new NickListener(this), this);
            getLogger().info("Nickname command enabled!");
        }
        
        // Register enchant command
        if (getConfig().getBoolean("enchant.enabled", true)) {
            EnchantCommand enchantCmd = new EnchantCommand(this);
            registerCommand("enchant", enchantCmd, "Enchant the item in your hand.", "/enchant <enchantment> [level]", "kelpylandia.enchant");
            getLogger().info("Enchant command enabled!");
        }
        
        // Register AFK system
        if (getConfig().getBoolean("afk.enabled", true)) {
            afkManager = new AfkManager(this);
            AfkCommand afkCmd = new AfkCommand(this);
            registerCommand("afk", afkCmd, "Toggle your AFK status.", "/afk", "kelpylandia.afk");
            getServer().getPluginManager().registerEvents(new AfkListener(this), this);
            getLogger().info("AFK system enabled!");
        }
        
        // Register messaging commands (/w, /r, /wt)
        if (getConfig().getBoolean("messaging.enabled", true)) {
            msgCommand = new MsgCommand(this);
            ReplyCommand replyCmd = new ReplyCommand(this);
            WhisperToggleCommand wtCmd = new WhisperToggleCommand(this);
            
            registerCommand("w", msgCommand, "Send a private message.", "/w <player> <message>", "kelpylandia.msg", "msg", "tell", "whisper");
            registerCommand("r", replyCmd, "Reply to the last private message.", "/r <message>", "kelpylandia.msg.reply", "reply");
            registerCommand("wt", wtCmd, "Set or clear your whisper target.", "/wt [player]", "kelpylandia.msg.whispertoggle", "whispertoggle");
            getServer().getPluginManager().registerEvents(new MsgListener(this), this);
            getLogger().info("Messaging commands enabled!");
        }
        
        // Register suicide command
        if (getConfig().getBoolean("suicide.enabled", true)) {
            SuicideCommand suicideCmd = new SuicideCommand(this);
            registerCommand("suicide", suicideCmd, "Kill yourself.", "/suicide", "kelpylandia.suicide");
            getLogger().info("Suicide command enabled!");
        }
        
        // Register skull command
        if (getConfig().getBoolean("skull.enabled", true)) {
            SkullCommand skullCmd = new SkullCommand(this);
            registerCommand("skull", skullCmd, "Get a player's head.", "/skull [player]", "kelpylandia.skull", "head", "playerhead");
            getLogger().info("Skull command enabled!");
        }
        
        // Register repair command
        if (getConfig().getBoolean("repair.enabled", true)) {
            RepairCommand repairCmd = new RepairCommand(this);
            registerCommand("repair", repairCmd, "Repair items.", "/repair [hand|helmet|chestplate|pants|boots|offhand|all] [player]", "kelpylandia.repair", "fix");
            getLogger().info("Repair command enabled!");
        }
        
        // Register sudo command
        if (getConfig().getBoolean("sudo.enabled", true)) {
            SudoCommand sudoCmd = new SudoCommand(this);
            registerCommand("sudo", sudoCmd, "Force a player to run a command or send a message.", "/sudo <player> <command or message>", "kelpylandia.sudo");
            getLogger().info("Sudo command enabled!");
        }
        
        // Register spy commands (/ss, /cs) and listener
        if (getConfig().getBoolean("spy.enabled", true)) {
            spyManager = new SpyManager();
            SocialSpyCommand ssCmd = new SocialSpyCommand(this);
            CommandSpyCommand csCmd = new CommandSpyCommand(this);
            registerCommand("ss", ssCmd, "Toggle social spy.", "/ss", "kelpylandia.socialspy", "socialspy");
            registerCommand("cs", csCmd, "Toggle command spy.", "/cs", "kelpylandia.commandspy", "commandspy");
            getServer().getPluginManager().registerEvents(new SpyListener(this), this);
            getLogger().info("Spy commands enabled!");
        }
        
        // Register fly command
        if (getConfig().getBoolean("fly.enabled", true)) {
            FlyCommand flyCmd = new FlyCommand(this);
            registerCommand("fly", flyCmd, "Toggle flight mode.", "/fly [player]", "kelpylandia.fly", "flight");
            getLogger().info("Fly command enabled!");
        }
        
        // Register god command
        if (getConfig().getBoolean("god.enabled", true)) {
            godCommand = new GodCommand(this);
            registerCommand("god", godCommand, "Toggle god mode (invincibility).", "/god [player]", "kelpylandia.god", "godmode");
            getLogger().info("God command enabled!");
        }
        
        // Register heal command
        if (getConfig().getBoolean("heal.enabled", true)) {
            HealCommand healCmd = new HealCommand(this);
            registerCommand("heal", healCmd, "Heal a player to full health.", "/heal [player]", "kelpylandia.heal");
            getLogger().info("Heal command enabled!");
        }
        
        // Register starve command
        if (getConfig().getBoolean("starve.enabled", true)) {
            StarveCommand starveCmd = new StarveCommand(this);
            registerCommand("starve", starveCmd, "Set a player's food to zero.", "/starve [player]", "kelpylandia.starve");
            getLogger().info("Starve command enabled!");
        }
        
        // Register feed command
        if (getConfig().getBoolean("feed.enabled", true)) {
            FeedCommand feedCmd = new FeedCommand(this);
            registerCommand("feed", feedCmd, "Fill a player's food bar.", "/feed [player]", "kelpylandia.feed");
            getLogger().info("Feed command enabled!");
        }
        
        // Register flyspeed / walkspeed commands
        if (getConfig().getBoolean("flyspeed.enabled", true)) {
            FlySpeedCommand speedCmd = new FlySpeedCommand(this);
            registerCommand("flyspeed", speedCmd, "Set fly speed (0-10).", "/flyspeed <speed> [player]", "kelpylandia.flyspeed", "fspeed");
            registerCommand("walkspeed", speedCmd, "Set walk speed (0-10).", "/walkspeed <speed> [player]", "kelpylandia.flyspeed", "wspeed");
            getLogger().info("Fly/Walk speed commands enabled!");
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
    
    public NickManager getNickManager() {
        return nickManager;
    }
    
    public AfkManager getAfkManager() {
        return afkManager;
    }
    
    public MsgCommand getMsgCommand() {
        return msgCommand;
    }
    
    public SpyManager getSpyManager() {
        return spyManager;
    }
    
    public GodCommand getGodCommand() {
        return godCommand;
    }
    
    public VanishCommand getVanishCommand() {
        return vanishCommand;
    }
    
    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }
}
