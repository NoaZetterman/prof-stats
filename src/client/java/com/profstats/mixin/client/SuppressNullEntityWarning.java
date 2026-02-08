package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;

/*
 * Minecraft creates a lot of warnings for invalid entity packets.
 * This suppresses these warnings in the logs to allow debugging.
 * 
 * NOTE: Only meant for development, and should not be used in the final mod
 */
@Mixin(EntityPassengersSetS2CPacket.class)
public class SuppressNullEntityWarning {
    @Shadow private int entityId;

    @Inject(
      method = "apply(Lnet/minecraft/network/listener/ClientPlayPacketListener;)V",
      at = @At("HEAD"),
      cancellable = true
    )
    private void onApply(net.minecraft.network.listener.ClientPlayPacketListener listener, CallbackInfo ci) {
        if (listener instanceof ClientPlayNetworkHandler handler) {
            if (handler.getWorld() != null && handler.getWorld().getEntityById(entityId) == null) {
                ci.cancel();
            }
        }
    }
}
