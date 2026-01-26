package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.gather.GuildBoostScanner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class InventoryDataMixin {
    @Inject(method = "onInventory", at = @At("HEAD"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        if (packet.getSyncId() == GuildBoostScanner.getSyncId() && GuildBoostScanner.hasActiveScan()) {
            GuildBoostScanner.scanInventory(packet.getContents());

            MinecraftClient.getInstance().getNetworkHandler()
                .sendPacket(new CloseHandledScreenC2SPacket(packet.getSyncId()));
        }
    }
}

