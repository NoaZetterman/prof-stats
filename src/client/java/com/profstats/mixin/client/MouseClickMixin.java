package com.profstats.mixin.client;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.gather.GatherScanner;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
public class MouseClickMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMouse(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        // Detect only when left clicking, right clicking is detected in ClientRightClickEvent, as it requires detecting holding the button
        if (action == GLFW.GLFW_PRESS && info.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            GatherScanner.tryDetectGather(info.button());
        }
    }
}
