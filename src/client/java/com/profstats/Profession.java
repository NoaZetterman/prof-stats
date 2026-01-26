package com.profstats;

import java.util.Arrays;

enum ProfessionType {
    CRAFTING,
    GATHERING
}

public enum Profession {
    WOODCUTTING("Woodcutting", 'Ⓒ', ProfessionType.GATHERING),
    MINING("Mining", 'Ⓑ', ProfessionType.GATHERING),
    FARMING("Farming", 'Ⓙ', ProfessionType.GATHERING),
    FISHING("Fishing", 'Ⓚ', ProfessionType.GATHERING),

    ARMOURING("Armouring", 'Ⓗ', ProfessionType.CRAFTING),
    WEAPONSMITHING("Weaponsmithing", 'Ⓖ', ProfessionType.CRAFTING),
    TAILORING("Tailoring", 'Ⓕ', ProfessionType.CRAFTING),
    WOODWORKING("Woodworking", 'Ⓘ', ProfessionType.CRAFTING),
    JEWELING("Jeweling", 'Ⓓ', ProfessionType.CRAFTING),
    COOKING("Cooking", 'Ⓐ', ProfessionType.CRAFTING),
    SCRIBLING("Scribing", 'Ⓔ', ProfessionType.CRAFTING),
    ALCHEMISM("Alchemism", 'Ⓛ', ProfessionType.CRAFTING);

    public final String displayName;
    public final char symbol;
    public final ProfessionType type;

    Profession(String displayName, char symbol, ProfessionType type) {
        this.displayName = displayName;
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
