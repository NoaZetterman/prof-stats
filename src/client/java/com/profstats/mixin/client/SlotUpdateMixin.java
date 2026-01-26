package com.profstats.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.craft.CraftingResultSlotTracker;

/*
 * Scans for updates in profession screens where crafted items are placed,
 * to detect the profession level of the crafted item 
 * and to know that the craft is finished
 */
@Mixin(ClientPlayNetworkHandler.class)
public class SlotUpdateMixin {
    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Ignore when we are not in a crafting screen
        if (client.currentScreen == null) return;

        String title = client.currentScreen.getTitle().getString();
        if(Profession.fromDisplayName(title) == null) return;

        ProfStatsClient statisticMod = ProfStatsClient.getInstance();
        if (statisticMod.getActiveCraft() == null) return;

        int slot = packet.getSlot();

        // Track 
        ItemStack newStack = packet.getStack();
        ItemStack oldStack = CraftingResultSlotTracker.getSlot(slot);

        if (oldStack != ItemStack.EMPTY && !ItemStack.areEqual(oldStack, newStack)) {
            ItemStack craftedItem = newStack;
            statisticMod.finishActiveCraft(craftedItem, client.player);
            CraftingResultSlotTracker.setSlot(slot, newStack);
        }

    }
}