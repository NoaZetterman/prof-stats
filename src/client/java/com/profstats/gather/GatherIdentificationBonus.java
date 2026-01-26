package com.profstats.gather;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.ProfStatsClient;
import com.profstats.mixin.client.PlayerListHudAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

/*
 * Helper class to read gathering XP and speed bonus
 */
public class GatherIdentificationBonus {
    private static final Pattern CHARM_GXP_PATTERN = Pattern.compile("^\\+(\\d+)\\%");
    private static final Pattern IDENTIFICATION_GXP_PATTERN = Pattern.compile("([+-]\\d+)\\%§8\\/(-?\\d+)\\%\\s*Gather XP Bonus");
    private static final Pattern IDENTIFICATION_GSPEED_PATTERN = Pattern.compile("([+-]\\d+)\\%§8\\/(-?\\d+)\\%\\s*Gather Speed");
    private static final Pattern POSITIVE_IDENTIFICATION_VALUE_PATTERN = Pattern.compile("^\\+(\\d+)\\%§8\\/(\\d+)\\%\\s");
    private static final Pattern CRAFTED_DEGRADATION_PERCENTAGE = Pattern.compile("\\[(\\d+)\\%\\]$");

    private static final Pattern STATUS_EFFECT_GXP_PATTERN = Pattern.compile("([+-]?\\d+)% Gather XP Bonus");
    private static final Pattern STATUS_EFFECT_GSPEED_PATTERN = Pattern.compile("([+-]?\\d+)% Gather Speed");

    private static final int[] EQUIPMENT_INDEXES = { 
            36,37,38,39, // Armor
            9,10,11,12 // Accessories
        };


