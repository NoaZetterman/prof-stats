package com.profstats;

import java.util.Arrays;

enum ProfessionType {
    CRAFTING,
    GATHERING
}

public enum Profession {
    WOODCUTTING("Woodcutting", "Chopping", 'Ⓒ', ProfessionType.GATHERING),
    MINING("Mining", "Mining", 'Ⓑ', ProfessionType.GATHERING),
    FARMING("Farming", "Farming", 'Ⓙ', ProfessionType.GATHERING),
    FISHING("Fishing", "Fishing", 'Ⓚ', ProfessionType.GATHERING),

    ARMOURING("Armouring", "Armouring", 'Ⓗ', ProfessionType.CRAFTING),
    WEAPONSMITHING("Weaponsmithing", "Weaponsmithing", 'Ⓖ', ProfessionType.CRAFTING),
    TAILORING("Tailoring", "Tailoring", 'Ⓕ', ProfessionType.CRAFTING),
    WOODWORKING("Woodworking", "Woodworking", 'Ⓘ', ProfessionType.CRAFTING),
    JEWELING("Jeweling", "Jeweling", 'Ⓓ', ProfessionType.CRAFTING),
    COOKING("Cooking", "Cooking", 'Ⓐ', ProfessionType.CRAFTING),
    SCRIBLING("Scribing", "Scribing", 'Ⓔ', ProfessionType.CRAFTING),
    ALCHEMISM("Alchemism", "Alchemism", 'Ⓛ', ProfessionType.CRAFTING);

    public final String displayName;
    public final String actionName;
    public final char symbol;
    public final ProfessionType type;

    Profession(String displayName, String actionName, char symbol, ProfessionType type) {
        this.displayName = displayName;
        this.actionName = actionName;
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

    public static boolean professionMentioned(String message) {
        for(Profession p : Profession.values()) {
            if(message.contains(p.displayName)) {
                return true;
            }
        }

        return false;
    }

}
