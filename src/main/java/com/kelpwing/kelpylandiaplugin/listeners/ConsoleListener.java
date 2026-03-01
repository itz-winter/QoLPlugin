package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConsoleListener extends Handler implements Listener {
    
    private final KelpylandiaPlugin plugin;
    
    public ConsoleListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        
        plugin.getLogger().info("[CONSOLE DEBUG] ConsoleListener initialized");
        
        // Add this handler to the root logger to intercept console messages
        Logger.getLogger("Minecraft").addHandler(this);
        plugin.getLogger().info("[CONSOLE DEBUG] Handler added to Minecraft logger");
    }
    
    @Override
    public void publish(LogRecord record) {
        if (record == null || record.getMessage() == null) return;
        
        if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            String message = record.getMessage();
            String level = record.getLevel().getName();
            
            // Filter out some spam messages and format for Discord
            if (shouldLogMessage(message)) {
                plugin.getDiscordIntegration().sendConsoleMessage(message, level);
            }
        }
    }
    
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (plugin.getDiscordIntegration() != null && plugin.getDiscordIntegration().isEnabled()) {
            String command = event.getCommand();
            plugin.getDiscordIntegration().sendConsoleMessage("Executed command: /" + command, "INFO");
        }
    }
    
    private boolean shouldLogMessage(String message) {
        // Don't log our own Discord messages to prevent loops
        if (message.contains("Discord") && message.contains("KelpylandiaPlugin")) {
            return false;
        }
        
        // Don't log debug messages
        if (message.contains("[DEBUG]") || message.contains("DEBUG")) {
            return false;
        }
        
        // Filter out some common spam
        String[] spamKeywords = {"Can't keep up!", "Is the server overloaded?", "KeepAlive", "ping"};
        for (String keyword : spamKeywords) {
            if (message.contains(keyword)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void flush() {
        // Not needed for our implementation
    }
    
    @Override
    public void close() throws SecurityException {
        // Clean up when plugin is disabled
        Logger.getLogger("Minecraft").removeHandler(this);
    }
}
