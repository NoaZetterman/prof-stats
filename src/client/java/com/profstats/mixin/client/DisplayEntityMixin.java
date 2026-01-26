package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.gather.ActiveGather;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public class DisplayEntityMixin {

    @Inject(
        method = "onEntityTrackerUpdate",
        at = @At("TAIL")
    )
    private void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Entity entity = mc.world.getEntityById(packet.id());

        // Try to detect gather when entity is updated
        ActiveGather.detectGather(entity);
    }
}
