package com.kelpwing.kelpylandiaplugin.utils;

import com.kelpwing.kelpylandiaplugin.KelpylandiaPlugin;
import com.kelpwing.kelpylandiaplugin.moderation.models.Punishment;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {
    private final KelpylandiaPlugin plugin;
    private String webhookUrl;

    public DiscordWebhook(KelpylandiaPlugin plugin) {
        this.plugin = plugin;
        initializeWebhook();
    }

    private void initializeWebhook() {
        String botToken = plugin.getConfigManager().getDiscordBotToken();
        String channelId = plugin.getConfigManager().getDiscordChannelId();

        if (botToken == null || botToken.isEmpty() || channelId == null || channelId.isEmpty()) {
            plugin.getLogger().warning("Discord bot token or channel ID not configured!");
            return;
        }

        // Create webhook asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                createWebhook(botToken, channelId);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create Discord webhook: " + e.getMessage());
            }
        });
    }

    private void createWebhook(String botToken, String channelId) throws IOException {
        // Create webhook URL
        String createWebhookUrl = "https://discord.com/api/v10/channels/" + channelId + "/webhooks";
        
        // Create webhook payload
        String payload = "{\n" +
            "  \"name\": \"kpauMOD\",\n" +
            "  \"avatar\": null\n" +
            "}";

        // Make HTTP request to create webhook
        URL url = new URL(createWebhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bot " + botToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            // Parse response to get webhook URL
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Extract webhook URL from JSON response (simple parsing)
            String responseStr = response.toString();
            int tokenStart = responseStr.indexOf("\"token\":\"") + 9;
            int tokenEnd = responseStr.indexOf("\"", tokenStart);
            String token = responseStr.substring(tokenStart, tokenEnd);
            
            int idStart = responseStr.indexOf("\"id\":\"") + 6;
            int idEnd = responseStr.indexOf("\"", idStart);
            String webhookId = responseStr.substring(idStart, idEnd);
            
            this.webhookUrl = "https://discord.com/api/webhooks/" + webhookId + "/" + token;
            plugin.getLogger().info("Discord webhook created successfully!");
        } else {
            plugin.getLogger().warning("Failed to create Discord webhook. Response code: " + responseCode);
        }
    }

    public void sendPunishment(Punishment punishment) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                sendPunishmentMessage(
                    punishment.getAction(),
                    punishment.getPunisher(),
                    punishment.getPlayer(),
                    punishment.getReason(),
                    punishment.getDuration()
                );
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord message: " + e.getMessage());
            }
        });
    }

    public void sendPunishmentMessage(String action, String staffName, String playerName, String reason, long duration) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Create placeholders map
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("action", action);
                placeholders.put("staff_name", staffName);
                placeholders.put("player_name", playerName);
                placeholders.put("reason", reason);
                placeholders.put("duration", formatDuration(duration));
                placeholders.put("server_name", plugin.getServer().getName());

                // Format the message
                String message = plugin.getConfigManager().formatDiscordMessage(placeholders);

                // Create Discord message payload
                String payload = "{\n" +
                    "  \"content\": \"" + escapeJson(message) + "\",\n" +
                    "  \"username\": \"kpauMOD\"\n" +
                    "}";

                // Send HTTP request
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    plugin.getLogger().info("Successfully sent punishment message to Discord");
                } else {
                    plugin.getLogger().warning("Failed to send Discord message. Response code: " + responseCode);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord message: " + e.getMessage());
            }
        });
    }

    private String formatDuration(long duration) {
        if (duration <= 0) return "Permanent";
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " day(s)";
        if (hours > 0) return hours + " hour(s)";
        if (minutes > 0) return minutes + " minute(s)";
        return seconds + " second(s)";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
