package com.kelpwing.kelpylandiaplugin.integrations;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public class LuckPermsIntegration {
    
    private LuckPerms luckPerms;
    
    public LuckPermsIntegration() {
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            this.luckPerms = null;
        }
    }
    
    public boolean isEnabled() {
        return luckPerms != null;
    }
    
    public String getPrefix(Player player) {
        if (!isEnabled()) return "";
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "";
            
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String prefix = metaData.getPrefix();
            return prefix != null ? prefix : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    public String getSuffix(Player player) {
        if (!isEnabled()) return "";
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "";
            
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String suffix = metaData.getSuffix();
            return suffix != null ? suffix : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    public String getPrimaryGroup(Player player) {
        if (!isEnabled()) return "default";
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "default";
            
            return user.getPrimaryGroup();
        } catch (Exception e) {
            return "default";
        }
    }
    
    public boolean hasPermission(Player player, String permission) {
        if (!isEnabled()) return player.hasPermission(permission);
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return player.hasPermission(permission);
            
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            return player.hasPermission(permission);
        }
    }
}
