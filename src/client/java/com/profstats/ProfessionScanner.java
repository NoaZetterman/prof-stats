package com.profstats;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profstats.pendingaction.InteractEntityAction;
import com.profstats.pendingaction.PendingAction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class ProfessionScanner {
    private static final Pattern PROFESSION_LINE_PATTERN = Pattern.compile("Lv\\.\\s*(\\d+)\\s+([A-Za-z]+)");

    private static final String screenTitle = "\uDAFF\uDFDC\uE003";

    private static boolean shouldTriggerScan = true;
    private static boolean scanInProgress = false;

    private static PendingAction pendingAction;

    public static boolean isScanInProgress() {
        return scanInProgress;
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
            ((InteractEntityAction) pendingAction).addToQueue((PlayerInteractEntityC2SPacket) p);
        }
    }

    // TODO: This can possibly use the other method of listening to packets (see GuildBoostScanner usage)
    public static void attemptTriggerScan(PendingAction pa) {
        if (!shouldTriggerScan) return;
        shouldTriggerScan = false;

        pendingAction = pa;

        MinecraftClient client = MinecraftClient.getInstance();

        int compassSlot = 43;
        scanInProgress = true;

        // Assumes we are not already in an inventory,
        // if actions that are done inside an inventory is used, this will not work
        client.interactionManager.clickSlot(
            client.player.currentScreenHandler.syncId,
            compassSlot,
            0,
            SlotActionType.PICKUP,
            client.player
        );
    }

    public static void tryScanScreen(Screen screen) {
        if(!scanInProgress) return;
        if (!(screen instanceof HandledScreen<?> hs)) return;

        String title = screen.getTitle().getString();
        if (!screenTitle.equals(title)) {
            return;
        }

        // We must wait a bit for the items to be loaded in the inventory, this waits a little
        // It's may be more robust to wait for the packet providing the itemstacks instead, but this seems to work too.
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean scanned = parseProfessions(hs.getScreenHandler().getSlot(17).getStack());

            client.player.closeHandledScreen();
            pendingAction.execute(client);
            // Try to scan at next opportunity if we failed to find the items this time
            shouldTriggerScan = !scanned;
        });

        scanInProgress = false;
    }

    private static boolean parseProfessions(ItemStack professionItemStack) {
        boolean scanned = false;
        List<Text> tooltip = professionItemStack.getTooltip(TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
        for (Text line : tooltip) {
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
}