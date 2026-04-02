package com.profstats.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.item.ItemStack;

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
@Mixin(ClientPacketListener.class)
public class SlotUpdateMixin {
    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void onSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen == null) return;

        String title = minecraft.screen.getTitle().getString();
        if(Profession.fromDisplayName(title) == null) return;

        ProfStatsClient statisticMod = ProfStatsClient.getInstance();
        if (statisticMod.getActiveCraft() == null) return;

        int slot = packet.getSlot();

        ItemStack newStack = packet.getItem();
        ItemStack oldStack = CraftingResultSlotTracker.getSlot(slot);

        if (oldStack != ItemStack.EMPTY && !ItemStack.isSameItemSameComponents(oldStack, newStack)) {
            statisticMod.finishActiveCraft(newStack, minecraft.player);
            CraftingResultSlotTracker.setSlot(slot, newStack);
        }
    }
}
