package com.profstats.craft;

import java.io.BufferedWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.UserData;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.nio.file.*;
import java.time.Instant;

public class ActiveCraft {
    private static final Pattern CRAFT_ITEM_LEVEL_PATTERN = Pattern.compile("Combat Level.*?(\\d+)");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("(\\d+)");

    private record MaterialData(String name, Integer level, Integer tier, Integer count) {}
    private record IngredientData(String name, Integer tier, Integer level) {}

    private MaterialData material1;
    private MaterialData material2;

    private IngredientData ingredient1;
    private IngredientData ingredient2;
    private IngredientData ingredient3;
    private IngredientData ingredient4;
    private IngredientData ingredient5;
    private IngredientData ingredient6;

    private Profession profession;
    private Integer professionLevel; // Must set this when getting the XP (must read each time we do a craft even if we do multiple at once)

    private Integer totalXp;
    private Integer xpMultiplier;
    private Integer levelPercent;

    private Integer craftLevel; // Provided when craft finishes


    public ActiveCraft(Profession profession) {
        this.profession = profession;
        this.professionLevel = UserData.getProfessionLevel(profession);
    }

    public ActiveCraft copyCraftSetup() {
        ActiveCraft copiedStatistic = new ActiveCraft(profession);

        copiedStatistic.material1 = material1;
        copiedStatistic.material2 = material2;

        copiedStatistic.ingredient1 = ingredient1;
        copiedStatistic.ingredient2 = ingredient2;
        copiedStatistic.ingredient3 = ingredient3;
        copiedStatistic.ingredient4 = ingredient4;
        copiedStatistic.ingredient5 = ingredient5;
        copiedStatistic.ingredient6 = ingredient6;

        return copiedStatistic;
    }

    /*
    * Slots positions in crafting GUI
    * 
    * Material slot 1: Slot id 0
    * Material slot 2: slot id 9
    * 
    * Ingredient slot 1 (top left): 2
    * Ingredient slot 2 (top right): 3
    * Ingredient slot 3 (mid left): 11
    * Ingredient slot 4 (top right): 12
    * Ingredient slot 5 (bottom left): 20
    * Ingredient slot 6 (top right): 21
    */
    public void setCraftItems(AbstractContainerMenu screenHandler) {
        this.material1 = parseMaterial(screenHandler.getSlot(0).getItem(), Minecraft.getInstance().player);
        this.material2 = parseMaterial(screenHandler.getSlot(9).getItem(), Minecraft.getInstance().player);

        this.ingredient1 = parseIngredient(screenHandler.getSlot(2).getItem(), Minecraft.getInstance().player);
        this.ingredient2 = parseIngredient(screenHandler.getSlot(3).getItem(), Minecraft.getInstance().player);
        this.ingredient3 = parseIngredient(screenHandler.getSlot(11).getItem(), Minecraft.getInstance().player);
        this.ingredient4 = parseIngredient(screenHandler.getSlot(12).getItem(), Minecraft.getInstance().player);
        this.ingredient5 = parseIngredient(screenHandler.getSlot(20).getItem(), Minecraft.getInstance().player);
        this.ingredient6 = parseIngredient(screenHandler.getSlot(21).getItem(), Minecraft.getInstance().player);

        CraftingResultSlotTracker.setSlots(screenHandler);
    }

    /*
     * @return true when successful
     */
    public boolean tryExtractCraftReward(String message) {
        Pattern pattern = Pattern.compile(
            "(?:§dx(\\d+(?:\\.\\d+)?) )?" + // First group -> xp multiplier
            "§7\\[\\+(?:§d)?(\\d+) §f. §7" + // Second group -> xp
            profession.displayName + // Only match against the relevant profession
            " XP\\] §6\\[(\\d+)%\\]" // Third group -> xp percentage
        );

        Matcher matcher = pattern.matcher(message);
        if(matcher.find()) {
            this.xpMultiplier = matcher.group(1) != null ? (int) Double.parseDouble(matcher.group(1)) : 1;
            this.totalXp = Integer.parseInt(matcher.group(2));
            this.levelPercent = Integer.parseInt(matcher.group(3));

            return true;
        } else {
            return false;
        }
    }

    public Integer setCraftLevel(ItemStack stack, Player player) {
        if (stack.isEmpty() || player == null) return null;

        List<Component> tooltip = stack.getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);

        // Tooltip with craft is at least 8 lines
        if (tooltip.size() <= 8) return null;

