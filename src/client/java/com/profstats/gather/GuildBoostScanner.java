package com.profstats.gather;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public class GuildBoostScanner {
    private static final Pattern PROFESSION_BOOST_PATTERN = Pattern.compile("^§d- §7Gathering Experience§8 \\[Lv\\. (\\d+)\\]$");

    private static boolean scanInProgress = false;
    private static Instant scanStartedAt;

    private static int syncId = -1;

    public static int getSyncId() {
        return syncId;
    }

    public static void setSyncId(int id) {
        syncId = id;
    }
    public static void stopScan() {
        scanInProgress = false;
    }

    public static boolean hasActiveScan() {
        return scanInProgress;
    }

    public static void triggerScan() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null && client.getNetworkHandler() != null && (scanInProgress == false || scanStartedAt.plusSeconds(1).isBefore(Instant.now()))) {
            scanStartedAt = Instant.now();
            scanInProgress = true;
            client.getNetworkHandler().sendChatCommand("guild territory");
        }
    }

    public static void scanInventory(List<ItemStack> items) {
        Integer boostLevel = parseGuildXpBoost(items.get(12));
        ActiveGather.setGuildGxpBoostLevel(boostLevel);
        GuildBoostScanner.syncId = -1;
        scanInProgress = false;

    }

    private static Integer parseGuildXpBoost(ItemStack professionItemStack) {
        List<Text> tooltip = professionItemStack.getTooltip(TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
        for (Text line : tooltip) {
            String s = line.getString();
            Matcher m = PROFESSION_BOOST_PATTERN.matcher(s);

            if(m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }

        return 0;
    }
}
