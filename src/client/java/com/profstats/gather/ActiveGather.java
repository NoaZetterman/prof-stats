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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.ProfessionScanner;
import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.UserData;
import com.profstats.pendingaction.NoAction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class ActiveGather {
    private static final Pattern GATHER_XP_PATTERN = Pattern.compile(
        "(?:\\[x(\\d+(?:\\.\\d+)?)\\]\\s*)?" + // Xp multiplier part
        "\\+(\\d+)\\s+[^ ]+\\s+([A-Za-z]+) XP \\[(\\d+(?:\\.\\d+)?)%\\]$"
    );

    private static final Pattern GATHER_COOLDOWN_PATTERN = Pattern.compile("^§\\d+s$");

    private static final Pattern TOOL_TIER_PATTERN = Pattern.compile("[a-zA-Z]+ T(\\d+)$");
    private static final Pattern TOOL_SPEED_PATTERN = Pattern.compile("(\\d+) Gathering Speed");
    private static final Pattern TOOL_DURABILITY_PATTERN = Pattern.compile("Durability (\\d+)\\/(\\d+)$");

    // When we detect a click to start crafting, any hologram nearby is considered, these are placed in a queue and processed.
    // The gathers live here for a few ticks, until considered stale or until node is found in ticking state.
    private static List<ActiveGather> potentialGathers = new ArrayList<ActiveGather>();

    // Once the node currently getting processed is found, they'll be put in a map here
    // A map is required because one gather may not be finished when the next is started.
    // This is because item gatehred is detected from the  display entity, and is not displayed directly when finished.
    private static Map<Vec3, ActiveGather> activeGathers = new HashMap<Vec3, ActiveGather>();
    

    private Map<Vec3, Integer> potentialLocations;
    private Vec3 location;

    private GatherState state = GatherState.INITIAL;


    private Profession profession;
    private Integer professionLevel;

    private Integer totalXp;

    private Double xpMultiplier; 

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

    public ActiveGather(Profession profession, Map<Vec3, Integer> potentialLocations, List<Component> gatheringToolTooltip) {
        this.profession = profession;
        this.potentialLocations = potentialLocations;
        this.createdAt = Instant.now();
        this.professionLevel = UserData.getProfessionLevel(profession);


        if (gatheringToolTooltip.size() != 9) return; // Skip adding object if tooltip is incorrect

        setToolData(gatheringToolTooltip);

        potentialGathers.add(this);
    }

    public static void reset() {
        activeGathers = new HashMap<Vec3, ActiveGather>();
        potentialGathers = new ArrayList<ActiveGather>();
    }

    public static void detectGather(Entity entity) {
        if (!(entity instanceof TextDisplay displayEntity)) return;

        Vec3 entityPosition = entity.position();

        detectPotentialLocations(entityPosition, displayEntity);

        ActiveGather activeGather = activeGathers.get(entityPosition);

        if (activeGather == null) return;

        activeGather.readGatherHologram(displayEntity);

        if (activeGather.isCompleted()) {
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
    private static void detectPotentialLocations(Vec3 entityPosition, TextDisplay displayEntity) {
        ListIterator<ActiveGather> it = potentialGathers.listIterator();
        String hologramText = displayEntity.getText().getString();


        while (it.hasNext()) {
            ActiveGather activeGather = it.next();

            if(activeGather.isOld()) {
                it.remove();
                continue;
            }

            // Sometimes hologram moves when the ticks start, we must detect that new location.
            if (hologramText.contains("\n" + activeGather.profession.symbol + " " + activeGather.profession.actionName)) {
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
                activeGather.setLocationData(entityPosition);

                activeGathers.put(entityPosition, activeGather);
                it.remove();
            }   
        }
    }

    public Profession getProfession() {
        return profession;
    }

    public void setLocationData(Vec3 location) {
        this.location = location;
        if(this.potentialLocations != null) {
            if (potentialLocations.keySet().contains(location)) {
                this.nodeLevel = potentialLocations.get(location);
            } else {
                // If the hologram detected was not visible when starting the gather,
                // guess the level, except for a few edge-cases,
                // all detected locations near the node should be the same level.
                Iterator<Integer> it = potentialLocations.values().iterator();
                this.nodeLevel = it.next();
                
                while (it.hasNext()) {
                    if (it.next() != this.nodeLevel) {
                        this.nodeLevel = null;
                        break;
                    }
                }
            }
        }

        this.potentialLocations = null;
    }

    public boolean isCompleted() {
        return state == GatherState.XP_GAIN || state == GatherState.COMPLETED_COOLDOWN || state == GatherState.INITIAL;
    }

    public void readGatherHologram(TextDisplay text) {
        String hologramText = text.getText().getString();

        if (hologramText.equals("")) return;

        if (detectTick(hologramText)) return;

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
        if (gatherXpModifier != null && !gatherXpModifier.equals(UserData.getProfessionXpBoost())) {
            gatherXpModifier = null;
        }

        if (gatherSpeedModifier != null && !gatherSpeedModifier.equals(UserData.getProfessionSpeedBoost())) {
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
                    String csvHeaders = "timestamp,profession,profession_level,level_percent,node_level,";
                    csvHeaders += "total_xp,xp_multiplier,xp_modifier,pvp_active,guild_gxp_boost_level,speed_modifier,ticks,";
                    csvHeaders += "tool_tier,tool_speed,tool_durability,";
                    csvHeaders += "material_tier,material_count,material_name\n";

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
        // TODO: Possibly figure out how to check the number in the middle as well.
        if (!hologramText.contains("\n" + profession.symbol + " " + profession.actionName)) return false;

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

            // Prevent scanning more than once
            if (isPvpActive == null) {
                initialDetectModifiers();
            }
        } else {
            ticks += 1;
        }

        return true;
    }

    private void initialDetectModifiers() {
        gatherXpModifier = null;
        gatherSpeedModifier = null;
        isPvpActive = UserData.isPvpActive();

        // Skip guidl boost scan when copied over from what was detected in previous gather
        if (guildGxpBoostLevel == null) {
            // TODO: Read xp from new gu boost UI, this will only detect level 0 when not in a territory owned by the players guild.
            GuildBoostScanner.triggerScan(() -> {
                ProfessionScanner.triggerScan(new NoAction(), () -> {
                    this.professionLevel = UserData.getProfessionLevel(profession);
                    this.gatherXpModifier = UserData.getProfessionXpBoost();
                    this.gatherSpeedModifier = UserData.getProfessionSpeedBoost();
                });
            });
        } else {
            ProfessionScanner.triggerScan(new NoAction(), () -> {
                this.professionLevel = UserData.getProfessionLevel(profession);
                this.gatherXpModifier = UserData.getProfessionXpBoost();
                this.gatherSpeedModifier = UserData.getProfessionSpeedBoost();
            });
        }
    }

    
    private boolean detectGatherXp(String hologramText) {
        Matcher m = GATHER_XP_PATTERN.matcher(hologramText);

        if (!m.find()) return false;

        // Only do this first iteration
        if (totalXp == null && guildGxpBoostLevel != null) {
            GuildBoostScanner.triggerScan(() -> {
                ProfessionScanner.triggerScan(new NoAction(), () -> {
                    store();
                });
            });
        }

        String multiplier_string = m.group(1);
        xpMultiplier = multiplier_string != null ?  Double.parseDouble(multiplier_string) : 1;
        totalXp = Integer.parseInt(m.group(2));
        levelPercent = Double.parseDouble(m.group(4));
        state = GatherState.XP_GAIN;

        return true;
    }


    private boolean detectCooldown(String hologramText) {
        Matcher m = GATHER_COOLDOWN_PATTERN.matcher(hologramText);
        if (!m.find()) return false;

        state = GatherState.COMPLETED_COOLDOWN;
        return true;
    }


    private void setToolData(List<Component> gatheringToolTooltip) {
        String tierLine = gatheringToolTooltip.get(1).getString();
        String speedLine = gatheringToolTooltip.get(4).getString();
        String durabilityLine = gatheringToolTooltip.get(5).getString();

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
