package com.profstats;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserData {
    private final static Pattern LEVEL_UP_PATTERN = Pattern.compile("You are now level (\\d+) in §f.?§e ([A-Za-z]+)"); 

    private static HashMap<Profession, Integer> levelForProfession = new HashMap<Profession,Integer>();
    private static boolean isPvpActive = false;


    public static Integer getProfessionLevel(Profession profession) {
        return levelForProfession.get(profession);
    }

    public static void setProfessionLevel(Profession profession, int level) {
        levelForProfession.put(profession, level);
    }

    public static void setPvpActive(boolean isPvpActive) {
        UserData.isPvpActive = isPvpActive;
    }

    public static boolean isPvpActive() {
        return isPvpActive;
    }

    public static void trySetProfessionLevelFromMessage(String message) {
        Matcher m = LEVEL_UP_PATTERN.matcher(message);

        if(m.find()) {
            int level = Integer.parseInt(m.group(1));
            Profession profession = Profession.fromDisplayName(m.group(2));
            
            levelForProfession.put(profession, level);
        }
    }

    public static void reset() {
        levelForProfession = new HashMap<Profession,Integer>();
        isPvpActive = false;
    }
}
