package com.profstats.gather;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.Profession;
import com.profstats.ProfessionScanner;
import com.profstats.pendingaction.NoAction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class GatherScanner {

    public static void tryDetectGather(int button) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) return;
        if (client.level == null) return;
        if (client.screen != null) return;

        ItemStack mainHandItem = player.getMainHandItem();

        if (mainHandItem.isEmpty()) return;

        String itemName = mainHandItem.getHoverName().getString();
        if (!itemName.contains("Gathering")) return;

        if(ProfessionScanner.shouldTriggerScan()) {
            ProfessionScanner.attemptTriggerScan(new NoAction());
        }

        if (itemName.startsWith("\uDAFC\uDC00Gathering Scythe")) {
            detectGather(player, mainHandItem, Profession.FARMING);
        } else if (itemName.startsWith("\uDAFC\uDC00Gathering Rod")) {
            detectGather(player, mainHandItem, Profession.FISHING);
        } else if (itemName.startsWith("\uDAFC\uDC00Gathering Pickaxe")) {
            detectGather(player, mainHandItem, Profession.MINING);
        } else if (itemName.startsWith("\uDAFC\uDC00Gathering Axe")) {
            detectGather(player, mainHandItem, Profession.WOODCUTTING);
        }
    }

    private static void detectGather(LocalPlayer player, ItemStack itemStack, Profession profession) {
        Level world = player.level();

        if (player == null || world == null) return;

        Map<Vec3, Integer> locations = findLocations(player, profession);

        List<Component> tooltip = itemStack.getTooltipLines(TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);

        
        if (locations.size() > 0) {
            new ActiveGather(profession, locations, tooltip);
        }
    }

    private static Map<Vec3, Integer> findLocations(LocalPlayer player, Profession profession) {
        
        Level world = player.level();

        Vec3 playerPosition = player.getEyePosition(1.0f);
        AABB box = new AABB(playerPosition, playerPosition).inflate(6.0f);

        List<TextDisplay> displays =
            world.getEntitiesOfClass(
                TextDisplay.class,
                box,
                e -> true
        );

        Map<Vec3, Integer> locations = new HashMap<>();

        Pattern level_pattern = Pattern.compile(profession.displayName + " Lv\\. Min: (?:§f)?(\\d+)"); // Could also add some more to this

        // Find and detect all prof nodes and their respective level
        displays.forEach((displayEntity) -> {
            if (displayEntity.getText().getString().contains(profession.displayName)) {
                Matcher m = level_pattern.matcher(displayEntity.getText().getString());
                if (m.find()) {
                    locations.put(displayEntity.position(), Integer.parseInt(m.group(1)));
                }
            }
        });

        return locations;
    }
}
