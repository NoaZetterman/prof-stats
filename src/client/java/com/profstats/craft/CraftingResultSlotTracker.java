package com.profstats.craft;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

/*
 * Track current state of itemstacks
 * 
 * Used to detect when a new item is spawned in the craft GUI, in other words when a new crafted item appears.
 */
public class CraftingResultSlotTracker {

    private static final Set<Integer> RESULT_SLOTS = Set.of(
        5,6,7,8, 14,15,16,17, 23,24,25,26
    );
    
    private static final Map<Integer, ItemStack> lastStacks = new HashMap<>();

    public static void setSlots(ScreenHandler screenHandler) {
        for(int i : RESULT_SLOTS) {
            setSlot(i, screenHandler.getSlot(i).getStack().copy());
        }
    }

    public static void setSlot(int slot, ItemStack stack) {
        lastStacks.put(slot, stack.copy());
    }

    public static ItemStack getSlot(int slot) {
        return lastStacks.getOrDefault(slot, ItemStack.EMPTY);
    }
}
