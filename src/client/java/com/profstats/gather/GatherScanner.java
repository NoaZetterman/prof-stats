package com.profstats.gather;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.Profession;
import com.profstats.ProfessionScanner;
import com.profstats.pendingaction.NoAction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GatherScanner {

    public static void tryDetectGather(int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;
        if (client.world == null) return;
        if (client.currentScreen != null) return;

        ItemStack mainHandItem = player.getMainHandStack();

        if (mainHandItem == null) return;

        String itemName = mainHandItem.getName().getString();
        if (!itemName.contains("Gathering")) return;

        if(ProfessionScanner.shouldTriggerScan()) {
            ProfessionScanner.attemptTriggerScan(new NoAction());
        }

        if (itemName.startsWith("\uE000 Gathering Scythe")) {
            detectGather(player, mainHandItem, Profession.FARMING);
        } else if (itemName.startsWith("\uE001 Gathering Rod")) {
            detectGather(player, mainHandItem, Profession.FISHING);
        } else if (itemName.startsWith("\uE002 Gathering Pickaxe")) {
            detectGather(player, mainHandItem, Profession.MINING);
        } else if (itemName.startsWith("\uE003 Gathering Axe")) {
            detectGather(player, mainHandItem, Profession.WOODCUTTING);
        }
    }

    private static void detectGather(ClientPlayerEntity player, ItemStack itemStack, Profession profession) {
        World world = player.getWorld();

        if (player == null || world == null) return;

        Map<Vec3d, Integer> locations = findLocations(player, profession);

        List<Text> tooltip = itemStack.getTooltip(TooltipContext.DEFAULT, player, TooltipType.BASIC);

        
        if (locations.size() > 0) {
            new ActiveGather(profession, locations, tooltip);
        }
    }

    private static Map<Vec3d, Integer> findLocations(ClientPlayerEntity player, Profession profession) {
        
        World world = player.getWorld();

        Vec3d playerPosition = player.getCameraPosVec(1.0f);
        Box box = new Box(playerPosition, playerPosition).expand(5.0f);

        List<TextDisplayEntity> displays =
            world.getEntitiesByClass(
                TextDisplayEntity.class,
                box,
                e -> true
        );

        Map<Vec3d, Integer> locations = new HashMap<>();

        Pattern level_pattern = Pattern.compile(profession.displayName + " Lv\\. Min: (?:§f)?(\\d+)"); // Could also add some more to this

        // Find and detect all prof nodes and their respective level
        displays.forEach((displayEntity) -> {
            if (displayEntity.getText().getString().contains(profession.displayName)) {
                Matcher m = level_pattern.matcher(displayEntity.getText().getString());
                if (m.find()) {
                    locations.put(displayEntity.getPos(), Integer.parseInt(m.group(1)));
                }
            }
        });

        return locations;
    }
}