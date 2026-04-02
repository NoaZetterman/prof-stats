package com.profstats.mixin.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Required to read footer for status effects
@Mixin(PlayerTabOverlay.class)
public interface PlayerListHudAccessor {
    @Accessor("footer")
    Component getFooter();

    @Accessor("header")
    Component getHeader();
}