    public static Integer readGatherSpeedBonus() {
        int gatherSpeedBonus = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return null;
        if (client.world == null) return null;

        
        for(int equipmentIdx : EQUIPMENT_INDEXES) {
            Integer gatheringSpeed = identificationStat(player.getInventory().getStack(equipmentIdx), player, IDENTIFICATION_GSPEED_PATTERN);

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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return null;
        if (client.world == null) return null;

        for(int equipmentIdx : EQUIPMENT_INDEXES) {
            Integer gatheringXp = identificationStat(player.getInventory().getStack(equipmentIdx), player, IDENTIFICATION_GXP_PATTERN);

            if (gatheringXp == null) {
                return null;
            }

            gatherXpBonus += gatheringXp;
        }

        int charmXp = 0;

        for(int i = 0; i <= 6; i++) {
            List<Text> tooltip = player.getInventory().getStack(i).getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);
            if (tooltip.get(0).getString().equals("Charm of the Void")) {
                Matcher m = CHARM_GXP_PATTERN.matcher(tooltip.get(6).getString());
                if (m.find()) {
                    int foundCharmXp = Integer.parseInt(m.group(1));

                    if (foundCharmXp > charmXp) {
                        charmXp = foundCharmXp;
                    }
                }
            }
        }

        for(int i = 13; i < 35; i++) {
            List<Text> tooltip = player.getInventory().getStack(i).getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);
            if (tooltip.get(0).getString().equals("Charm of the Void")) {
                Matcher m = CHARM_GXP_PATTERN.matcher(tooltip.get(6).getString());
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

    private static Integer identificationStat(ItemStack item, ClientPlayerEntity player, Pattern identification_pattern) {
        List<Text> tooltip = item.getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);

        for (int i = 0; i < tooltip.size(); i++) {
            Matcher m = identification_pattern.matcher(tooltip.get(i).getString());
            if (m.find()) {
                int itemCurrentValue = Integer.parseInt(m.group(1));
                int baseValue = Integer.parseInt(m.group(2));

                Integer degradedValue = degradedValue(item, player, itemCurrentValue, baseValue);

                return degradedValue; // This can be null when we did not properly detect the true value
            }
        }

        return 0;
    }


    /*
     * Calculates the degraded value, as the one when hovering the item is sometimes off by one due to discrepancies in Wynn implementation.
     *
     * Mirrors Wynncraft calculation related to durability degradation, keeping the same floating point errors.
     * Therefore, some values are casted to integers instead of rounded / floored.
     * 
     * Sorry for a huge method but I can't be assed to refactor it properly. This should probably be its own module...
     */
    private static Integer degradedValue(ItemStack itemStack, ClientPlayerEntity player, int displayedValue, int baseValue) {
        double deg1 = 0.005;
        double deg2 = 0.010;
        double deg3 = 0.015;
        double deg4 = 0.025;
        double deg5 = 0.035;

        double multiplier = 1.0;

        int degradationPercent = 100;

        List<Text> tooltip = itemStack.getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);

        
        Matcher degradationPercentMatcher = CRAFTED_DEGRADATION_PERCENTAGE.matcher(tooltip.get(0).getString());
        if (degradationPercentMatcher.find()) {
            degradationPercent = Integer.parseInt(degradationPercentMatcher.group(1));
        } else {
            ProfStatsClient.LOGGER.info("Failed to find degradation % for:" + tooltip.get(0).getString());
            return null;
        }
        
        if (degradationPercent == 100) {
            return baseValue;
        }

        for(int i = 0; i < 11; i++) {
            multiplier -= deg1;

            if (((int) (multiplier*100)) == degradationPercent) {

                // 0.945 (94%) is last with 0.005 degradation, next step has 93% degradation so it can be detected.
                if (multiplier == 0.945) {
                    return (int) (baseValue*multiplier);
                }

                // Possible value pairs:
                // 0.995, 0.990 - 99%
                // 0.985, 0.980 - 98%
                // 0.975, 0.970 - 97%
                // 0.965, 0.960 - 96%
                // 0.955, 0.950 - 95%

                double multiplierNext = multiplier - deg1;


                // As shown in character info under identifications
                int actual = (int) (baseValue*multiplier);
                int actualNext = (int) (baseValue*multiplierNext);


                // If actual value in both cases are equal, we know the value
                if (actual == actualNext) {
                    return actual;
                }

                // Iterate over all identifications to find if one differs between the two multipliers
                for (int j = 1; j < tooltip.size()-2; j++) {
                    Matcher m = POSITIVE_IDENTIFICATION_VALUE_PATTERN.matcher(tooltip.get(i).getString());
                    if (m.find()) {
                        int degraded = Integer.parseInt(m.group(1));
                        int base = Integer.parseInt(m.group(2));

                        if (((int) (base * multiplierNext)) != ((int) (base * multiplier))) {
                            if (((int) (base * multiplier)) == degraded) {
                                return actualNext;
                            }

                            if (((int) (base * multiplierNext)) == degraded) {
                                return actualNext;
                            }
                        }
                    }
                }

                // Skipping second detection method
                // Durability percent detection is not necessarily accurate because some values may be skipped.
                // Can potentially be implemented for higher dura items (24 dura misses 2 values, so perhaps around 30+ dura will always work)
                
                // Detect these from ItemStack, convert to percent
                // double durabilityMaxPercent = 1;
                // double durabilityMinPercent = 0;


                // // If min is above multiplier, 
                // if (durabilityMinPercent > durabilityPercent) {
                //     return actual;
                // }
                
                // if (durabilityMaxPercent <= durabilityPercent) {
                //     return actualNext;
                // }


                ProfStatsClient.LOGGER.info("Failed to detect degradation %: " + degradationPercent);

                return null;
            }
        }

        for(int i = 0; i < 9; i++) {
            multiplier -= deg2;
            if (((int) (multiplier*100)) == degradationPercent) {
                // 1 possible value
                return (int) (baseValue*multiplier);
            }
        }

        for(int i = 0; i < 10; i++) {
            multiplier -= deg3;
            if (((int) (multiplier*100)) == degradationPercent) {
                // 1 possible value
                return (int) (baseValue*multiplier);
            }
        }

        for(int i = 0; i < 10; i++) {
            multiplier -= deg4;
            if (((int) (multiplier*100)) == degradationPercent) {
                // 1 possible value
                return (int) (baseValue*multiplier);
            }
        }

        for(int i = 0; i < 10; i++) {
            multiplier -= deg5;
            if (((int) (multiplier*100)) == degradationPercent) {
                // 1 possible value
                return (int) (baseValue*multiplier);
            }
        }

        ProfStatsClient.LOGGER.info("Failed to detect degradation %: " + degradationPercent);
        // This should not happen?
        return null;
    }


    private static int readStatusEffectGatherXpBonus() {
        return readStatusEffectBonus(STATUS_EFFECT_GXP_PATTERN);
    }

    private static int readStatusEffectGatherSpeedBonus() {
        return readStatusEffectBonus(STATUS_EFFECT_GSPEED_PATTERN);
    }

    private static int readStatusEffectBonus(Pattern effectPattern) {
        int effectBonus = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.inGameHud != null) {
            var playerListHud = client.inGameHud.getPlayerListHud();
            
            Text footer = ((PlayerListHudAccessor) playerListHud).getFooter();
            
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
