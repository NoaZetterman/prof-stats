package com.profstats.migrations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import com.profstats.ProfStatsClient;

public class GatheringCsvHeaderRename002 {
    public static void migrateIfNeeded() {
        Path dir = Paths.get("data", "prof-stats");
        Path filePath = dir.resolve("gathering.csv");

        boolean fileExists = Files.exists(filePath);

        if (!fileExists) {
            return;
        }

        Path tempPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");

        try {
            BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
            String header = reader.readLine();

            // If already migrated, do nothing
            if (header.contains("profession_level")) {
                reader.close();
                return;
            }

            

            // Build new header
            String newHeader = header
                    .replace("professionLevel", "profession_level")
                    .replace("levelPercent", "level_percent")
                    .replace("nodeLevel", "node_level")
                    .replace("totalXp", "total_xp")
                    .replace("xpMultiplier", "xp_multiplier")
                    .replace("xpModifier", "xp_modifier")
                    .replace("pvpActive", "pvp_active")
                    .replace("guildGxpBoostLevel", "guild_gxp_boost_level")
                    .replace("speedModifier", "speed_modifier")
                    .replace("toolTier", "tool_tier")
                    .replace("toolSpeed", "tool_speed")
                    .replace("toolDurability", "tool_durability")
                    .replace("materialTier", "material_tier")
                    .replace("materialCount", "material_count")
                    .replace("materialName", "material_name");

            BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

            // Write migrated header
            writer.write(newHeader);
            writer.newLine();

            // Stream the rest (constant memory)
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, read);
            }

            Files.move(tempPath, filePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
            
            writer.close();
            reader.close();
            ProfStatsClient.LOGGER.warn("[ProfStats] Migrated CSV headers for gathering");
        } catch (IOException e) {
            ProfStatsClient.LOGGER.warn("[ProfStats] Failed to migrate CSV headers for gathering:" + e.getMessage());
        }
    }
}
