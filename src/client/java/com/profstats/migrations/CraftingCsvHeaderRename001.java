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

public class CraftingCsvHeaderRename001 {
    public static void migrateIfNeeded() {
        Path dir = Paths.get("data", "prof-stats");
        Path filePath = dir.resolve("crafting.csv");

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
            
            String newHeader = header
                    .replace("professionLevel", "profession_level")
                    .replace("craftLevel", "craft_level")

                    .replace("material1Level", "material1_level")
                    .replace("material1Tier", "material1_tier")
                    .replace("material1Count", "material1_count")
                    .replace("material1Name", "material1_name")

                    .replace("material2Level", "material2_level")
                    .replace("material2Tier", "material2_tier")
                    .replace("material2Count", "material2_count")
                    .replace("material2Name", "material2_name")

                    .replace("ingredient1Level", "ingredient1_level")
                    .replace("ingredient1Tier", "ingredient1_tier")
                    .replace("ingredient1Name", "ingredient1_name")

                    .replace("ingredient2Level", "ingredient2_level")
                    .replace("ingredient2Tier", "ingredient2_tier")
                    .replace("ingredient2Name", "ingredient2_name")

                    .replace("ingredient3Level", "ingredient3_level")
                    .replace("ingredient3Tier", "ingredient3_tier")
                    .replace("ingredient3Name", "ingredient3_name")

                    .replace("ingredient4Level", "ingredient4_level")
                    .replace("ingredient4Tier", "ingredient4_tier")
                    .replace("ingredient4Name", "ingredient4_name")

                    .replace("ingredient5Level", "ingredient5_level")
                    .replace("ingredient5Tier", "ingredient5_tier")
                    .replace("ingredient5Name", "ingredient5_name")

                    .replace("ingredient6Level", "ingredient6_level")
                    .replace("ingredient6Tier", "ingredient6_tier")
                    .replace("ingredient6Name", "ingredient6_name");

            BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

            writer.write(newHeader);
            writer.newLine();

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
            
            ProfStatsClient.LOGGER.warn("[ProfStats] Migrated CSV headers for crafting");
        } catch (IOException e) {
            ProfStatsClient.LOGGER.warn("[ProfStats] Failed to migrate CSV headers for crafting:" + e.getMessage());
        }
    }
}
