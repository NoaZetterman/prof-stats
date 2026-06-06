package com.profstats;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserData {
    private final static Pattern LEVEL_UP_PATTERN = Pattern.compile("You are now level (\\d+) in §f.?§e ([A-Za-z]+)"); 

    private static HashMap<Profession, Integer> levelForProfession = new HashMap<Profession,Integer>();
    private static boolean isPvpActive = false;

    private static Integer professionXpBoost = null;
    private static Integer professionSpeedBoost = null;

    public static Integer getProfessionLevel(Profession profession) {
        return levelForProfession.get(profession);
    }

    public static Integer getProfessionXpBoost() {
        return professionXpBoost;
    }

    public static Integer getProfessionSpeedBoost() {
        return professionSpeedBoost;
    }

    public static void setProfessionLevel(Profession profession, int level) {
        levelForProfession.put(profession, level);
    }

    public static void setPvpActive(boolean isPvpActive) {
        UserData.isPvpActive = isPvpActive;
    }

    public static void setProfessionXpBoost(int professionXpBoost) {
        UserData.professionXpBoost = professionXpBoost;
    }

    public static void setProfessionSpeedBoost(int professionSpeedBoost) {
        UserData.professionSpeedBoost = professionSpeedBoost;
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

    public static void resetProfessionIdentifications() {
        professionXpBoost = null;
        professionSpeedBoost = null;
    }
}
