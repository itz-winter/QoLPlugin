# QoLPlugin

The one-and-done all-purpose plugin for any and all uses!

## Overview

QoLPlugin is a highly customizable, all-in-one Minecraft server plugin designed for small to mid-sized SMP servers. Originally built for my own community, it combines advanced chat, moderation, player homes, teleportation, and more—making it a great alternative to EssentialsX and similar plugins. Built on the Spigot/Bukkit API, it supports **Minecraft 1.16 – 1.21.11** (and likely newer).

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

## Getting Started

1. Drop the plugin JAR into your `plugins` folder.
2. Start your server to generate the config.
3. Edit `config.yml` to enable/disable features as you like.
4. Reload or restart your server.

## Recommendations

- For advanced Discord integration, use [DiscordSRV](https://modrinth.com/plugin/discordsrv).
- ~~For sitting, use [Gsit](https://www.spigotmc.org/resources/gsit-modern-sit-seat-and-chair-lay-and-crawl-plugin-1-16-1-21-11.62325/) until the built-in system is improved.~~
  - **I am not fixing the sit system. It has been removed completely in the latest versions.**

---

*Made with love, cringe, and a lot of config options.*

\- Kelp
> [GitHub](https://github.com/itz-winter/) • [Discord](https://discord.com/users/851136131071344681) • [Youtube](https://www.youtube.com/@kelpwing)

---

# Config Files

## config.yml

```yaml
# QoLPlugin Configuration File
# Combined chat and moderation system for Kelpylandia
# (Formerly KelpylandiaPlugin — old folder is auto-migrated)

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
    discord-channel: '123456789012345678'  # Set to your Discord channel ID for global chat
    default: true
    alias: "g"
    
  local:
    display-name: "Local"
    format: "&e[L]&r {prefix}{displayname}&r: {message}"
    permission: "kelpylandia.channel.local"
    proximity: true
    proximity-distance: 50.0
    discord-enabled: false
    discord-channel: ''
    default: false
    alias: "l"
    
  admin:
    display-name: "Admin"
    format: "&9[A]&r {prefix}{displayname}&r: {message}"
    permission: "kelpylandia.channel.admin"
    proximity: false
    proximity-distance: 0.0
    discord-enabled: true
    discord-channel: '123456789012345678'  # Set to your Discord channel ID for admin chat
    default: false
    alias: "at"

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

    # When DiscordSRV is installed, skip this plugin's own join/leave/advancement
    # Discord messages to avoid duplicates. Set to false if you want both.
    skip-if-discordsrv: true
    
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
  cooldown: 1
  # Invulnerability duration in seconds after teleporting (0 to disable)
  invulnerability: 4
  # How long a TPA request lasts before expiring in seconds
  request-timeout: 120

# ===== BACK COMMANDS =====
# /back returns to your previous location before teleporting
# /dback returns to your last death location
back:
  enabled: true
  cooldown: 1

# ===== SPAWN COMMAND =====
# /spawn teleports to the main world's spawn point
spawn:
  enabled: true

# ===== GAMEMODE COMMANDS =====
# Shortcut gamemode commands: /gm, /gmc, /gms, /gma, /gmsp
# Set enabled to false to disable all gamemode commands (avoids conflicts with other plugins)
gamemode:
  enabled: true

# ===== NICKNAME COMMAND =====
# /nick [nickname] - Set or reset your display name
# Set enabled to false to disable the nick command (avoids conflicts with other plugins)
nickname:
  enabled: true

# ===== ENCHANT COMMAND =====
# /enchant <enchantment> [level] - Enchant the item in your hand
# Set enabled to false to disable the enchant command (avoids conflicts with other plugins)
enchant:
  enabled: true

# ===== ENCHANT OVERRIDES =====
# Allows normally conflicting enchantments to be combined in an anvil.
# Enabled combos: Infinity+Mending, Sharpness+Smite+Bane (any combo),
#   sword enchants on tridents, Multishot+Piercing.
# Hard-blocked: Fortune+Silk Touch.
# Makes the game more enjoyable by allowing fun enchantment combinations that are normally disallowed, while still preventing truly overpowered combos like Fortune+Silk Touch.

enchant-overrides:
  enabled: true

# ===== AFK SYSTEM =====
# /afk - Toggle AFK status. Players are auto-marked AFK after the idle time.
# Set enabled to false to disable the AFK system
afk:
  enabled: true
  # Seconds of inactivity before a player is automatically marked AFK (0 to disable auto-AFK)
  auto-afk-seconds: 300

# ===== MESSAGING SYSTEM =====
# /w, /r, /wt - Private messaging, reply, whisper target
# Set enabled to false to disable all messaging commands
messaging:
  enabled: true
  # Format shown to the SENDER of a private message
  # Placeholders: {sender} = sender display name, {receiver} = receiver display name, {message} = the message
  format-sender: "&c[Me &c-> &r{receiver}&c]:&7 {message}"
  # Format shown to the RECEIVER of a private message
  format-receiver: "&c[&r{sender}&c -> Me]:&7 {message}"

# ===== SUICIDE COMMAND =====
# /suicide - Kill yourself
# Set enabled to false to disable the suicide command
suicide:
  enabled: true

# ===== SKULL COMMAND =====
# /skull [username] - Get a player's head as an item
# Set enabled to false to disable the skull command
skull:
  enabled: true

# ===== REPAIR COMMAND =====
# /repair [hand|helmet|chestplate|pants|boots|offhand|all] [user] - Repair item durability
# Set enabled to false to disable the repair command
repair:
  enabled: true

# ===== SUDO COMMAND =====
# /sudo <user> <command or message> - Force a player to run a command or say something
# Set enabled to false to disable the sudo command
sudo:
  enabled: true

# ===== RUNAT COMMAND =====
# /runat <player|Console> <command> - Run a command as another player with full permission bypass.
# The target is temporarily granted all permissions so the command always succeeds.
# Use "Console" as the target to execute commands as the server console.
# Console can always use /runat. Players need the kelpylandia.runat permission (unset by default).
# Alias: /forcecmd
runat:
  enabled: true

# ===== STATE PERSISTENCE =====
# Toggle states (vanish, socialspy, commandspy, god, fly) are saved to disk
# immediately when toggled and restored on rejoin. Enabled by default.
# Set to false to disable persistence for a specific toggle.
state-persistence:
  enabled: true
  vanish: true
  socialspy: true
  commandspy: true
  god: true
  fly: true

# ===== SPY COMMANDS =====
# /ss /socialspy - Toggle social spy (see all private messages and channel chat)
# /cs /commandspy - Toggle command spy (see all commands other players run)
# Set enabled to false to disable both spy commands
spy:
  enabled: true

# ===== FLY COMMAND =====
# /fly [user] - Toggle flight mode (use * to target all players)
# Set enabled to false to disable the fly command
fly:
  enabled: true

# ===== GOD COMMAND =====
# /god [user] - Toggle god mode / invincibility (use * to target all players)
# Set enabled to false to disable the god command
god:
  enabled: true

# ===== HEAL COMMAND =====
# /heal [user] - Heal a player to full health (use * to target all players)
# Set enabled to false to disable the heal command
heal:
  enabled: true

# ===== STARVE COMMAND =====
# /starve [user] - Set a player's hunger to zero (use * to target all players)
# Set enabled to false to disable the starve command
starve:
  enabled: true

# ===== FEED COMMAND =====
# /feed [user] - Fill a player's food bar (use * to target all players)
# Set enabled to false to disable the feed command
feed:
  enabled: true

# ===== FLY/WALK SPEED COMMANDS =====
# /flyspeed <speed> [user] - Set fly speed (0-10, default 1)
# /walkspeed <speed> [user] - Set walk speed (0-10, default 2)
# Set enabled to false to disable both speed commands
flyspeed:
  enabled: true

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
  # Allow players with kelpylandia.anvil.color to use & color codes in anvil renames
  anvil-color: true

# ===== TRASH COMMAND =====
# /trash - Opens a disposable inventory. Items left inside are permanently destroyed on close.
# Set enabled to false to disable the trash command
trash:
  enabled: true

# ===== LORE COMMAND =====
# /lore <set|add|clear|remove|insert> [args] - Edit the lore on the held item
# Supports & color codes. Use \n for newlines in /lore set.
# Set enabled to false to disable the lore command
lore:
  enabled: true

# ===== HAT COMMAND =====
# /hat - Place the item in your hand on your head as a hat
# Set enabled to false to disable the hat command
hat:
  enabled: true

# Auto-broadcast system
# Messages from broadcasts.yml are sent to all players at random intervals
auto-broadcasts:
  enabled: true

# ===== KITS SYSTEM =====
# /kit - Claimable item kits with cooldowns
# Kit definitions are stored in kits.yml (create/edit in-game or in the file)
kits:
  enabled: true

# ===== WARP SYSTEM =====
# /warp, /warps, /setwarp, /delwarp - Server warp points
warps:
  enabled: true

# ===== RANDOM TELEPORT =====
# /rtp - Teleport to a random location in the world
rtp:
  enabled: true
  # Maximum distance from spawn for random teleport (in blocks)
  radius: 2000
  # Cooldown in seconds between uses (0 = no cooldown)
  cooldown: 0
  # Allow teleporting to underground locations
  allow-underground: false
  # Allow teleporting into water
  allow-water: false
  # Minimum Y level for surface teleports (ignored when allow-underground is true)
  min-y: 50

# ===== SEEN COMMAND =====
# /seen <player> - Check when a player was last online
seen:
  enabled: true

# ===== JAIL SYSTEM =====
# /jail <player> <duration> [reason] and /release <player> [reason]
# Jailed players are teleported to spawn and completely restricted
jail:
  enabled: true

# ===== FREEZE SYSTEM =====
# /freeze and /unfreeze - Freeze players in place (persists across relog)
freeze:
  enabled: true

# ===== STUCK COMMAND =====
# /stuck - Helps players escape the nether roof or void
# Disabled by default as it could be exploited
stuck:
  enabled: false

# ===== NOCLIP COMMAND =====
# /noclip [player] - Toggle spectator mode while flying to clip through blocks
noclip:
  enabled: true

# ===== SMITE COMMAND =====
# /smite <player> - Strike lightning on a player with a random broadcast message
# Use {player} for the target's display name and {sender} for the staff member's name
smite:
  enabled: true
  messages:
    - "&e{player} &7has been struck by the wrath of the gods!"
    - "&e{player} &7was smote by &e{sender}&7!"
    - "&e{player} &7was struck by divine lightning!"
    - "&e{player} &7displeased the heavens."
    - "&7The sky opens up and lightning strikes &e{player}&7!"
    - "&e{player} &7felt the power of Zeus."
    - "&7A thunderbolt finds its mark on &e{player}&7!"

# ===== REPORT COMMAND =====
# /report <player|bug> <reason> - Submit a report to staff and Discord
report:
  enabled: true
  # Discord webhook URL to send reports to. Leave blank or set to "your-webhook-url-here" to disable.
  webhook-url: "your-webhook-url-here"

# ===== RECIPE COMMAND =====
# /recipe <item> [page] - View the crafting recipe for an item in a GUI
# Hold an item and type /recipe with no arguments to look up what you're holding
recipe:
  enabled: true

# ===== RULES COMMAND =====
# /rules - Display server rules to a player
# Use & for color codes. {number} is replaced with the rule number (1, 2, 3, ...)
rules:
  enabled: true
  header:
    - "&8&m----------------------------------------"
    - "&6&l         Server Rules"
    - "&8&m----------------------------------------"
  lines:
    - "&e{number}. &fBe respectful to all players."
    - "&e{number}. &fNo griefing or stealing."
    - "&e{number}. &fNo hacking, cheating, or exploiting."
    - "&e{number}. &fNo spamming or advertising."
    - "&e{number}. &fUse common sense."
  footer:
    - "&8&m----------------------------------------"
    - "&7Breaking the rules may result in a punishment."
    - "&8&m----------------------------------------"

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

## broadcasts.yml

```yaml
# ===================================================
# Auto-Broadcast Configuration
# Messages are broadcasted globally at random intervals.
# ===================================================

# Minimum interval between broadcasts (in seconds)
min-interval: 60

# Maximum interval between broadcasts (in seconds)
max-interval: 120

# Prefix prepended to every broadcast message
# Set to "" to disable the prefix
prefix: "&6[Broadcast] &r"

# Whether to randomize message order (true) or cycle sequentially (false)
random-order: true

# Messages to broadcast
# Supports color codes (&1-&f, &l, &o, &n, &m, &k, &r) and PlaceholderAPI placeholders
messages:
  - "&eWelcome to the server! Have fun and play fair."
  - "&eJoin our Discord for updates and community events!"
  - "&eRemember to set your homes with &b/sethome&e!"
  - "&eNeed help? Ask a staff member or use &b/help&e."
  - "&eVote for the server to earn rewards!"
  - "&eRespect other players and follow the rules."
  - "&eExplore the world, build amazing things, and make friends!"
  - "&eCheck out our website for more information!"
  - "&eYou seeing this message means that the owner was too lazy to actually configure the broadcast system!"
# Remove the last line lmfao
```

## deathmessages.yml

```yaml
# ===== CUSTOM DEATH MESSAGES =====
# These messages are used when a player uses /suicide.
# A random message is picked from this list each time.
#
# Placeholders:
#   {player}       - The player's display name
#   {player_name}  - The player's raw username
#   {world}        - The world the player died in
#
# PlaceholderAPI placeholders are also supported if PlaceholderAPI is installed.
# Example: %player_health%, %player_level%, etc.

messages:
  - "{player_name} exploded"
  - "{player_name} tried to divide by 0"
  - "{player_name} forgot about gravity and flew away"
  - "{player_name} tripped on a blade of grass"
  - "{player_name} looked at a creeper the wrong way"
  - "{player_name} didn't survive the vibe check"
  - "{player_name} had a skill issue"
  - "{player_name} was not the imposter"
  - "{player_name} ragequit life"
  - "{player_name} fell into the void of their own making"
  - "{player_name} tried to respawn in real life"
  - "{player_name} forgot to eat"
  - "{player_name} spontaneously combusted"
  - "{player_name} thought fall damage was a myth"
  - "{player_name} uninstalled themselves"
```

## kits.yml

```yaml
# ═══════════════════════════════════════════════════
# KelpylandiaPlugin — Kits Configuration
# ═══════════════════════════════════════════════════
# Each kit is defined under the "kits" section.
#
# Kit properties:
#   cooldown:           Seconds between claims. -1 = no cooldown, 0+ = seconds
#   one-time:           If true, kit can only ever be claimed once (default: false)
#   give-on-first-join: If true, automatically given to new players (default: false)
#   permission:         Permission node required (null = no permission)
#   icon:               Material name for /kit list display
#   description:        Short description shown in /kit list
#   items:              List of serialised ItemStacks (managed automatically
#                       when you /kit create or /kit edit in-game)
#
# Cooldown data is stored under "cooldowns" and managed automatically.
# ═══════════════════════════════════════════════════

kits:
  starter:
    cooldown: 0
    one-time: true
    give-on-first-join: true
    permission: "kelpylandia.kit.starter"
    icon: "CHEST"
    description: "Basic starting gear"
    items:
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: STONE_SWORD
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: STONE_PICKAXE
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: STONE_AXE
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: BREAD
        amount: 16
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: TORCH
        amount: 32
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: CRAFTING_TABLE

  tools:
    cooldown: 86400
    one-time: false
    give-on-first-join: false
    permission: "kelpylandia.kit.tools"
    icon: "IRON_PICKAXE"
    description: "Iron tool set (24h cooldown)"
    items:
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: IRON_PICKAXE
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: IRON_AXE
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: IRON_SHOVEL
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: IRON_SWORD
      - ==: org.bukkit.inventory.ItemStack
        v: 2230
        type: SHIELD

# Player cooldown data (managed automatically — do not edit manually)
# cooldowns:
#   <uuid>:
#     <kitName>: <epoch-millis>

```
