package com.profstats.mixin.client;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Required to read footer for status effects
@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("footer")
    Text getFooter();

    @Accessor("header")
    Text getHeader();
}