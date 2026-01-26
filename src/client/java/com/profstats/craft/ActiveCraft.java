package com.profstats.craft;

import java.io.BufferedWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.UserData;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.nio.file.*;
import java.time.Instant;

public class ActiveCraft {
    private static final Pattern LEVEL_PATTERN = Pattern.compile("Lv\\. Min: (?:§f)?(\\d+)"); // Capute both profession and combat level
    private static final Pattern MATERIAL_PATTERN = Pattern.compile("^(.*?)§6 \\[§e(✫*)(?:§8✫*)?§6\\]$");
    private static final Pattern EMPTY_INGREDIENT_PATTERN = Pattern.compile("^§7Ingredient Slots §8\\(Optional\\)$");

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

    private Integer total_xp;
    private Integer xp_multiplier;
    private Integer level_percent;

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
    public void setCraftItems(ScreenHandler screenHandler) {
        this.material1 = parseMaterial(screenHandler.getSlot(0).getStack(), MinecraftClient.getInstance().player);
        this.material2 = parseMaterial(screenHandler.getSlot(9).getStack(), MinecraftClient.getInstance().player);

        this.ingredient1 = parseIngredient(screenHandler.getSlot(2).getStack(), MinecraftClient.getInstance().player);
        this.ingredient2 = parseIngredient(screenHandler.getSlot(3).getStack(), MinecraftClient.getInstance().player);
        this.ingredient3 = parseIngredient(screenHandler.getSlot(11).getStack(), MinecraftClient.getInstance().player);
        this.ingredient4 = parseIngredient(screenHandler.getSlot(12).getStack(), MinecraftClient.getInstance().player);
        this.ingredient5 = parseIngredient(screenHandler.getSlot(20).getStack(), MinecraftClient.getInstance().player);
        this.ingredient6 = parseIngredient(screenHandler.getSlot(21).getStack(), MinecraftClient.getInstance().player);

        CraftingResultSlotTracker.setSlots(screenHandler);

        // Maybe read existing items in the other slots so that we can detect the new craft level later on

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
            this.xp_multiplier = matcher.group(1) != null ? (int) Double.parseDouble(matcher.group(1)) : 1;
            this.total_xp = Integer.parseInt(matcher.group(2));
            this.level_percent = Integer.parseInt(matcher.group(3));

            return true;
        } else {
            return false;
        }
    }

    public Integer setCraftLevel(ItemStack stack, PlayerEntity player) {
        if (stack.isEmpty() || player == null) return null;

        List<Text> tooltip = stack.getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);

        for(int i = 0; i < tooltip.size(); i++) {

            String levelLine =  tooltip.get(i).getString();

            Matcher matcher = LEVEL_PATTERN.matcher(levelLine);

            if(matcher.find()) {
                craftLevel = Integer.parseInt(matcher.group(1));
                return craftLevel;        
            } 
        }

        return null;
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
                profession == null || professionLevel == null || total_xp == null || 
                xp_multiplier == null || level_percent == null || craftLevel == null ||
                material1 == null || material2 == null
            ) {
                ProfStatsClient.LOGGER.info("""
                    [ProfStats] Aborting craft scan because something null:
                    profession: %s
                    professionLevel: %s
                    total_xp: %s
                    xp_multiplier: %s
                    level_percent: %s
                    craftlevel: %s
                    material1: %s
                    material2: %s
                """.formatted(profession, professionLevel, total_xp, xp_multiplier, level_percent, craftLevel, material1, material2));
                return;
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                // Write CSV header if new file
                if (!fileExists) {
                    String csvHeaders = "timestamp,profession,professionLevel,total_xp,xp_multiplier,level_percent,craftLevel,";
                    csvHeaders += "material1Level,material1Tier,material1Count,material1Name,";
                    csvHeaders += "material2Level,material2Tier,material2Count,material2Name,";

                    csvHeaders += "ingredient1Level,ingredient1Tier,ingredient1Name,";
                    csvHeaders += "ingredient2Level,ingredient2Tier,ingredient2Name,";
                    csvHeaders += "ingredient3Level,ingredient3Tier,ingredient3Name,";
                    csvHeaders += "ingredient4Level,ingredient4Tier,ingredient4Name,";
                    csvHeaders += "ingredient5Level,ingredient5Tier,ingredient5Name,";
                    csvHeaders += "ingredient6Level,ingredient6Tier,ingredient6Name\n";

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
            toSafeCsv(total_xp),
            toSafeCsv(xp_multiplier),
            toSafeCsv(level_percent),
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

    private IngredientData parseIngredient(ItemStack ingredientItemStack, PlayerEntity player) {
        String ingredientName = ingredientItemStack.getName().getString();

        if (ingredientName == null) return new IngredientData(null, null, null);
        if (EMPTY_INGREDIENT_PATTERN.matcher(ingredientName).matches()) {
            return new IngredientData(null, null, null);
        }

        Integer tier = getIngredientTier(ingredientItemStack.getName());
        Integer level = getLevel(ingredientItemStack, player);
        
        String parsedIngredientName = parseIngredientName(ingredientName);

        return new IngredientData(parsedIngredientName, tier, level);
    }

    private String parseIngredientName(String ingredientName) {
        int bracketIndex = ingredientName.indexOf('[');
        if (bracketIndex == -1) {
            return ingredientName.trim();
        }
        return ingredientName.substring(0, bracketIndex).trim();
    }

    private int getIngredientTier(Text text) {
        return countDarkGrayStars(text, text.getStyle().getColor());
    }

    private int countDarkGrayStars(Text text, TextColor inheritedColor) {
        int count = 0;
        TextColor color = text.getStyle().getColor();

        if (color == null) color = inheritedColor;

        String raw = text.getString();

        // Avoids counting the same text more than once,
        // and must be root node to know the color of the text 
        boolean hasChildren = !text.getSiblings().isEmpty();

        if (!hasChildren) {
            if (raw.contains("✫") && color != TextColor.fromFormatting(Formatting.DARK_GRAY)) {
                count += raw.length();
            }
        }

        // Recurse through siblings
        for (Text sibling : text.getSiblings()) {
            count += countDarkGrayStars(sibling, color);
        }

        return count;
    }



    private MaterialData parseMaterial(ItemStack material, PlayerEntity player) {
        String name;
        Integer tier;

        Integer count = material.getCount();
        Integer level = getLevel(material, player);


        Matcher matcher = MATERIAL_PATTERN.matcher(material.getName().getString());
        if (matcher.find()) {
            name = matcher.group(1).trim();
            tier = matcher.group(2).length(); // Number of 'highlighted' stars in the ingredient name
        } else {
            return null;
        }

        return new MaterialData(name, level, tier, count);

    }

        private Integer getLevel(ItemStack stack, PlayerEntity player) {
        var tooltip = stack.getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);
        for (Text line : tooltip) {
            String raw = line.getString();
            Matcher matcher = LEVEL_PATTERN.matcher(raw);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return null;
    }
}
