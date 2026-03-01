package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.UUID;
import java.util.Optional;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LuckPermsManager {
    private final KelpylandiaPlugin plugin;
    private final LuckPerms luckPerms;

    public LuckPermsManager(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
    }

    public String getPrefix(String username) {
        try {
            return luckPerms.getUserManager()
                .lookupUniqueId(username)
                .thenApplyAsync((optionalUUID) -> {
                    if (optionalUUID != null) {
                        return luckPerms.getUserManager().loadUser(optionalUUID)
                            .thenApplyAsync(user -> 
                                user.getCachedData().getMetaData().getPrefix()
                            ).join();
                    }
                    return "";
                }).join();
        } catch (Exception e) {
            return "";
        }
    }

    public String getPrimaryGroup(String username) {
        try {
            return luckPerms.getUserManager()
                .lookupUniqueId(username)
                .thenApplyAsync(optionalUUID -> {
                    if (optionalUUID != null) {
                        return luckPerms.getUserManager().loadUser(optionalUUID)
                            .thenApplyAsync(User::getPrimaryGroup)
                            .join();
                    }
                    return "default";
                }).join();
        } catch (Exception e) {
            return "default";
        }
    }

    public boolean hasPermission(String username, String permission) {
        try {
            return luckPerms.getUserManager()
                .lookupUniqueId(username)
                .thenApplyAsync(optionalUUID -> {
                    if (optionalUUID != null) {
                        return luckPerms.getUserManager().loadUser(optionalUUID)
                            .thenApplyAsync(user -> 
                                user.getCachedData().getPermissionData().checkPermission(permission).asBoolean()
                            ).join();
                    }
                    return false;
                }).join();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasPermissionSync(String username, String permission) {
        try {
            return hasPermission(username, permission);
        } catch (Exception e) {
            return false;
        }
    }

    public CompletableFuture<Boolean> isStaff(String username) {
        return luckPerms.getUserManager()
            .lookupUniqueId(username)
            .thenApplyAsync(optionalUUID -> {
                if (optionalUUID != null) {
                    return luckPerms.getUserManager().loadUser(optionalUUID)
                        .thenApplyAsync(user -> 
                            user.getNodes(NodeType.INHERITANCE).stream()
                                .map(InheritanceNode::getGroupName)
                                .anyMatch(group -> group.equalsIgnoreCase("staff") || 
                                                 group.equalsIgnoreCase("admin") || 
                                                 group.equalsIgnoreCase("mod"))
                        ).join();
                }
                return false;
            });
    }
}
