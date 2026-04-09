package com.kelpwing.kelpylandiaplugin.listeners;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.integrations.DiscordIntegration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConsoleListener extends Handler implements Listener {

    private final KelpylandiaPlugin plugin;

    /**
     * Records that arrived before DiscordIntegration was ready.
     * Flushed and cleared the first time Discord becomes available.
     */
    private final List<LogRecord> pendingRecords = new ArrayList<>();
    private boolean discordReady = false;

    public ConsoleListener(KelpylandiaPlugin plugin) {
        this.plugin = plugin;

        // Attach to the root logger so ALL server output is captured.
        // The "Minecraft" named logger only receives plugin-specific messages;
        // the root logger ("") gets everything (vanilla, Paper internals, etc.).
        Logger.getLogger("").addHandler(this);
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || record.getMessage() == null) return;

        synchronized (pendingRecords) {
            if (!discordReady) {
                // Discord not up yet — check if it is now
                DiscordIntegration discord = plugin.getDiscordIntegration();
                if (discord == null || !discord.isEnabled()) {
                    // Still not ready; buffer this record (cap at 500 to avoid unbounded growth)
                    if (pendingRecords.size() < 500) {
                        pendingRecords.add(record);
                    }
                    return;
                }

                // Discord just became ready — flush buffered records first
                discordReady = true;
                for (LogRecord pending : pendingRecords) {
                    relay(discord, pending.getMessage(), pending.getLevel().getName());
                }
                pendingRecords.clear();
            }
        }

        DiscordIntegration discord = plugin.getDiscordIntegration();
        if (discord != null && discord.isEnabled()) {
            relay(discord, record.getMessage(), record.getLevel().getName());
        }
    }

    private void relay(DiscordIntegration discord, String message, String level) {
        if (shouldLogMessage(message)) {
            discord.sendConsoleMessage(message, level);
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        DiscordIntegration discord = plugin.getDiscordIntegration();
        if (discord != null && discord.isEnabled()) {
            discord.sendConsoleMessage("Executed command: /" + event.getCommand(), "INFO");
        }
    }

    private boolean shouldLogMessage(String message) {
        // Don't log our own Discord-relay messages to prevent feedback loops
        if (message.contains("[Discord]") || message.contains("Discord Integration")
                || message.contains("Failed to send console message")
                || message.contains("Failed to edit console message")) {
            return false;
        }

        // Filter out some common high-frequency spam
        String[] spamKeywords = {"Can't keep up!", "Is the server overloaded?", "KeepAlive", "ping"};
        for (String keyword : spamKeywords) {
            if (message.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {
        Logger.getLogger("").removeHandler(this);
    }
}
