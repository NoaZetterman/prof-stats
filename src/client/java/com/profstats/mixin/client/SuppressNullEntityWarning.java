package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

/*
 * Minecraft creates a lot of warnings for invalid entity packets.
 * This suppresses these warnings in the logs to allow debugging.
 * 
 * NOTE: Only meant for development, and should not be used in the final mod
 */
@Mixin(ClientboundSetPassengersPacket.class)
public class SuppressNullEntityWarning {
    @Shadow private int vehicle;

    @Inject(
      method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V",
      at = @At("HEAD"),
      cancellable = true
    )
    private void onApply(ClientGamePacketListener listener, CallbackInfo ci) {
        if (listener instanceof ClientPacketListener handler) {
            if (handler.getLevel() != null && handler.getLevel().getEntity(vehicle) == null) {
                ci.cancel();
            }
        }
    }
}
