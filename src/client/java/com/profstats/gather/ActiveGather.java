package com.profstats.gather;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.UserData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ActiveGather {
    private static final Pattern GATHER_XP_PATTERN = Pattern.compile(
        "(?:\\[x(\\d+)\\]\\s*)?" + // Xp multiplier part
        "\\+(\\d+)\\s+[^ ]+\\s+([A-Za-z]+) XP \\[(\\d+(?:\\.\\d+)?)%\\]$"
    );

    private static final Pattern GATHER_MATERIAL_PATTERN = Pattern.compile(
        "\\+(\\d+)\\s+(.*?)§6 \\[§e(✫*)(?:§8✫*)?§6\\]$"
    );

    private static final Pattern GATHER_COOLDOWN_PATTERN = Pattern.compile("^§\\d+s$");

    private static final Pattern TOOL_TIER_PATTERN = Pattern.compile("^§7Tier (\\d+)$");
    private static final Pattern TOOL_SPEED_PATTERN = Pattern.compile("^§6Gathering Speed: (\\d+)§");
    private static final Pattern TOOL_DURABILITY_PATTERN = Pattern.compile("^§3Gathering Tool §8\\[(\\d+)\\/(\\d+) Durability\\]$");


    private static final Map<Profession, String> tickForProf = Map.of(
        Profession.FARMING,     "[|||Farming|||]",
        Profession.MINING,      "[|||Mining|||]",
        Profession.FISHING,     "[|||Fishing|||]",
        Profession.WOODCUTTING, "[|||Chopping|||]"
    );

    // When we detect a click to start crafting, any hologram nearby is considered, these are placed in a queue and processed.
    // The gathers live here for a few ticks, until considered stale or until node is found in ticking state.
    private static List<ActiveGather> potentialGathers = new ArrayList<ActiveGather>();

    // Once the node currently getting processed is found, they'll be put in a map here
    // A map is required because one gather may not be finished when the next is started.
    // This is because item gatehred is detected from the  display entity, and is not displayed directly when finished.
    private static Map<Vec3d, ActiveGather> activeGathers = new HashMap<Vec3d, ActiveGather>();
    

    private Map<Vec3d, Integer> potentialLocations;
    private Vec3d location;

    private GatherState state = GatherState.INITIAL;


    private Profession profession;
    private Integer professionLevel;

    private Integer totalXp;

    private Integer xpMultiplier; 

    private Integer gatherXpModifier;
    private Integer gatherSpeedModifier;
    private Boolean isPvpActive;
    private Integer guildGxpBoostLevel;

    private Double levelPercent;

    private Integer nodeLevel;
    private int ticks = 0;

    private Integer materialCount;
    private Integer materialTier;
    private String materialName;

    private Integer toolTier;
    private Integer toolSpeed;
    private Integer toolDurability;


    private final Instant createdAt;

    public ActiveGather(Profession profession, Map<Vec3d, Integer> potentialLocations, List<Text> gatheringToolTooltip) {
        this.profession = profession;
        this.potentialLocations = potentialLocations;
        this.createdAt = Instant.now();

        if (gatheringToolTooltip.size() != 8) return; // Skip adding object if tooltip is incorrect

        setToolData(gatheringToolTooltip);
        potentialGathers.add(this);
    }

    public static void reset() {
        activeGathers = new HashMap<Vec3d, ActiveGather>();
        potentialGathers = new ArrayList<ActiveGather>();
    }

    public static void detectGather(Entity entity) {
        if (!(entity instanceof TextDisplayEntity displayEntity)) return;

        Vec3d entityPosition = entity.getPos();

        detectPotentialLocations(entityPosition, displayEntity);

        ActiveGather activeGather = activeGathers.get(entityPosition);

        if (activeGather == null) return;

        activeGather.readGatherHologram(displayEntity);

        if (activeGather.isCompleted()) {
            activeGather.store();
            activeGathers.remove(entityPosition);
        }
    }

    public static void setGuildGxpBoostLevel(Integer boostLevel) {
        for(ActiveGather activeGather : activeGathers.values()) {
            if (activeGather.guildGxpBoostLevel == null) {
                activeGather.guildGxpBoostLevel = boostLevel;
            } else if (activeGather.guildGxpBoostLevel != boostLevel) {
                // When scan before and after diff, we abort saving the boost level as we don't know what was present when receiving the xp
                activeGather.guildGxpBoostLevel = null;
            }
        }
    }

    /*
    * Find node that is currently being gathered, from a list of potential nodes
    */
    private static void detectPotentialLocations(Vec3d entityPosition, TextDisplayEntity displayEntity) {
        ListIterator<ActiveGather> it = potentialGathers.listIterator();
        String hologramText = displayEntity.getText().getString();


        while (it.hasNext()) {
            ActiveGather activeGather = it.next();

            if(activeGather.isOld()) {
                it.remove();
                continue;
            }

            if (activeGather.isAtLocation(entityPosition)) {
                if (hologramText.contains(tickForProf.get(activeGather.getProfession()))) {

                    // Avoid checking guild boost level if we recently checked it,
                    // Also avoids an issue with GuildBoostScanner that would happen
                    // when running two scans almost at the same time
                    Integer guildGxpBoostLevel = null;
                    for (ActiveGather ag : activeGathers.values()) {
                        if (ag.guildGxpBoostLevel != null) {
                            guildGxpBoostLevel = ag.guildGxpBoostLevel;
                        }
                    }
                    activeGather.guildGxpBoostLevel = guildGxpBoostLevel;

                    activeGathers.put(entityPosition, activeGather);
                    it.remove();
                }   
            }
        }
    }

    public Profession getProfession() {
        return profession;
    }

    public boolean isAtLocation(Vec3d location) {
        if (this.location != null && this.location.equals(location)) {
            return true;
        }

        if(this.potentialLocations != null) {
            if (potentialLocations.keySet().contains(location)) {
                this.location = location;
                this.nodeLevel = potentialLocations.get(location);
                this.potentialLocations = null;
                this.professionLevel = UserData.getProfessionLevel(profession);

                return true;
            }
        }

        return false;
    }

    public boolean isCompleted() {
        return state == GatherState.XP_GAIN_WITH_ITEM || state == GatherState.COMPLETED_COOLDOWN || state == GatherState.INITIAL;
    }

    public void readGatherHologram(TextDisplayEntity text) {
        String hologramText = text.getText().getString();

        if (hologramText.equals("")) return;

        if (detectTick(hologramText)) return;

        // detectGatherMaterial must be before detectGatherXp as it contains the same text, but with an additional line
        if (detectGatherMaterial(hologramText)) return; 

        if (detectGatherXp(hologramText)) return;
        if (detectCooldown(hologramText)) return;

        // If none above is found, we are at the initial state.
        state = GatherState.INITIAL;
    }

    public boolean isOld() {
        return Duration.between(createdAt, Instant.now()).getSeconds() > 5;
    }

    public void store() {
        // If any data has changed, we don't know what was applied to the gather. 
        // So it will be cancelled by setting items to null, and hence cancel the entire gather
        if (gatherXpModifier != null && !gatherXpModifier.equals(GatherIdentificationBonus.readGatherXpBonus())) {
            gatherXpModifier = null;
        }

        if (gatherSpeedModifier != null && !gatherSpeedModifier.equals(GatherIdentificationBonus.readGatherSpeedBonus())) {
            gatherSpeedModifier = null;
        }

        if (isPvpActive != null && !isPvpActive.equals(UserData.isPvpActive())) {
            isPvpActive = null;
        }

        try {
            // Create directory in the mod’s config folder
            Path dir = Paths.get("data", "prof-stats");
            Files.createDirectories(dir);

            // Use profession name as filename
            Path filePath = dir.resolve("gathering.csv");

            boolean fileExists = Files.exists(filePath);

            if(
                profession == null || professionLevel == null || nodeLevel == null ||
                xpMultiplier == null || totalXp == null || levelPercent == null ||
                toolTier == null || toolSpeed == null || toolDurability == null || 
                gatherXpModifier == null || isPvpActive == null || gatherSpeedModifier == null || guildGxpBoostLevel == null
            ) {
                // Only log when there's an issue (missing totalXp happens when the gather was cancelled)
                if (totalXp != null) {
                    ProfStatsClient.LOGGER.info("""
                        [ProfStats] Something was null when scanning gather:
                        profession: %s
                        professionLevel: %s
                        totalXp: %s
                        xpMultiplier: %s
                        gatherXpModifier: %s
                        isPvpActive: %s
                        guildGxpBoostLevel: %s
                        gatherSpeedModifier: %s
                        levelPercent: %s
                        nodeLevel: %s
                        toolTier: %s
                        toolSpeed: %s
                        toolDurability: %s
                    """.formatted(profession, professionLevel, totalXp, xpMultiplier, gatherXpModifier, isPvpActive, guildGxpBoostLevel, gatherSpeedModifier, levelPercent, nodeLevel, toolTier, toolSpeed, toolDurability));
                }

                // If these have not been detected, the gather did not finish
                if(profession == null || totalXp == null || nodeLevel == null) {
                    return;
                }
                
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                // Write CSV header if new file
                if (!fileExists) {
                    String csvHeaders = "timestamp,profession,professionLevel,levelPercent,nodeLevel,";
                    csvHeaders += "totalXp,xpMultiplier,xpModifier,pvpActive,guildGxpBoostLevel,speedModifier,ticks,";
                    csvHeaders += "toolTier,toolSpeed,toolDurability,";
                    csvHeaders += "materialTier,materialCount,materialName\n";

                    writer.write(csvHeaders);
                }

                ProfStatsClient.LOGGER.info("[ProfStats] Detected new gather! Writing to '" + filePath.toString() + "'");
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
            toSafeCsv(levelPercent),
            toSafeCsv(nodeLevel),

            toSafeCsv(totalXp),
            toSafeCsv(xpMultiplier),
            toSafeCsv(gatherXpModifier),
            toSafeCsv(isPvpActive),
            toSafeCsv(guildGxpBoostLevel),
            toSafeCsv(gatherSpeedModifier),
            toSafeCsv(ticks),

            toSafeCsv(toolTier), toSafeCsv(toolSpeed), toSafeCsv(toolDurability),
            
            toSafeCsv(materialTier), toSafeCsv(materialCount), toSafeCsv(materialName)
        ) + "\n";
    }

    private String toSafeCsv(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean detectTick(String hologramText) {
        if (!hologramText.contains(tickForProf.get(profession))) return false;

        if (professionLevel == null) {
            this.professionLevel = UserData.getProfessionLevel(profession);
        }

        // This ticking logic is a bit weird.
        // First "tick" is detected 3 times.
        // First of those 3 switches state, the other 2 are detected.
        // However, the last tick is when the next screen shows up, by not counting that tick,
        // we cancel out the extra tick detected at the beginning to get the right amount of ticks.
        if (state == GatherState.INITIAL) {
            state = GatherState.TICKING;
        } else {
            // Prevent scanning more than once
            if (isPvpActive == null) {
                detectModifiers();
            }
            ticks += 1;
        }

        return true;
    }

    private void detectModifiers() {
        gatherXpModifier = GatherIdentificationBonus.readGatherXpBonus();
        gatherSpeedModifier = GatherIdentificationBonus.readGatherSpeedBonus();
        isPvpActive = UserData.isPvpActive();

        // Skip when copied over from what was detected in previous gather
        if (guildGxpBoostLevel == null) {
            GuildBoostScanner.triggerScan();
        }
    }

    
    private boolean detectGatherXp(String hologramText) {
        Matcher m = GATHER_XP_PATTERN.matcher(hologramText);

        if (!m.find()) return false;

        // Only do this first iteration
        if (totalXp == null && guildGxpBoostLevel != null) {
            GuildBoostScanner.triggerScan();
        }

        String multiplier_string = m.group(1);
        xpMultiplier = multiplier_string != null ?  Integer.parseInt(multiplier_string) : 1;
        totalXp = Integer.parseInt(m.group(2));
        levelPercent = Double.parseDouble(m.group(4));
        state = GatherState.XP_GAIN;

        return true;
    }
    private boolean detectGatherMaterial(String hologramText) {
        Matcher m = GATHER_MATERIAL_PATTERN.matcher(hologramText);
        if (!m.find()) return false;

        materialCount = Integer.parseInt(m.group(1));
        materialName = m.group(2).trim();
        materialTier = m.group(3).length();
        state = GatherState.XP_GAIN_WITH_ITEM;

        return true;
    }

    private boolean detectCooldown(String hologramText) {
        Matcher m = GATHER_COOLDOWN_PATTERN.matcher(hologramText);
        if (!m.find()) return false;

        state = GatherState.COMPLETED_COOLDOWN;
        return true;
    }


    private void setToolData(List<Text> gatheringToolTooltip) {
        String tierLine = gatheringToolTooltip.get(1).getString();
        String speedLine = gatheringToolTooltip.get(3).getString();
        String durabilityLine = gatheringToolTooltip.get(7).getString();

        setToolTier(tierLine);
        setToolSpeed(speedLine);
        setToolDurability(durabilityLine);
    }

    private void setToolTier(String tooltipLine) {
        Matcher m = TOOL_TIER_PATTERN.matcher(tooltipLine);
        if (m.find()) toolTier = Integer.parseInt(m.group(1));
    }

    private void setToolSpeed(String tooltipLine) {
        Matcher m = TOOL_SPEED_PATTERN.matcher(tooltipLine);
        if (m.find()) toolSpeed = Integer.parseInt(m.group(1));
    }

    private void setToolDurability(String tooltipLine) {
        Matcher m = TOOL_DURABILITY_PATTERN.matcher(tooltipLine);
        if (m.find()) toolDurability = Integer.parseInt(m.group(1));

    }
}

