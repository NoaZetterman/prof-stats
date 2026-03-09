package com.profstats.mixin.client;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.gather.GatherScanner;

import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public class MouseClickMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
        // Detect only when left clicking, right clicking is detected in ClientRightClickEvent, as it requires detecting holding the button
        if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            GatherScanner.tryDetectGather(button);
        }
    }
}
