package com.profstats.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.UserData;
import com.profstats.craft.ActiveCraft;
import com.profstats.gather.ActiveGather;
import com.profstats.gather.GuildBoostScanner;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatPacketMixin {
    private String lastMessage = "";

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String message = packet.content().getString();

        // Avoid double messages, it's very unlikely two crafts get the same xp after one another with no other messages in between
        if(message.equals(lastMessage)) {
            return;
        }

        lastMessage = message;

        // Always sent when the player joins with a class or from the lobby
        // (usually not displayed as wynntils removes the message)
        if(message.contains("  §6§lWelcome to Wynncraft!")) {
            UserData.reset();
            ProfStatsClient.getInstance().waitForCharacterData();
            return;
        } 
        
        // Detect if player has hunted active or not (shown in chat when joining and when using /pvp)
        if (
            message.contains("  §4§lYou are currently in hunted mode (PvP on)!") ||
            message.contains("  §4You have enabled hunted mode (PvP on)!")
        ) {
            UserData.setPvpActive(true);
            return;
        }
        
        if (message.contains("§4You have disabled hunted mode (PvP off)!")) {
            UserData.setPvpActive(false);
            return;
        }

        if (
            GuildBoostScanner.hasActiveScan() && 
            (message.contains("You must be standing inside of the territory to view.") || message.contains("Your guild must own the territory to view details") || message.contains("You must be in a guild to use this."))
        ) {
            ActiveGather.setGuildGxpBoostLevel(0);
            GuildBoostScanner.stopScan();
            ci.cancel();
        }

        if (!Profession.professionMentioned(message)) return;

        UserData.trySetProfessionLevelFromMessage(message);

        ProfStatsClient statisticMod = ProfStatsClient.getInstance();
        ActiveCraft craft = statisticMod.getActiveCraft();

        if(craft == null) return;

        craft.tryExtractCraftReward(message);
    }
}
