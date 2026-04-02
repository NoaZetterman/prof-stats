package com.profstats.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.gather.GuildBoostScanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;

@Mixin(ClientPacketListener.class)
public abstract class InventoryDataMixin {
    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (packet.containerId() == GuildBoostScanner.getSyncId() && GuildBoostScanner.hasActiveScan()) {
            GuildBoostScanner.scanInventory(packet.items());

            Minecraft.getInstance().getConnection()
                .send(new ServerboundContainerClosePacket(packet.containerId()));
        }
    }
}
