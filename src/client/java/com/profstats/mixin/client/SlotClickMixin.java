package com.profstats.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import java.util.regex.Pattern;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.craft.ActiveCraft;

@Mixin(ClientPlayerInteractionManager.class)
public class SlotClickMixin {

    private static final Pattern CRAFT_BUTTON_PATTERN = Pattern.compile("§f.§l Craft §r.");


    @Inject(method = "clickSlot", at = @At("HEAD"))
    private void onClickSlot(
            int syncId,
            int slotId,
            int button,
            SlotActionType actionType,
            net.minecraft.entity.player.PlayerEntity player,
            CallbackInfo ci
    ) {
        int startCraftButton = 13;

        // Can also do pickup_all but that means we cancel the craft no matter what state we are in
        if(slotId == startCraftButton && actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
            ItemStack stack = player.currentScreenHandler.getSlot(slotId).getStack();
            String itemName = stack.getName().getString();

            ProfStatsClient statisticMod = ProfStatsClient.getInstance();

            if(CRAFT_BUTTON_PATTERN.matcher(itemName).matches()) {
                MinecraftClient client = MinecraftClient.getInstance();
                Profession profession = Profession.fromDisplayName(client.currentScreen.getTitle().getString());

                ActiveCraft statistic = statisticMod.startNewCraft(profession);
                statistic.setCraftItems(player.currentScreenHandler);
            } else if (itemName.equals("§cCrafting...")) {
                statisticMod.cancelActiveCraft();
            }

        }
    }
}

