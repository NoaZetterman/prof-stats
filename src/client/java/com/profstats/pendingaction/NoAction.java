package com.profstats.pendingaction;

import net.minecraft.client.Minecraft;

public class NoAction extends PendingAction {
    @Override
    public void execute(Minecraft client) {
        return;
    }
}
