package com.profstats.gather;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

public class GuildBoostScanner {
    private static final Pattern PROFESSION_BOOST_PATTERN = Pattern.compile("^§d- §7Gathering Experience§8 \\[Lv\\. (\\d+)\\]$");

    private static boolean scanInProgress = false;
    private static Instant scanStartedAt;

    private static Runnable onInventoryScanCompleteCallback = null;

    private static int syncId = -1;

    public static int getSyncId() {
        return syncId;
    }

    public static void setSyncId(int id) {
        syncId = id;
    }
    public static void stopScan() {
        if (onInventoryScanCompleteCallback != null) {
            try {
                onInventoryScanCompleteCallback.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onInventoryScanCompleteCallback = null;
            }
        }
        scanInProgress = false;
    }

    public static boolean hasActiveScan() {
        return scanInProgress;
    }

    public static void triggerScan(Runnable callback) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.getConnection() != null && (scanInProgress == false || scanStartedAt.plusSeconds(1).isBefore(Instant.now()))) {
            scanStartedAt = Instant.now();
            scanInProgress = true;
            onInventoryScanCompleteCallback = callback;
            client.getConnection().sendCommand("guild territory");
        }
    }

    public static void scanInventory(List<ItemStack> items) {
        Integer boostLevel = parseGuildXpBoost(items.get(12));
        ActiveGather.setGuildGxpBoostLevel(boostLevel);
        GuildBoostScanner.syncId = -1;
        scanInProgress = false;
        if (onInventoryScanCompleteCallback != null) {
            try {
                onInventoryScanCompleteCallback.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onInventoryScanCompleteCallback = null;
            }
        }
    }

    private static Integer parseGuildXpBoost(ItemStack professionItemStack) {
        List<Component> tooltip = professionItemStack.getTooltipLines(TooltipContext.EMPTY, Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
        for (Component line : tooltip) {
            String s = line.getString();
            Matcher m = PROFESSION_BOOST_PATTERN.matcher(s);

            if(m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }

        return 0;
    }
}
