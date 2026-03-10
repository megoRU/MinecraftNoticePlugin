package org.megoru.ru;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private final HttpClient client = HttpClient.newHttpClient();

    private final List<String> joinBuffer = Collections.synchronizedList(new ArrayList<>());
    private final List<String> quitBuffer = Collections.synchronizedList(new ArrayList<>());

    private Set<String> ignorePlayers;

    private String webhook;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            boolean mkdir = getDataFolder().mkdirs();
            getLogger().info("MinecraftNoticePlugin created: " + mkdir);
        }

        saveDefaultConfig();

        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        webhook = getConfig().getString("webhook");

        if (webhook == null || webhook.isBlank() || webhook.equals("https://discord.com/api/webhooks/ID/TOKEN")) {
            getLogger().warning("Webhook URL not set in config.yml");
            return;
        }

        ignorePlayers = new HashSet<>(getConfig().getStringList("ignore_players"));

        getServer().getPluginManager().registerEvents(this, this);

        startBatchWorker();

        getLogger().info("MinecraftNoticePlugin enabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String player = event.getPlayer().getName();

        if (ignorePlayers.contains(player)) {
            return;
        }

        joinBuffer.add(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();

        if (ignorePlayers.contains(player)) {
            return;
        }

        quitBuffer.add(player);
    }

    private void startBatchWorker() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {

            if (!joinBuffer.isEmpty()) {

                List<String> players;

                synchronized (joinBuffer) {
                    players = new ArrayList<>(joinBuffer);
                    joinBuffer.clear();
                }

                if (players.size() == 1) {

                    String msg = getConfig().getString("join_message");

                    if (msg != null && !msg.isBlank()) {
                        sendDiscord(String.format(msg, players.get(0)));
                    }

                } else {

                    String msg = getConfig().getString("join_batch_message");

                    if (msg != null && !msg.isBlank()) {
                        sendDiscord(String.format(msg, String.join(", ", players)));
                    }
                }
            }

            if (!quitBuffer.isEmpty()) {

                List<String> players;

                synchronized (quitBuffer) {
                    players = new ArrayList<>(quitBuffer);
                    quitBuffer.clear();
                }

                if (players.size() == 1) {

                    String msg = getConfig().getString("quit_message");

                    if (msg != null && !msg.isBlank()) {
                        sendDiscord(String.format(msg, players.get(0)));
                    }

                } else {

                    String msg = getConfig().getString("quit_batch_message");

                    if (msg != null && !msg.isBlank()) {
                        sendDiscord(String.format(msg, String.join(", ", players)));
                    }
                }
            }

        }, 100L, 100L);
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