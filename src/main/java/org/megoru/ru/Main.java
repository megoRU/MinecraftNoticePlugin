package org.megoru.ru;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin {

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveDefaultConfig();

        String url = getConfig().getString("webhook");

        if (url == null || url.isBlank()) {
            getLogger().warning("Webhook URL not set in config.yml");
            return;
        }

        String json = """
            {
              "content": "Server started"
            }
            """;

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> getLogger().info("Discord status: " + response.statusCode()))
                    .exceptionally(e -> {
                        getLogger().warning("Discord error: " + e.getMessage());
                        return null;
                    });

        } catch (IllegalArgumentException e) {
            getLogger().severe("Invalid webhook URL in config.yml");
        }
    }
}
