package com.profstats.pendingaction;

import net.minecraft.client.MinecraftClient;

/*
 * This class should be inherited when we want to intercept the action
 * and do something else before.
 * 
 * The execute method should perform action we intercepted.
 * 
 */
public abstract class PendingAction {
    public abstract void execute(MinecraftClient client);
}
