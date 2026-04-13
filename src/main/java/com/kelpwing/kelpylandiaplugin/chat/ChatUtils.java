package com.kelpwing.kelpylandiaplugin.chat;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    // â”€â”€ Clickable command box support â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //
    // Cherry-picked from InteractiveChat's CommandsDisplay module.
    // IC uses a configurable "Format" (e.g. "[{Command}]") to build a regex at
    // runtime.  We do the same: the format from config.yml is turned into a
    // Pattern that matches any /command wrapped in the configured brackets.
    //
    // Example: format "[{Command}]" + command "/spawn" â†’ matches "[/spawn]"
    //   and replaces it with the styled, clickable text from config "text".

    /**
     * Builds a {@link Pattern} from the configurable format string.
     * The format uses {@code {Command}} as the placeholder for the command text.
     * The command must start with {@code /} and may contain any non-bracket chars.
     *
     * <p>Cherry-picked from IC's {@code CommandsDisplay.process()} â€” IC builds
     * the regex at call-time from {@code InteractiveChat.clickableCommandsFormat}.
     * We build it once here and cache on first use.
     */
    private static volatile Pattern cachedCommandPattern = null;
    private static volatile String cachedCommandFormat = null;

    /**
     * Returns (building/caching if needed) a Pattern that matches the clickable
     * command format configured in config.yml.
     */
    public static Pattern getCommandPattern(KelpylandiaPlugin plugin) {
        String format = plugin.getConfig().getString(
                "clickable-commands.format", "[{Command}]");
        // Rebuild only if the config format changed (hot-reload support)
        if (!format.equals(cachedCommandFormat)) {
            cachedCommandPattern = buildCommandPattern(format);
            cachedCommandFormat  = format;
        }
        return cachedCommandPattern;
    }

    /**
     * Compiles a regex from the IC-style format string.
     * The command capture group matches {@code /anything-not-closing-bracket}.
     *
     * <p>e.g. format {@code "[{Command}]"} â†’
     * {@code \[(/[^\[\]]*)\]} â€” matches {@code [/spawn]} and captures {@code /spawn}.
     */
    private static Pattern buildCommandPattern(String format) {
        if (!format.contains("{Command}")) {
            // Broken config â€” fall back to a safe default
            return Pattern.compile("\\[(/[a-zA-Z0-9_.\\- ]+)]");
        }
        // Split the format on {Command}
        int idx = format.indexOf("{Command}");
        String prefix = format.substring(0, idx);           // e.g. "["
        String suffix = format.substring(idx + "{Command}".length()); // e.g. "]"

        // Collect characters that appear in prefix+suffix â€” these are the
        // "closing" chars the command text must not contain (IC's escapeChars set).
        String escapedPrefix = Pattern.quote(prefix);
        String escapedSuffix = Pattern.quote(suffix);

        // Build a character class of all unique chars in prefix+suffix to
        // use as negation inside the command capture group.
        // Each special regex char-class metacharacter is individually escaped
        // (Pattern.quote wraps in \Q..\E which is invalid inside [^...]).
        String combined = prefix + suffix;
        StringBuilder negated = new StringBuilder();
        for (char c : combined.toCharArray()) {
            // Escape chars that are special inside a character class
            if ("\\[]^-".indexOf(c) >= 0) negated.append('\\');
            negated.append(c);
        }
        // Command group: /  followed by zero-or-more chars not in the format delimiters
        String cmdGroup = "(/(?:[^" + negated + "]*))";

        return Pattern.compile(escapedPrefix + cmdGroup + escapedSuffix);
    }

    /**
     * Returns {@code true} when the text contains at least one command box matching
     * the pattern configured in {@code clickable-commands.format}.
     */
    public static boolean containsCommand(KelpylandiaPlugin plugin, String text) {
        if (!plugin.getConfig().getBoolean("clickable-commands.enabled", true)) return false;
        return getCommandPattern(plugin).matcher(text).find();
    }

    /** @deprecated Use {@link #containsCommand(KelpylandiaPlugin, String)} */
    @Deprecated
    public static boolean containsCommand(String text) {
        return Pattern.compile("\\[(/[a-zA-Z0-9_.\\- ]+)]").matcher(text).find();
    }

    /**
     * Converts a legacy-colour-coded string into a {@link BaseComponent} array
     * where every command box occurrence becomes a clickable component.
     *
     * <p>The display text, click action, and hover message are all read from
     * {@code clickable-commands.*} in config.yml â€” matching IC's configurable
     * approach.  Non-command portions are kept as legacy-text components.
     *
     * @param plugin     plugin instance (for config access)
     * @param legacyText the text (already colour-translated) to process
     * @return component array ready for {@code player.spigot().sendMessage()}
     */
    public static BaseComponent[] parseClickableCommands(KelpylandiaPlugin plugin, String legacyText) {
        Pattern pattern = getCommandPattern(plugin);
        String displayTemplate = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("clickable-commands.text", "&b[&e{Command}&b]"));
        String actionName = plugin.getConfig().getString(
                "clickable-commands.action", "SUGGEST_COMMAND").toUpperCase();
        String hoverRaw = plugin.getConfig().getString(
                "clickable-commands.hover-message", "&eClick to use command!");

        ClickEvent.Action action;
        try {
            action = ClickEvent.Action.valueOf(actionName);
        } catch (IllegalArgumentException ex) {
            action = ClickEvent.Action.SUGGEST_COMMAND;
        }

        Matcher matcher = pattern.matcher(legacyText);
        ComponentBuilder builder = new ComponentBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before this match
            if (matcher.start() > lastEnd) {
                builder.append(
                        TextComponent.fromLegacyText(legacyText.substring(lastEnd, matcher.start())),
                        ComponentBuilder.FormatRetention.NONE);
            }

            // The command is always captured in group 1
            String command = matcher.group(1).trim();

            // Apply the configured display text  (IC: "clickableCommandsDisplay.replace({Command}, cmd)")
            String displayText = displayTemplate.replace("{Command}", command);

            TextComponent comp = new TextComponent(TextComponent.fromLegacyText(displayText));
            comp.setClickEvent(new ClickEvent(action, command));

            if (!hoverRaw.isEmpty()) {
                String hoverText = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        hoverRaw.replace("{Command}", command));
                comp.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(hoverText))));
            }

            builder.append(comp, ComponentBuilder.FormatRetention.NONE);
            lastEnd = matcher.end();
        }

        // Trailing text
        if (lastEnd < legacyText.length()) {
            builder.append(
                    TextComponent.fromLegacyText(legacyText.substring(lastEnd)),
                    ComponentBuilder.FormatRetention.NONE);
        }

        return builder.create();
    }

    /** @deprecated Use {@link #parseClickableCommands(KelpylandiaPlugin, String)} */
    @Deprecated
    public static BaseComponent[] parseClickableCommands(String legacyText) {
        // Legacy shim â€” plain aqua boxes, no config
        Pattern pattern = Pattern.compile("\\[(/[a-zA-Z0-9_.\\- ]+)]");
        Matcher matcher = pattern.matcher(legacyText);
        ComponentBuilder builder = new ComponentBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                builder.append(TextComponent.fromLegacyText(legacyText.substring(lastEnd, matcher.start())),
                        ComponentBuilder.FormatRetention.NONE);
            }
            String command = matcher.group(1).trim();
            TextComponent comp = new TextComponent(TextComponent.fromLegacyText(
                    org.bukkit.ChatColor.AQUA + "[" + command + "]"));
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(
                            org.bukkit.ChatColor.YELLOW + "Click to use " + org.bukkit.ChatColor.WHITE + command))));
            builder.append(comp, ComponentBuilder.FormatRetention.NONE);
            lastEnd = matcher.end();
        }
        if (lastEnd < legacyText.length()) {
            builder.append(TextComponent.fromLegacyText(legacyText.substring(lastEnd)),
                    ComponentBuilder.FormatRetention.NONE);
        }
        return builder.create();
    }

    // â”€â”€ Chat formatting helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Builds the fully-formatted chat prefix (everything before the message body).
     */
    public static String formatPrefix(KelpylandiaPlugin plugin, Player player, Channel channel) {
        String format = channel.getFormat();
        format = format.replace("{prefix}", getPlayerPrefix(player));
        format = format.replace("{suffix}", getPlayerSuffix(player));
        format = format.replace("{player}", player.getName());
        format = format.replace("{displayname}", player.getDisplayName());
        format = format.replace("{channel}", channel.getDisplayName());
        format = org.bukkit.ChatColor.translateAlternateColorCodes('&', format);
        int msgIdx = format.indexOf("{message}");
        if (msgIdx >= 0) return format.substring(0, msgIdx);
        return format + ": ";
    }

    /**
     * Builds the complete chat line as a legacy-colour string.
     */
    public static String formatMessage(KelpylandiaPlugin plugin, Player player, Channel channel, String message) {
        String format = channel.getFormat();
        format = format.replace("{prefix}", getPlayerPrefix(player));
        format = format.replace("{suffix}", getPlayerSuffix(player));
        format = format.replace("{player}", player.getName());
        format = format.replace("{displayname}", player.getDisplayName());
        format = format.replace("{channel}", channel.getDisplayName());
        format = format.replace("{message}", message);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', format);
    }

    private static String getPlayerPrefix(Player player) {
        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
                if (prefix != null && !prefix.equals("%luckperms_prefix%")) return prefix;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String getPlayerSuffix(Player player) {
        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                String suffix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_suffix%");
                if (suffix != null && !suffix.equals("%luckperms_suffix%")) return suffix;
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public static boolean isPlayerMuted(Player player) {
        KelpylandiaPlugin plugin = KelpylandiaPlugin.getInstance();
        if (plugin == null) return false;
        Long muteExpiry = plugin.getMutedPlayers().get(player.getUniqueId());
        if (muteExpiry == null) return false;
        long now = System.currentTimeMillis();
        if (muteExpiry > 0 && muteExpiry <= now) {
            plugin.getMutedPlayers().remove(player.getUniqueId());
            return false;
        }
        return true;
    }
}

