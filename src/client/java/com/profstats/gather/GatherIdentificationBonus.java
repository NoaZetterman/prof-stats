package com.profstats.gather;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.ProfStatsClient;
import com.profstats.mixin.client.PlayerListHudAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

/*
 * Helper class to read gathering XP and speed bonus
 */
public class GatherIdentificationBonus {
    private static final Pattern IDENTIFICATION_GXP_PATTERN = Pattern.compile("^Gathering Experience.*([+-]\\d+)\\%");
    private static final Pattern IDENTIFICATION_GSPEED_PATTERN = Pattern.compile("Gathering Speed.*([+-]\\d+)\\%");

    private static final Pattern STATUS_EFFECT_GXP_PATTERN = Pattern.compile("([+-]?\\d+)% Gather XP Bonus");
    private static final Pattern STATUS_EFFECT_GSPEED_PATTERN = Pattern.compile("([+-]?\\d+)% Gather Speed");

    private static final int[] EQUIPMENT_INDEXES = { 
            36,37,38,39, // Armor
            9,10,11,12 // Accessories
        };


    public static Integer readGatherSpeedBonus() {
        int gatherSpeedBonus = 0;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) return null;
        if (client.level == null) return null;

        
        for(int equipmentIdx : EQUIPMENT_INDEXES) {
            Integer gatheringSpeed = identificationStat(player.getInventory().getItem(equipmentIdx), player, IDENTIFICATION_GSPEED_PATTERN);

            if (gatheringSpeed == null) {
                return null;
            }

            gatherSpeedBonus += gatheringSpeed;
        }
        gatherSpeedBonus += readStatusEffectGatherSpeedBonus();

        return gatherSpeedBonus;
    }

    // Detect XP from gear + consus and other status effects
    public static Integer readGatherXpBonus() {
        int gatherXpBonus = 0;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) return null;
        if (client.level == null) return null;

        for(int equipmentIdx : EQUIPMENT_INDEXES) {
            Integer gatheringXp = identificationStat(player.getInventory().getItem(equipmentIdx), player, IDENTIFICATION_GXP_PATTERN);

            if (gatheringXp == null) {
                return null;
            }

            gatherXpBonus += gatheringXp;
        }

        int charmXp = 0;

        for(int i = 0; i <= 6; i++) {
            List<Component> tooltip = player.getInventory().getItem(i).getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);

            if (tooltip.get(0).getString().equals("\uDAFC\uDC00Charm of the Void\uDAFC\uDC00")) {
                Matcher m = IDENTIFICATION_GXP_PATTERN.matcher(tooltip.get(8).getString());
                if (m.find()) {
                    int foundCharmXp = Integer.parseInt(m.group(1));

                    if (foundCharmXp > charmXp) {
                        charmXp = foundCharmXp;
                    }
                }
            }
        }

        for(int i = 13; i < 35; i++) {
            List<Component> tooltip = player.getInventory().getItem(i).getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);
            if (tooltip.get(0).getString().equals("\uDAFC\uDC00Charm of the Void\uDAFC\uDC00")) {
                Matcher m = IDENTIFICATION_GXP_PATTERN.matcher(tooltip.get(8).getString());
                if (m.find()) {
                    int foundCharmXp = Integer.parseInt(m.group(1));

                    if (foundCharmXp > charmXp) {
                        charmXp = foundCharmXp;
                    }
                }
            }
        }

        gatherXpBonus += charmXp;
        gatherXpBonus += readStatusEffectGatherXpBonus();
        
        return gatherXpBonus;
    }

    private static Integer identificationStat(ItemStack item, LocalPlayer player, Pattern identification_pattern) {
        List<Component> tooltip = item.getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);

        for (int i = 0; i < tooltip.size(); i++) {
            Matcher m = identification_pattern.matcher(tooltip.get(i).getString());
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }

        return 0;
    }


    private static int readStatusEffectGatherXpBonus() {
        return readStatusEffectBonus(STATUS_EFFECT_GXP_PATTERN);
    }

    private static int readStatusEffectGatherSpeedBonus() {
        return readStatusEffectBonus(STATUS_EFFECT_GSPEED_PATTERN);
    }

    private static int readStatusEffectBonus(Pattern effectPattern) {
        int effectBonus = 0;

        Minecraft client = Minecraft.getInstance();
        
        if (client.gui != null) {
            var playerListHud = client.gui.getTabList();
            
            Component footer = ((PlayerListHudAccessor) playerListHud).getFooter();
            
            if (footer != null) {
                String footerText = footer.getString();
                Matcher matcher = effectPattern.matcher(footerText);

                while (matcher.find()) {
                    int value = Integer.parseInt(matcher.group(1));
                    effectBonus += value;
                }
            }
        }

        return effectBonus;
    }
}
