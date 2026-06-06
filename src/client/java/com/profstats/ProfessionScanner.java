package com.profstats;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.pendingaction.InteractEntityAction;
import com.profstats.pendingaction.PendingAction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.chat.Component;

public class ProfessionScanner {
    private static final Pattern PROFESSION_LINE_PATTERN = Pattern.compile("Lv\\.\\s*(\\d+)\\s+([A-Za-z]+)");
    private static final Pattern EXP_PATTERN = Pattern.compile("Gathering Experience:\\s*([-+]?\\d+)%");
    private static final Pattern SPEED_PATTERN = Pattern.compile("Gathering Speed:\\s*([-+]?\\d+)%");



    private static final String screenTitle = "\uDAFF\uDFDC\uE003";

    private static boolean shouldTriggerScan = true;
    private static boolean scanInProgress = false;

    private static Runnable onScanCompleteCallback = null;

    private static PendingAction pendingAction;

    private static int syncId = -1;

    public static int getSyncId() {
        return syncId;
    }

    public static void setSyncId(int id) {
        syncId = id;
    }
    public static void stopScan() {
        scanInProgress = false;
    }

    public static boolean hasActiveScan() {
        return scanInProgress;
    }

    public static String getScreenTitle() {
        return screenTitle;
    }

    /*
     * This is called a bit exessively for a few different triggers when the player
     * switches class, or joins with a new class.
     * 
     * It should be safe to call this too often, it's better to trigger a scan extra than miss one and not know current character level.
     * There are probably better ways to track the state, but all depend on detecting data defined by Wynncraft.
     * This means it can change at an update, so it is not worth to spend time to build such perfect system if it may get changed.
     * 
     */
    public static void startWaitForScan() {
        shouldTriggerScan = true;
    }

    public static boolean shouldTriggerScan() {
        return shouldTriggerScan;
    }

    public static void addToPendingActionQueue(Packet<?> p) {
        if(pendingAction != null && pendingAction instanceof InteractEntityAction) {
            ((InteractEntityAction) pendingAction).addToQueue((ServerboundInteractPacket) p);
        }
    }

    public static void attemptTriggerScan(PendingAction pa) {
        if (!shouldTriggerScan) return;
        triggerScan(pa, () -> {});
    }

    public static void triggerScan(PendingAction pa, Runnable callback) {
        shouldTriggerScan = false;

        pendingAction = pa;
        onScanCompleteCallback = callback;

        Minecraft minecraft = Minecraft.getInstance();

        int compassSlot = 43;
        
        if (minecraft.gameMode != null && minecraft.player != null && scanInProgress == false) {
            minecraft.player.closeContainer();
            scanInProgress = true;
            AbstractContainerMenu menu = minecraft.player.containerMenu;

            // Assumes we are not already in an inventory,
            // if actions that are done inside an inventory is used, this will not work
            minecraft.gameMode.handleInventoryMouseClick(
                menu.containerId,
                compassSlot,
                0,
                ClickType.CLONE,
                minecraft.player
            );
        }
    }

    public static void scanScreen(List<ItemStack> items) {
        ProfessionScanner.syncId = -1;

        ItemStack professionStack = items.get(17);
        boolean scanned = parseProfessions(professionStack);

        findIdentifications(items.get(7));

        scanInProgress = false;

        Minecraft minecraft = Minecraft.getInstance();

        minecraft.player.closeContainer();
        pendingAction.execute(minecraft);

        // Execute the callback if one was provided
        if (onScanCompleteCallback != null) {
            try {
                onScanCompleteCallback.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onScanCompleteCallback = null; // Clear it to prevent memory leaks
            }
        }
        
        // Try to scan at next opportunity if we failed to find the items this time
        shouldTriggerScan = !scanned;
    }

    private static boolean parseProfessions(ItemStack professionItemStack) {
        if (professionItemStack.isEmpty()) return false;
        
        boolean scanned = false;
        Minecraft minecraft = Minecraft.getInstance();
        
        List<Component> tooltip = professionItemStack.getTooltipLines(
            TooltipContext.of(minecraft.level), 
            minecraft.player, 
            TooltipFlag.Default.NORMAL
        );

        for (Component line : tooltip) {
            String s = line.getString();

            Matcher m = PROFESSION_LINE_PATTERN.matcher(s);

            if(m.find()) {
                int level = Integer.parseInt(m.group(1));
                Profession profession = Profession.fromDisplayName(m.group(2));

                UserData.setProfessionLevel(profession, level);
                scanned = true; // If we can scan one, we are in the right itemstack and we assume all can be scanned
            }
        }

        return scanned;
    }

    private static boolean findIdentifications(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        
        boolean scanned = false;
        Minecraft minecraft = Minecraft.getInstance();
        
        List<Component> tooltip = itemStack.getTooltipLines(
            TooltipContext.of(minecraft.level), 
            minecraft.player, 
            TooltipFlag.Default.NORMAL
        );

        UserData.resetProfessionIdentifications();
        for (Component line : tooltip) {
            String s = line.getString();

            Matcher expMatcher = EXP_PATTERN.matcher(s);
            if (expMatcher.find()) {
                UserData.setProfessionXpBoost(Integer.parseInt(expMatcher.group(1)));
            }

            Matcher speedMatcher = SPEED_PATTERN.matcher(s);
            if (speedMatcher.find()) {
                UserData.setProfessionSpeedBoost(Integer.parseInt(speedMatcher.group(1)));
            }
        }

        return scanned;
    }
}
