A lil' custom plugin with a cringe name :3

# KelpylandiaPlugin

## Overview
KelpylandiaPlugin is a highly customizable, all-in-one Minecraft server plugin designed for small to mid-sized SMP servers. Originally built for my own community, it combines advanced chat, moderation, player homes, teleportation, and more—making it a great alternative to EssentialsX and similar plugins. Built on the Spigot/Bukkit API, it supports **Minecraft 1.16 – 1.21.11** (and likely newer).

## Features

### Chat System
- **Channel-Based Communication**: Multiple chat channels (Global, Local, Admin, etc.) with customizable formats and permissions.
- **Proximity Chat**: Local chat within a configurable radius.
- **Custom Channels**: Create and delete your own channels.
- **Discord Integration**: Sync in-game chat with Discord channels.
- **Customizable Formats**: Fully configurable message formats for both Minecraft and Discord.

> **Tip:** For advanced Discord features, [DiscordSRV](https://modrinth.com/plugin/discordsrv) is recommended.

### Moderation Tools
- **Broadcast Actions**: Notify players of bans, kicks, mutes, and other moderation actions.
- **Customizable Punishment Formats**: Define how moderation messages appear.
- **Join/Leave Messages**: Custom join/leave messages, with the option to hide vanilla ones.
- **Rules Command**: Display server rules to players.
- **Ban, Kick, Mute, Warn, Unban, Unmute, Unwarn**: All the essentials for server moderation.

### Player Utilities
- **Homes System**: `/home`, `/sethome`, `/delhome`, `/homes` with GUI and configurable max homes.  
  *(Disabled by default—enable in `config.yml`!)*
- **Teleportation System**: `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel` for player-to-player teleport requests, with cooldowns and invulnerability options.  
  *(Disabled by default—enable in `config.yml`!)*
- **Back Commands**: `/back` to return to your previous location and `/dback` to return to your last death location.  
  *(Configurable cooldown and permissions in `config.yml`!)*
- **Workbench Commands**: Open crafting tables, ender chests, anvils, and more via commands like `/wb`, `/ec`, `/anvil`, etc.  
  *(Configurable permissions and can be disabled to avoid conflicts.)*

### Sitting System (pre-2.1.0 ONLY)
- **Player Sitting**: Sit on other players by right-clicking with an empty hand.
- **Block Sitting**: Sit on stairs, slabs, and other blocks.
- **Configurable**: Toggle sitting features in the config.

> **Note:** For a more robust sitting experience, use [Gsit](https://www.spigotmc.org/resources/gsit-modern-sit-seat-and-chair-lay-and-crawl-plugin-1-16-1-21-11.62325/) and set `sitting.enabled` to `false` in your config.

### General
- **Highly Configurable**: Nearly every feature can be toggled or tweaked in `config.yml`.
- **Permission-Based**: Fine-grained permissions for all commands and features.
- **No EssentialsX Dependency**: Can fully replace EssentialsX for most servers.

## Compatibility

- **Minecraft Versions:** 1.16 – 1.21.11 (auto-detects server version for maximum compatibility)
- **Java:** 17+

## Installation

1. Download the latest release from the [Releases](https://github.com/itz-winter/KelpylandiaPlugin/releases) page.
2. Place the plugin JAR file into your server's `plugins` folder.
3. Start your server to generate the default configuration files.
4. Edit `config.yml` to customize the plugin to your liking.
5. Reload or restart your server to apply changes.

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make your changes and test them thoroughly.
4. Submit a pull request with a clear description of your changes.

## License

This project is licensed under the [MIT License](LICENSE). You are free to use, modify, and distribute this software as long as the original license is included.

---

*Made with love, cringe, and a lot of config options.*
- Kelp • [GitHub](https://github.com/itz-winter/)

# Full Configuration File

```yaml
# KelpylandiaPlugin Configuration File
# Combined chat and moderation system for Kelpylandia

# ===== CHAT SYSTEM CONFIGURATION =====

# Channel Configurations
channels:
  global:
    display-name: "Global"
    format: "&2[G]&r {prefix}{displayname}&r: {message}"
    permission: "kelpylandia.channel.global"
    proximity: false
    proximity-distance: 0.0
    discord-enabled: true
    discord-channel: ''
    default: true
    
  local:
    display-name: "Local"
    format: "&e[L]&r {prefix}{displayname}&r: {message}"
    permission: "kelpylandia.channel.local"
    proximity: true
    proximity-distance: 50.0
    discord-enabled: false
    discord-channel: ''
    default: false
    
  admin:
    display-name: "Admin"
    format: "&c[A]&r {prefix}{displayname}&r: {message}"
    permission: "kelpylandia.channel.admin"
    proximity: false
    proximity-distance: 0.0
    discord-enabled: true
    discord-channel: ''
    default: false

# Discord Bot Integration Settings
discord:
  enabled: true
  bot-token: "your-bot-token-here"
  # Channel for moderation actions
  moderation-channel-id: "your-moderation-channel-id"
  # Channel for general chat (global) 
  chat-channel-id: "your-chat-channel-id"
  # Channel for console logs and server events
  console-channel-id: "your-console-channel-id"
  # Role ID required to use Discord slash moderation commands
  moderation-role-id: "your-moderator-role-id"
  # Role ID for admin commands (rules, echo, restart, etc.)
  admin-role-id: "your-admin-role-id"
  # Role to ping when server starts
  server-start-ping-role-id: "your-server-start-role-id"
  
  # Webhook Configuration
  webhooks:
    # Whether to reuse existing webhooks instead of creating new ones
    reuse-existing: true
    # Delete webhooks when plugin disables (not recommended)
    cleanup-on-disable: false
  
  # Server Event Broadcasting
  events:
    # Broadcast join/leave messages to global chat
    broadcast-joins: true
    broadcast-leaves: true
    broadcast-advancements: true
    broadcast-achievements: true
    # Send server events to console channel
    console-logging: true
    
    # Use embeds for events (like DiscordSRV)
    use-embeds: true
    
    # Server lifecycle events
    server-start: true
    server-stop: true
    server-restart: true
  
  # Channel topic updates (updates every 10 minutes)
  channel-topics:
    enabled: true
    update-interval: 600  # 10 minutes in seconds
    channels:
      # Example: "channel-id": "Server: {server_name} | Players: {online_players}/{max_players} | TPS: {tps}"
      # "123456789012345678": "Kelpylandia | Online: {online_players} | TPS: {tps} | Memory: {memory_used}MB/{memory_max}MB"
    
    # Console channel topic (special case - includes server info)
    console-channel-topic: "TPS: {tps} | Memory: {memory_used}MB/{memory_max}MB | Version: {server_version}"
  
  # Embed styling (colors in hex format)
  embeds:
    join-color: "#00FF00"      # Green
    leave-color: "#FF0000"     # Red  
    advancement-color: "#FFFF00"  # Yellow
    achievement-color: "#FFA500"  # Orange
    punishment-color: "#FF4500"   # Red-Orange
    server-start-color: "#00FF00"  # Green
    server-stop-color: "#FF0000"   # Red
    server-restart-color: "#FFA500" # Orange
  
  formats:
    # How Minecraft messages appear in Discord
    minecraft-to-discord: "{message}"
    
    # How Discord messages appear in Minecraft  
    discord-to-minecraft: "&b[Discord] &r{user}&r: {message}"
    
    # Enable debug logging for Discord relay issues
    debug-relay: true
    
    # Username format for webhook messages
    username-format: "{displayname}"
    # Avatar URL template (uses player UUID)
    avatar-url: "https://mc-heads.net/avatar/{uuid}/64"
    
    # Event formats for Discord
    join: "{player} joined the game"
    leave: "{player} left the game"
    advancement: "**{player}** has made the advancement **{advancement}**!"
    achievement: "**{player}** has completed the achievement **{achievement}**!"
    
    # Server lifecycle event formats
    server-start: ":white_check_mark: **Server has started**"
    server-stop: ":warning: **Server restarting...**"
    server-restart: ":warning: **Server restarting...**"
    
    # Server lifecycle event descriptions
    server-start-description: ""
    server-stop-description: ""
    server-restart-description: ""
    
    # Moderation action format for Discord
    moderation-format: |
      **Action:** {action}
      **Staff:** {staff_name}
      **Player:** {player_name}
      **Reason:** {reason}
      **Duration:** {duration}
      **Server:** {server_name}

# Join/Leave Message Configuration
join-leave:
  # Enable custom join/leave messages
  enabled: true
  # Custom join message format (matches Minecraft default style)
  join-message: '&r[&a+&r] {player}'
  # Custom leave message format (matches Minecraft default style)
  leave-message: '&r[&c-&r] {player}'
  # Hide default Minecraft join/leave messages
  hide-default: true
  # Join/leave messages are broadcast to all players regardless of channel

# ===== MODERATION SYSTEM CONFIGURATION =====

# Broadcast Configuration for moderation actions
broadcast:
  enabled: true
  formats:
    ban: "&c------------------------\n&cAction - BAN\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - {duration}\n&c------------------------"
    kick: "&c------------------------\n&cAction - KICK\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - N/A\n&c------------------------"
    mute: "&c------------------------\n&cAction - MUTE\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - {duration}\n&c------------------------"
    warn: "&c------------------------\n&cAction - WARN\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - 30 days\n&c------------------------"
    ipban: "&c------------------------\n&cAction - IPBAN\n&cStaff Member - {staff_name}\n&cPunished Player - {player_name}\n&cReason - {reason}\n&cDuration - {duration}\n&c------------------------"
    unban: "&a------------------------\n&aAction - UNBAN\n&aStaff Member - {staff_name}\n&aUnbanned Player - {player_name}\n&aReason - {reason}\n&aDuration - N/A\n&a------------------------"
    unmute: "&a------------------------\n&aAction - UNMUTE\n&aStaff Member - {staff_name}\n&aUnmuted Player - {player_name}\n&aReason - {reason}\n&aDuration - N/A\n&a------------------------"
    unwarn: "&a------------------------\n&aAction - UNWARN\n&aStaff Member - {staff_name}\n&aPlayer - {player_name}\n&aReason - {reason}\n&aDuration - N/A\n&a------------------------"

# Colors Configuration for messages
colors:
  primary: "YELLOW"
  secondary: "WHITE"
  error: "RED"
  success: "GREEN"

# Plugin Settings
settings:
  warn-expiry: 2592000  # 30 days in seconds
  check-updates: true
  server-name: ""

# Server Rules (for /rules Discord command)
rules:
  title: "Rules"
  description: "Please follow these rules to ensure a fun experience for everyone!"
  rules-list:
    - "1. Be respectful to all players and staff members"
    - "2. No griefing, stealing, or destroying other players' builds"
    - "3. No cheating, hacking, or using exploits"
    - "4. Keep chat family-friendly and appropriate"
    - "5. No spamming in chat or Discord"
    - "6. Follow staff instructions and decisions"
    - "7. Report any issues or rule violations to staff"
    - "8. Have fun and enjoy your time on the server!"
  footer: "Breaking these rules may result in warnings, mutes, kicks, or bans."

# ===== HOMES CONFIGURATION =====
homes:
  # Enable the homes system (/home, /homes, /sethome, /delhome)
  enabled: true
  # Default maximum homes per player (can be overridden with kelpylandia.homes.max.<n> permission)
  max-homes: 12
  # Use interactive GUI inventory for /homes (false = text list)
  use-gui: true

# ===== TELEPORT REQUEST CONFIGURATION =====
teleport:
  # Enable the TPA system (/tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel)
  enabled: true
  # Cooldown between teleports in seconds
  cooldown: 2
  # Invulnerability duration in seconds after teleporting (0 to disable)
  invulnerability: 4
  # How long a TPA request lasts before expiring in seconds
  request-timeout: 120

# ===== BACK COMMANDS =====
# /back returns to your previous location before teleporting
# /dback returns to your last death location
back:
  enabled: true
  cooldown: 3

# ===== WORKBENCH COMMANDS =====
# Open virtual workbench inventories via commands (like EssentialsX)
# Set enabled to false to disable all workbench commands (avoids conflicts)
# Individual workbench types can also be toggled independently
workbenches:
  enabled: true
  craft: true
  enderchest: true
  anvil: true
  grindstone: true
  stonecutter: true
  smithing: true
  cartography: true
  loom: true

# ===== AVAILABLE PLACEHOLDERS =====
# Chat placeholders:
# {channel} - Channel display name
# {player} - Player name
# {displayname} - Player display name
# {message} - Chat message
# {prefix} - Player prefix (from LuckPerms)
# {suffix} - Player suffix (from LuckPerms)
# Any PlaceholderAPI placeholder is also supported

# Moderation placeholders:
# {action} - The moderation action performed
# {staff_name} - Name of the staff member
# {player_name} - Name of the punished player
# {reason} - Reason for the punishment
# {duration} - Duration of the punishment
# {server_name} - Server name
```