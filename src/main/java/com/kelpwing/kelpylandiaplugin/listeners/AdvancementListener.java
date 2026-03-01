package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.integrations.DiscordIntegration;
import com.kelpwing.kelpylandiaplugin.utils.VersionHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {
    
    private final KelpylandiaPlugin plugin;
    
    public AdvancementListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @SuppressWarnings("deprecation") // getKey() deprecated since 1.21.4, but still functional
    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        
        // Only broadcast if advancement is displayed to players
        if (!VersionHelper.hasAdvancementDisplay(event.getAdvancement())) {
            return;
        }
        
        DiscordIntegration discord = plugin.getDiscordIntegration();
        if (discord != null && discord.isEnabled()) {
            String advancementTitle = VersionHelper.getAdvancementTitle(event.getAdvancement());
            String advancementDescription = VersionHelper.getAdvancementDescription(event.getAdvancement());
            
            if (advancementTitle != null) {
                // Send the advancement announcement to Discord
                discord.sendAdvancementMessage(player, advancementTitle, advancementDescription);
            }
        }
    }
}
