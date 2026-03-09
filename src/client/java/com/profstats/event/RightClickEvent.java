package com.profstats.event;

import org.lwjgl.glfw.GLFW;

import com.profstats.gather.GatherScanner;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class RightClickEvent {
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(RightClickEvent::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) return;

        long window = client.getWindow().getHandle();
        
        // Detect when either clicking or holding rightclick. Left click is detected using the MouseClickMixin
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) {
            GatherScanner.tryDetectGather(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        }
    }
}
