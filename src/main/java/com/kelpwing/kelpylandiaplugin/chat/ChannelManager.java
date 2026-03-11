package com.kelpwing.kelpylandiaplugin.chat;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {
    
    private final KelpylandiaPlugin plugin;
    private final Map<String, Channel> channels;
    private final Map<UUID, String> playerChannels;
    private final Map<UUID, Set<String>> playerMutedChannels;
    /** Maps an alias (e.g. "l") to a channel name (e.g. "local"). */
    private final Map<String, String> channelAliases;
    
    public ChannelManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.channels = new ConcurrentHashMap<>();
        this.playerChannels = new ConcurrentHashMap<>();
        this.playerMutedChannels = new ConcurrentHashMap<>();
        this.channelAliases = new ConcurrentHashMap<>();
        
        loadChannelsFromConfig();
    }
    
    private void loadChannelsFromConfig() {
        ConfigurationSection channelsSection = plugin.getConfig().getConfigurationSection("channels");
        if (channelsSection == null) {
            // Create default channels
            createDefaultChannels();
            return;
        }
        
        for (String channelName : channelsSection.getKeys(false)) {
            ConfigurationSection channelSection = channelsSection.getConfigurationSection(channelName);
            if (channelSection != null) {
                Channel channel = new Channel(
                    channelName,
                    channelSection.getString("display-name", channelName),
                    channelSection.getString("format", "&7[{channel}] {prefix}{player}: {message}"),
                    channelSection.getString("permission", "KelpylandiaPlugin.channel." + channelName),
                    channelSection.getBoolean("proximity", false),
                    channelSection.getDouble("proximity-distance", 50.0),
                    channelSection.getBoolean("discord-enabled", false),
                    channelSection.getString("discord-channel", null),
                    channelSection.getBoolean("default", false)
                );
                
                channels.put(channelName.toLowerCase(), channel);
                
                // Read aliases (e.g. alias: "l" or aliases: ["l", "loc"])
                if (channelSection.contains("alias")) {
                    String alias = channelSection.getString("alias", "").toLowerCase();
                    if (!alias.isEmpty()) {
                        channelAliases.put(alias, channelName.toLowerCase());
                    }
                }
                if (channelSection.contains("aliases")) {
                    List<String> aliases = channelSection.getStringList("aliases");
                    for (String alias : aliases) {
                        channelAliases.put(alias.toLowerCase(), channelName.toLowerCase());
                    }
                }
            }
        }
        
        if (channels.isEmpty()) {
            createDefaultChannels();
        }
    }
    
    private void createDefaultChannels() {
        // Global channel
        Channel global = new Channel(
            "global",
            "Global",
            "&2[G]&r {prefix}{player}&r: {message}",
            "KelpylandiaPlugin.channel.global",
            false,
            0,
            true,
            "0000000000000000000",
            true
        );
        channels.put("global", global);
        
        // Local proximity channel
        Channel local = new Channel(
            "local",
            "Local",
            "&e[L]&r {prefix}{player}&r: {message}",
            "KelpylandiaPlugin.channel.local",
            true,
            50.0,
            false,
            null,
            false
        );
        channels.put("local", local);
        
        // Admin channel
        Channel admin = new Channel(
            "admin",
            "Admin",
            "&c[A]&r {prefix}{player}&r: {message}",
            "KelpylandiaPlugin.channel.admin",
            false,
            0,
            true,
            "0000000000000000000",
            false
        );
        channels.put("admin", admin);
    }
    
    public void addChannel(Channel channel) {
        channels.put(channel.getName().toLowerCase(), channel);
    }
    
    public void removeChannel(String name) {
        channels.remove(name.toLowerCase());
        // Move players from deleted channel to default
        String defaultChannel = getDefaultChannelName();
        playerChannels.replaceAll((uuid, channelName) -> 
            channelName.equalsIgnoreCase(name) ? defaultChannel : channelName);
    }
    
    public Channel getChannel(String name) {
        Channel ch = channels.get(name.toLowerCase());
        if (ch != null) return ch;
        // Try to resolve via alias
        String resolved = channelAliases.get(name.toLowerCase());
        return resolved != null ? channels.get(resolved) : null;
    }
    
    /** Returns the mapping of alias -> channel name. */
    public Map<String, String> getChannelAliases() {
        return Collections.unmodifiableMap(channelAliases);
    }
    
    public Collection<Channel> getChannels() {
        return channels.values();
    }
    
    public Set<String> getChannelNames() {
        return new HashSet<>(channels.keySet());
    }
    
    public String getPlayerChannel(UUID playerId) {
        return playerChannels.getOrDefault(playerId, getDefaultChannelName());
    }
    
    public void setPlayerChannel(UUID playerId, String channelName) {
        if (channels.containsKey(channelName.toLowerCase())) {
            playerChannels.put(playerId, channelName.toLowerCase());
        }
    }
    
    public String getDefaultChannelName() {
        for (Channel channel : channels.values()) {
            if (channel.isDefault()) {
                return channel.getName();
            }
        }
        return "global";
    }
    
    public boolean hasPermission(Player player, String channelName) {
        Channel channel = getChannel(channelName);
        if (channel == null) return false;
        
        return player.hasPermission(channel.getPermission()) || player.isOp();
    }
    
    public void muteChannel(UUID playerId, String channelName) {
        playerMutedChannels.computeIfAbsent(playerId, k -> new HashSet<>()).add(channelName.toLowerCase());
    }
    
    public void unmuteChannel(UUID playerId, String channelName) {
        Set<String> mutedChannels = playerMutedChannels.get(playerId);
        if (mutedChannels != null) {
            mutedChannels.remove(channelName.toLowerCase());
            if (mutedChannels.isEmpty()) {
                playerMutedChannels.remove(playerId);
            }
        }
    }
    
    public boolean isChannelMuted(UUID playerId, String channelName) {
        Set<String> mutedChannels = playerMutedChannels.get(playerId);
        return mutedChannels != null && mutedChannels.contains(channelName.toLowerCase());
    }
    
    public Set<String> getMutedChannels(UUID playerId) {
        return new HashSet<>(playerMutedChannels.getOrDefault(playerId, new HashSet<>()));
    }
    
    public void onPlayerLeave(UUID playerId) {
        playerChannels.remove(playerId);
        playerMutedChannels.remove(playerId);
    }
    
    public void setPlayerToDefaultChannel(Player player) {
        String defaultChannelName = getDefaultChannelName();
        setPlayerChannel(player.getUniqueId(), defaultChannelName);
    }
    
    public void removePlayerFromChannel(Player player) {
        onPlayerLeave(player.getUniqueId());
    }
    
    public Channel getPlayerChannel(Player player) {
        String channelName = getPlayerChannel(player.getUniqueId());
        return getChannel(channelName);
    }
    
    public Channel getDefaultChannel() {
        String defaultChannelName = getDefaultChannelName();
        return getChannel(defaultChannelName);
    }
    
    public List<Channel> getAllChannels() {
        return new ArrayList<>(channels.values());
    }
    
    public List<Channel> getAvailableChannels(Player player) {
        List<Channel> availableChannels = new ArrayList<>();
        for (Channel channel : channels.values()) {
            if (hasPermission(player, channel.getName())) {
                availableChannels.add(channel);
            }
        }
        return availableChannels;
    }
    
    public List<Player> getPlayersInChannel(Channel channel) {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerChannels.entrySet()) {
            if (entry.getValue().equals(channel.getName().toLowerCase())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
        }
        return players;
    }
    
    public void saveChannel(Channel channel) {
        // Save channel to config
        plugin.getConfig().set("channels." + channel.getName() + ".display-name", channel.getDisplayName());
        plugin.getConfig().set("channels." + channel.getName() + ".format", channel.getFormat());
        plugin.getConfig().set("channels." + channel.getName() + ".permission", channel.getPermission());
        plugin.getConfig().set("channels." + channel.getName() + ".proximity", channel.isProximity());
        plugin.getConfig().set("channels." + channel.getName() + ".proximity-distance", channel.getProximityDistance());
        plugin.getConfig().set("channels." + channel.getName() + ".discord-enabled", channel.isDiscordEnabled());
        plugin.getConfig().set("channels." + channel.getName() + ".discord-channel", channel.getDiscordChannel());
        plugin.getConfig().set("channels." + channel.getName() + ".default", channel.isDefault());
        plugin.saveConfig();
    }
    
    public void reloadChannels() {
        channels.clear();
        channelAliases.clear();
        plugin.reloadConfig();
        loadChannelsFromConfig();
    }
}
