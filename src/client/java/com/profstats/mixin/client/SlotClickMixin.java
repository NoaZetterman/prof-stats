package com.profstats.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import java.util.regex.Pattern;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.ProfStatsClient;
import com.profstats.Profession;
import com.profstats.craft.ActiveCraft;

@Mixin(MultiPlayerGameMode.class)
public class SlotClickMixin {

    private static final Pattern CRAFT_BUTTON_PATTERN = Pattern.compile("§f.§l Craft §r.");

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"))
    private void onClickSlot(
            int containerId,
            int slotId,
            int button,
            ClickType clickType,
            net.minecraft.world.entity.player.Player player,
            CallbackInfo ci
    ) {
        int startCraftButton = 13;

        // Can also do pickup_all but that means we cancel the craft no matter what state we are in
        if (slotId == startCraftButton && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            ItemStack stack = player.containerMenu.getSlot(slotId).getItem();
            String itemName = stack.getHoverName().getString();

            ProfStatsClient statisticMod = ProfStatsClient.getInstance();

            if (CRAFT_BUTTON_PATTERN.matcher(itemName).matches()) {
                Minecraft minecraft = Minecraft.getInstance();
                Profession profession = Profession.fromScreenName(minecraft.screen.getTitle().getString());

                ActiveCraft statistic = statisticMod.startNewCraft(profession);
                statistic.setCraftItems(player.containerMenu);
            } else if (itemName.equals("§cCrafting...")) {
                statisticMod.cancelActiveCraft();
            }
        }
    }
}
