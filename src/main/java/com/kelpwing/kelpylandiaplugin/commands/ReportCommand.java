package com.kelpwing.kelpylandiaplugin.commands;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * /report <player|bug> <reason>
 * Players can report other players or bugs. The report is:
 *   1. Sent to online staff (permission: kelpylandia.report.notify)
 *   2. Forwarded to a Discord channel via webhook (report.webhook-url config)
 */
public class ReportCommand implements CommandExecutor, TabCompleter {

    private final KelpylandiaPlugin plugin;

    public ReportCommand(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /report.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /report <player|bug> <reason>");
            return true;
        }

        Player reporter = (Player) sender;
        String typeOrTarget = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        boolean isBugReport = typeOrTarget.equalsIgnoreCase("bug");
        String reportType;
        String targetName;

        if (isBugReport) {
            reportType = "Bug Report";
            targetName = "N/A";
        } else {
            reportType = "Player Report";
            targetName = typeOrTarget;
            // Validate target is a real player (online or offline)
            Player onlineTarget = Bukkit.getPlayerExact(typeOrTarget);
            if (onlineTarget != null) {
                targetName = onlineTarget.getName(); // normalise capitalisation
            }
            // Prevent self-reports
            if (targetName.equalsIgnoreCase(reporter.getName())) {
                reporter.sendMessage(ChatColor.RED + "You cannot report yourself.");
                return true;
            }
        }

        // Notify the reporter
        reporter.sendMessage(ChatColor.GREEN + "Your report has been submitted. Thank you!");

        // Notify online staff
        String staffMessage = ChatColor.RED + "[REPORT] " + ChatColor.GOLD + reportType
                + ChatColor.RED + " from " + ChatColor.WHITE + reporter.getName()
                + (isBugReport ? "" : ChatColor.RED + " against " + ChatColor.WHITE + targetName)
                + ChatColor.RED + ": " + ChatColor.GRAY + reason;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("qol.report.notify")) {
                online.sendMessage(staffMessage);
            }
        }
        Bukkit.getConsoleSender().sendMessage(staffMessage);

        // Send to Discord webhook
        String webhookUrl = plugin.getConfig().getString("report.webhook-url", "");
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.equals("your-webhook-url-here")) {
            sendDiscordReport(webhookUrl, reporter.getName(), reportType, targetName, reason, isBugReport);
        }

        return true;
    }

    private void sendDiscordReport(String webhookUrl, String reporter, String reportType,
                                   String target, String reason, boolean isBugReport) {
        CompletableFuture.runAsync(() -> {
            try {
                String color = isBugReport ? "16776960" : "16711680"; // yellow for bug, red for player

                String description = "**Reporter:** " + escapeJson(reporter) + "\\n"
                        + (isBugReport ? "" : "**Reported Player:** " + escapeJson(target) + "\\n")
                        + "**Reason:** " + escapeJson(reason) + "\\n"
                        + "**Time:** <t:" + Instant.now().getEpochSecond() + ":F>";

                String payload = "{"
                        + "\"username\": \"Report System\","
                        + "\"embeds\": [{"
                        + "\"title\": \"" + escapeJson(reportType) + "\","
                        + "\"description\": \"" + description + "\","
                        + "\"color\": " + color
                        + "}]"
                        + "}";

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 200 && responseCode != 204) {
                    plugin.getLogger().warning("[Report] Discord webhook returned code " + responseCode);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[Report] Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            suggestions.add("bug");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) suggestions.add(p.getName());
            }
            if (!"bug".startsWith(prefix)) suggestions.remove("bug");
            else suggestions.add(0, "bug"); // keep bug at top
            // Deduplicate
            List<String> result = new ArrayList<>();
            for (String s : suggestions) {
                if (s.toLowerCase().startsWith(prefix) && !result.contains(s)) result.add(s);
            }
            Collections.sort(result);
            return result;
        }
        return Collections.emptyList();
    }
}
