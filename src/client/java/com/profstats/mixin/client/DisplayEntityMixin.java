package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.gather.ActiveGather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

@Mixin(ClientPacketListener.class)
public class DisplayEntityMixin {

    @Inject(
        method = "handleSetEntityData",
        at = @At("TAIL")
    )
    private void onEntityTrackerUpdate(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(packet.id());

        // Try to detect gather when entity is updated
        try {
            ActiveGather.detectGather(entity);
        } catch(Exception e) {
            ProfStatsClient.LOGGER.info("[ProfStats] Caught exception when detecting gather:");
            ProfStatsClient.LOGGER.warn(e.getMessage());
            ActiveGather.reset();
        }
    }
}
