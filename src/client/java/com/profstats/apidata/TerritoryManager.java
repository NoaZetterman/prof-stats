package com.profstats.apidata;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.profstats.ProfStatsClient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TerritoryManager {
    private static final String API_URL = "https://api.wynncraft.com/v3/guild/list/territory";
    private static final Path CACHE_FILE = FabricLoader.getInstance().getConfigDir().resolve("prof-stats/wynn-territories.json");
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    
    private Set<String> territoryNames = new HashSet<>();
    private long lastUpdate = 0;

    public TerritoryManager() {
        if (Files.exists(CACHE_FILE)) {
            loadFromDisk();
        }
        
        // Refresh on booting mc if older than 24h.
        if (System.currentTimeMillis() - lastUpdate > CACHE_TTL_MS) {
            ProfStatsClient.LOGGER.info("[ProfStats] Refreshing territory names from Wynncraft API");
            refreshFromApi();
        }
    }

    public boolean isTerritory(String title) {
        return territoryNames.contains(title);
    }

    private void refreshFromApi() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL)).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(json -> {
                // The v3 API returns a map where keys are territory names
                Map<String, Object> data = new Gson().fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                this.territoryNames = data.keySet();
                this.lastUpdate = System.currentTimeMillis();
                saveToDisk();
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
    }

    private void saveToDisk() {
        try (Writer writer = Files.newBufferedWriter(CACHE_FILE)) {
            CacheData data = new CacheData(lastUpdate, territoryNames);
            new Gson().toJson(data, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadFromDisk() {
        try (Reader reader = Files.newBufferedReader(CACHE_FILE)) {
            CacheData data = new Gson().fromJson(reader, CacheData.class);
            this.lastUpdate = data.timestamp;
            this.territoryNames = data.names;
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static class CacheData {
        long timestamp;
        Set<String> names;
        CacheData(long ts, Set<String> n) { this.timestamp = ts; this.names = n; }
    }
}

