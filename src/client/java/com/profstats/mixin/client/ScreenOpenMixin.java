package com.profstats.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.ProfessionScanner;

@Mixin(MinecraftClient.class)
public class ScreenOpenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        ProfStatsClient.getInstance().cancelActiveCraft();

        if (screen == null) {
            return;
        }

        // Scans screen when we have an active scan and the screen is the correct one
        ProfessionScanner.tryScanScreen(screen);
        // GuildBoostScanner.tryScanScreen(screen);
    }
}
