package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.integrations.DiscordIntegration;
import com.kelpwing.kelpylandiaplugin.moderation.commands.VanishCommand;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {
    
    private final KelpylandiaPlugin plugin;
    
    public AdvancementListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @SuppressWarnings("deprecation") // getKey() deprecated since 1.21.4, but still functional
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        
        // Skip non-display advancements (recipes, etc.)
        if (!VersionHelper.hasAdvancementDisplay(advancement)) {
            return;
        }
        
        // If the player is vanished, suppress the message and revoke the advancement
        VanishCommand vc = plugin.getVanishCommand();
        if (vc != null && vc.isVanished(player)) {
            // Try Paper API to suppress the chat message directly
            try {
                // Paper has event.message(Component) — call via reflection
                // Passing null suppresses the advancement chat broadcast
                java.lang.reflect.Method msgMethod = event.getClass().getMethod("message",
                        Class.forName("net.kyori.adventure.text.Component"));
                msgMethod.invoke(event, (Object) null);
            } catch (Exception ignored) {
                // Not on Paper or method unavailable — fall through to revoke approach
            }

            // Revoke the advancement so it can be earned again later and to suppress
            // the vanilla broadcast on Spigot (revoking criteria prevents the message)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                for (String criteria : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criteria);
                }
            }, 1L);
            
            return;
        }
        
        // Send advancement to Discord
        DiscordIntegration discord = plugin.getDiscordIntegration();
        if (discord != null && discord.isEnabled()) {
            String advancementTitle = VersionHelper.getAdvancementTitle(advancement);
            String advancementDescription = VersionHelper.getAdvancementDescription(advancement);
            
            if (advancementTitle != null) {
                // Send the advancement announcement to Discord
                discord.sendAdvancementMessage(player, advancementTitle, advancementDescription);
            }
        }
    }
}
