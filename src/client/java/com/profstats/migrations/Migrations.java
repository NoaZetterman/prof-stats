package com.profstats.migrations;

public class Migrations {
    public static void migrate() {
        CraftingCsvHeaderRename001.migrateIfNeeded();
        GatheringCsvHeaderRename002.migrateIfNeeded();
    }
}
