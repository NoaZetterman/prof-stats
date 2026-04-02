package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.gather.GuildBoostScanner;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;

@Mixin(ClientPacketListener.class)
public abstract class SilentScreenMixin {
    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        String screenTitle = packet.getTitle().getString();

        if (GuildBoostScanner.hasActiveScan() && ProfStatsClient.getInstance().getTerritoryManager().isTerritory(screenTitle)) {
            GuildBoostScanner.setSyncId(packet.getContainerId());
            
            // Cancel the packet so MinecraftClient.setScreen() is never called
            ci.cancel(); 
        }
    }
}
