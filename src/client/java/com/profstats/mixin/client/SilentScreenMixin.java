package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.gather.GuildBoostScanner;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class SilentScreenMixin {
    @Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        String screenTitle = packet.getName().getString();

        if (GuildBoostScanner.hasActiveScan() && ProfStatsClient.getInstance().getTerritoryManager().isTerritory(screenTitle)) {
            GuildBoostScanner.setSyncId(packet.getSyncId());
            
            // Cancel the packet so MinecraftClient.setScreen() is never called
            ci.cancel(); 
        }
    }
}