        craftLevel = getCraftItemLevel(tooltip);
        return craftLevel;
    }


    public void store() {
        try {
            // Create directory in the mod’s config folder
            Path dir = Paths.get("data", "prof-stats");
            Files.createDirectories(dir);

            // Use profession name as filename
            Path filePath = dir.resolve("crafting.csv");

            boolean fileExists = Files.exists(filePath);

            if(
                profession == null || professionLevel == null || totalXp == null || 
                xpMultiplier == null || levelPercent == null || craftLevel == null ||
                material1 == null || material2 == null
            ) {
                // Only quit if we failed to detect the gather.
                // Some data may be null if player exits gather screen before material is read
                // Avoid logging as this is expected to happen when crafts are cancelled mid-craft
                if (profession == null || totalXp == null) {
                    return;
                }


                ProfStatsClient.LOGGER.info("""
                    [ProfStats] Something was null when scanning craft:
                    profession: %s
                    profession_level: %s
                    total_xp: %s
                    xp_multiplier: %s
                    level_percent: %s
                    craft_level: %s
                    material1: %s
                    material2: %s
                """.formatted(profession, professionLevel, totalXp, xpMultiplier, levelPercent, craftLevel, material1, material2));


            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                // Write CSV header if new file
                if (!fileExists) {
                    String csvHeaders = "timestamp,profession,profession_level,total_xp,xp_multiplier,level_percent,craft_level,";
                    csvHeaders += "material1_level,material1_tier,material1_count,material1_name,";
                    csvHeaders += "material2_level,material2_tier,material2_count,material2_name,";

                    csvHeaders += "ingredient1_level,ingredient1_tier,ingredient1_name,";
                    csvHeaders += "ingredient2_level,ingredient2_tier,ingredient2_name,";
                    csvHeaders += "ingredient3_level,ingredient3_tier,ingredient3_name,";
                    csvHeaders += "ingredient4_level,ingredient4_tier,ingredient4_name,";
                    csvHeaders += "ingredient5_level,ingredient5_tier,ingredient5_name,";
                    csvHeaders += "ingredient6_level,ingredient6_tier,ingredient6_name\n";

                    writer.write(csvHeaders);
                }

                ProfStatsClient.LOGGER.info("[ProfStats] Detected new craft! Writing to '" + filePath.toString() + "'");
                writer.write(craftToCsvLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String craftToCsvLine() {
        return String.join(",",
            toSafeCsv(Instant.now()),
            profession.displayName,
            toSafeCsv(professionLevel),
            toSafeCsv(totalXp),
            toSafeCsv(xpMultiplier),
            toSafeCsv(levelPercent),
            toSafeCsv(craftLevel),
            
            toSafeCsv(material1.level()), toSafeCsv(material1.tier()), toSafeCsv(material1.count()), toSafeCsv(material1.name()),
            toSafeCsv(material2.level()), toSafeCsv(material2.tier()), toSafeCsv(material2.count()), toSafeCsv(material2.name()),

            toSafeCsv(ingredient1.level()), toSafeCsv(ingredient1.tier()), toSafeCsv(ingredient1.name()), 
            toSafeCsv(ingredient2.level()), toSafeCsv(ingredient2.tier()), toSafeCsv(ingredient2.name()),
            toSafeCsv(ingredient3.level()), toSafeCsv(ingredient3.tier()), toSafeCsv(ingredient3.name()),
            toSafeCsv(ingredient4.level()), toSafeCsv(ingredient4.tier()), toSafeCsv(ingredient4.name()),
            toSafeCsv(ingredient5.level()), toSafeCsv(ingredient5.tier()), toSafeCsv(ingredient5.name()),
            toSafeCsv(ingredient6.level()), toSafeCsv(ingredient6.tier()), toSafeCsv(ingredient6.name())
        ) + "\n";
    }

    private String toSafeCsv(Object value) {
        return value == null ? "" : value.toString();
    }

    private IngredientData parseIngredient(ItemStack ingredientItemStack, Player player) {
        String ingredientName = removeSpecialCharacters(ingredientItemStack.getHoverName().getString());

        if (ingredientName == null) return new IngredientData(null, null, null);
        if (ingredientName.equals("7Ingredient Slots 8(Optional)")) {
            return new IngredientData(null, null, null);
        }

        List<Component> c = ingredientItemStack.getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);

        Integer tier = getStarCount(c.get(2));
        Integer level = getLevel(c.get(4));
        
        return new IngredientData(ingredientName, tier, level);
    }

    private MaterialData parseMaterial(ItemStack material, Player player) {
        Integer count = material.getCount();

        List<Component> c = material.getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);

        Integer tier = getStarCount(c.get(2));
        Integer level = getLevel(c.get(4));
        String name = removeSpecialCharacters(material.getHoverName().getString());

        return new MaterialData(name, level, tier, count);
    }

    private Integer getStarCount(Component component) {
        try {
            Component firstStarContainer = component.getSiblings().get(1)
                                                .getSiblings().get(0)
                                                .getSiblings().get(0)
                                                .getSiblings().get(0);
            
            TextColor color = firstStarContainer.getStyle().getColor();
            if (color == null) return 0;

            int hex = color.getValue();

            return switch (hex) {
                case 0xE6E647 -> 1; // Gold
                case 0xE647E6 -> 2; // Magenta
                case 0x47E6E6 -> 3; // Cyan
                case 0x000000 -> 0; // Black
                default -> null;
            };
        } catch (IndexOutOfBoundsException e) {
            return null; // UI structure changed or isn't the star UI
        }
    }

    private String removeSpecialCharacters(String str) {
        return str.replaceAll("[^\\x20-\\x7E]", "").trim();
    }

    private Integer getCraftItemLevel(List<Component> tooltip) {
        for(Component component : tooltip) {
            component.getString();
            Matcher matcher = CRAFT_ITEM_LEVEL_PATTERN.matcher(component.getString());
            
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return null;
    }


    private Integer getLevel(Component component) {
        component.getString();
        Matcher matcher = LEVEL_PATTERN.matcher(component.getString());
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
