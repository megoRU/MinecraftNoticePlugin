package org.megoru.ru;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin implements Listener {

    private final HttpClient client = HttpClient.newHttpClient();
    private String webhook;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        webhook = getConfig().getString("webhook");

        if (webhook == null || webhook.isBlank() || webhook.equals("https://discord.com/api/webhooks/ID/TOKEN")) {
            getLogger().warning("Webhook URL not set in config.yml");
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MinecraftNoticePlugin enabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();
        sendDiscord("**" + player + "** joined the server");
    }

    private void sendDiscord(String message) {
        String json = """
            {
              "content": "%s"
            }
            """.formatted(message);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception e) {
            getLogger().warning("Discord error: " + e.getMessage());
        }
    }
}