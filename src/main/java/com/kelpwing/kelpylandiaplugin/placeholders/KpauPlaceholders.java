package com.kelpwing.kelpylandiaplugin.placeholders;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;

public class KpauPlaceholders extends PlaceholderExpansion {
    private final KelpylandiaPlugin plugin;

    public KpauPlaceholders(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return "kpaumod";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return "";
        }

        String playerName = player.getName();
        if (playerName == null) {
            return "";
        }

        // %kpaumod_warnings%
        if (identifier.equals("warnings")) {
            return String.valueOf(plugin.getFileManager().getActiveWarnings(playerName).size());
        }

        // %kpaumod_total_punishments%
        if (identifier.equals("total_punishments")) {
            List<Map<String, String>> history = plugin.getFileManager().getPlayerHistory(playerName);
            return String.valueOf(history.size());
        }

        // %kpaumod_bans%
        if (identifier.equals("bans")) {
            List<Map<String, String>> history = plugin.getFileManager().getPlayerHistory(playerName);
            long bans = history.stream().filter(record -> 
                "BAN".equals(record.get("action")) || "IPBAN".equals(record.get("action"))
            ).count();
            return String.valueOf(bans);
        }

        // %kpaumod_kicks%
        if (identifier.equals("kicks")) {
            List<Map<String, String>> history = plugin.getFileManager().getPlayerHistory(playerName);
            long kicks = history.stream().filter(record -> 
                "KICK".equals(record.get("action"))
            ).count();
            return String.valueOf(kicks);
        }

        // %kpaumod_mutes%
        if (identifier.equals("mutes")) {
            List<Map<String, String>> history = plugin.getFileManager().getPlayerHistory(playerName);
            long mutes = history.stream().filter(record -> 
                "MUTE".equals(record.get("action"))
            ).count();
            return String.valueOf(mutes);
        }

        // %kpaumod_is_muted%
        if (identifier.equals("is_muted")) {
            return plugin.getMutedPlayers().containsKey(player.getUniqueId()) ? "true" : "false";
        }

        // %kpaumod_mute_time_left%
        if (identifier.equals("mute_time_left")) {
            if (plugin.getMutedPlayers().containsKey(player.getUniqueId())) {
                long timeLeft = plugin.getMutedPlayers().get(player.getUniqueId()) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    return formatTime(timeLeft);
                }
            }
            return "Not muted";
        }

        return null;
    }

    private String formatTime(long timeLeft) {
        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
