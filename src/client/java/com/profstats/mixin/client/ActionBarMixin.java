package com.profstats.mixin.client;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;

@Mixin(InGameHud.class)
public class ActionBarMixin {
    private static final String HOTBAR_STRING = "\uDAFF\uDF98\uE00A\uDAFF\uDFFF\uDAFF\uDF98";
    private static boolean isHotbar = false;

    @Inject(method = "setOverlayMessage", at = @At("TAIL"))
    private void onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        String messageStr = message.getString();

        // Detect if we have selected a character and are in game.
        if (messageStr.contains(HOTBAR_STRING) || messageStr.equals("")) {
            if(!isHotbar) {
                ProfStatsClient.getInstance().waitForCharacterData();
                isHotbar = true;
            } 
        } else {
            isHotbar = false;
        }
    }
}