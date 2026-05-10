package com.profstats;

import java.util.Arrays;

enum ProfessionType {
    CRAFTING,
    GATHERING
}

public enum Profession {
    WOODCUTTING("Woodcutting", "Chopping", null ,'Ⓒ', ProfessionType.GATHERING),
    MINING("Mining", "Mining",null , 'Ⓑ', ProfessionType.GATHERING),
    FARMING("Farming", "Farming",null , 'Ⓙ', ProfessionType.GATHERING),
    FISHING("Fishing", "Fishing",null , 'Ⓚ', ProfessionType.GATHERING),

    ARMOURING("Armouring", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF042", 'Ⓗ', ProfessionType.CRAFTING),
    WEAPONSMITHING("Weaponsmithing", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF047", 'Ⓖ', ProfessionType.CRAFTING),
    TAILORING("Tailoring", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF046", 'Ⓕ', ProfessionType.CRAFTING),
    WOODWORKING("Woodworking", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF048", 'Ⓘ', ProfessionType.CRAFTING),
    JEWELING("Jeweling", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF044", 'Ⓓ', ProfessionType.CRAFTING),
    COOKING("Cooking", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF043", 'Ⓐ', ProfessionType.CRAFTING),
    SCRIBLING("Scribing", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF045", 'Ⓔ', ProfessionType.CRAFTING),
    ALCHEMISM("Alchemism", null, "\uDAFF\uDFF8\uE053\uDAFF\uDF80\uF041", 'Ⓛ', ProfessionType.CRAFTING);

    public final String displayName;
    public final String actionName;
    public final String screenName;
    public final char symbol;
    public final ProfessionType type;

    Profession(String displayName, String actionName, String screenName, char symbol, ProfessionType type) {
        this.displayName = displayName;
        this.actionName = actionName;
        this.screenName = screenName;
        this.symbol = symbol;
        this.type = type;
    }

    public static Profession[] getCraftingProfessions() {
        return Arrays.stream(Profession.values())
                 .filter(p -> p.type == ProfessionType.CRAFTING)
                 .toArray(Profession[]::new);
    }

    public static Profession[] getGatheringProfessions() {
        return Arrays.stream(Profession.values())
                 .filter(p -> p.type == ProfessionType.GATHERING)
                 .toArray(Profession[]::new);
    }

    public static Profession fromDisplayName(String s) {
        for(Profession p : Profession.values()) {
            if(p.displayName.equals(s)) {
                return p;
            }
        }

        return null;
    }

    public static Profession fromScreenName(String s) {
        for(Profession p : Profession.values()) {
            if(p.screenName != null && p.screenName.equals(s)) {
                return p;
            }
        }

        return null;
    }

    public static boolean professionMentioned(String message) {
        for(Profession p : Profession.values()) {
            if(message.contains(p.displayName)) {
                return true;
            }
        }

        return false;
    }

}
