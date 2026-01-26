package com.profstats.pendingaction;

import net.minecraft.client.MinecraftClient;

public class NoAction extends PendingAction {
    @Override
    public void execute(MinecraftClient client) {
        return;
    }
}
