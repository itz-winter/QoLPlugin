package com.kelpwing.kelpylandiaplugin.integrations;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.chat.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Optional integration for InteractiveChat.
 * <p>
 * When InteractiveChat is present, this listener runs at {@link EventPriority#MONITOR}
 * (after IC has finished all its Bukkit-level processing) and sends the processed
 * message text to Discord instead of the raw message sent by {@code ChatListener}
 * at {@code HIGHEST} priority.
 * <p>
 * No InteractiveChat classes are imported — the integration is purely runtime-checked,
 * so the plugin loads normally even when InteractiveChat is absent.
 */
public class InteractiveChatIntegration implements Listener {

    /**
     * InteractiveChat appends a sender-tracking tag to messages when
     * {@code UseAccurateSenderParser} is enabled in its config.
     * Pattern matches {@code <chat=UUID:escapedContent:>} including color-code noise.
     * See {@code ProcessAccurateSender.COLOR_IGNORE_PATTERN} in IC's source.
     */
    private static final Pattern IC_CHAT_TAG_PATTERN = Pattern.compile(
            "(?:(?:§.)*<(?:§.)*c(?:§.)*h(?:§.)*a(?:§.)*t(?:§.)*=" +
            "(?:(?:§.)*[0-9a-f]){8}(?:§.)*-(?:(?:§.)*[0-9a-f]){4}(?:§.)*-" +
            "(?:(?:§.)*[0-9a-f]){4}(?:§.)*-(?:(?:§.)*[0-9a-f]){4}(?:§.)*-" +
            "(?:(?:§.)*[0-9a-f]){12}:.*?(?:§.)*>)"
    );

    /**
     * Fallback: IC may also append a compact base-64-ish UUID identifier
     * ({@code Registry.ID_PATTERN}) in some configurations.
     */
    private static final Pattern IC_ID_TAG_PATTERN = Pattern.compile(
            "<(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})>"
    );

    private final KelpylandiaPlugin plugin;
    private final boolean icPresent;
    private final boolean icAddonPresent;

    public InteractiveChatIntegration(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.icPresent = Bukkit.getPluginManager().isPluginEnabled("InteractiveChat");
        this.icAddonPresent = Bukkit.getPluginManager().isPluginEnabled("InteractiveChatDiscordSrvAddon");

        if (icPresent) {
            plugin.getLogger().info("InteractiveChat detected — Discord relay will use processed messages.");
        }
        if (icAddonPresent) {
            plugin.getLogger().info("InteractiveChat-DiscordSRV-Addon detected.");
        }
    }

    /**
     * Returns {@code true} when InteractiveChat is installed and the config
     * option {@code discord.interactivechat.use-processed-message} is enabled.
     */
    public boolean isEnabled() {
        return icPresent
                && plugin.getConfig().getBoolean("discord.interactivechat.use-processed-message", true);
    }

    /**
     * Returns whether InteractiveChatDiscordSrvAddon is present.
     * Informational only; we do not delegate to it since this plugin uses its
     * own JDA bot rather than DiscordSRV.
     */
    public boolean isAddonPresent() {
        return icAddonPresent;
    }

    /**
     * Fires at MONITOR — after InteractiveChat has finished all Bukkit-level
     * processing (mention resolution, color-code translation, sender tagging).
     * We capture the processed message, strip IC's internal tracking tags,
     * and forward it to Discord.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        DiscordIntegration discord = plugin.getDiscordIntegration();
        if (discord == null || !discord.isEnabled()) return;

        Player player = event.getPlayer();

        // Get the message as IC left it at MONITOR — mentions resolved,
        // color codes translated, tracking tags appended.
        String processed = event.getMessage();

        // Strip IC's sender-tracking tags so they don't appear in Discord.
        processed = IC_CHAT_TAG_PATTERN.matcher(processed).replaceAll("");
        processed = IC_ID_TAG_PATTERN.matcher(processed).replaceAll("");
        processed = processed.trim();

        // If the entire message was IC interactive content (e.g. item display, [item] keyword)
        // and nothing readable is left, substitute a human-friendly placeholder so Discord
        // doesn't receive an empty payload (which causes a 400 error).
        if (processed.isEmpty()) {
            String raw = event.getMessage();
            // Detect common IC interactive keywords that expand to components
            if (raw.contains("[item]") || raw.contains("[Item]")) {
                processed = "\uD83D\uDFEB [showed an item]";
            } else if (raw.contains("[block]") || raw.contains("[Block]")) {
                processed = "\uD83E\uDDF1 [showed a block]";
            } else if (raw.contains("[inv]") || raw.contains("[Inv]")
                    || raw.contains("[inventory]") || raw.contains("[Inventory]")) {
                processed = "\uD83C\uDF92 [showed their inventory]";
            } else if (raw.contains("[enderchest]") || raw.contains("[EnderChest]")) {
                processed = "\uD83D\uDC9C [showed their ender chest]";
            } else {
                // Nothing useful left — skip sending to Discord entirely
                return;
            }
        }

        // Look up the player's channel the same way ChatListener does.
        Channel playerChannel = plugin.getChannelManager().getPlayerChannel(player);
        if (playerChannel == null) {
            playerChannel = plugin.getChannelManager().getDefaultChannel();
        }
        if (playerChannel == null) return;

        if (!playerChannel.isDiscordEnabled()) return;

        try {
            discord.sendChatMessage(player, processed, playerChannel.getDiscordChannel());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "[InteractiveChatIntegration] Failed to relay chat to Discord", e);
        }
    }
}
