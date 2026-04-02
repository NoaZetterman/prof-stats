package com.profstats.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.profstats.Profession;
import com.profstats.ProfessionScanner;
import com.profstats.pendingaction.InteractEntityAction;
import com.profstats.pendingaction.PendingAction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.chat.Component;

@Mixin(ClientCommonPacketListenerImpl.class)
public class PlayerInputPacketMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundInteractPacket p) {
            if(ProfessionScanner.hasActiveScan()) {
                ProfessionScanner.addToPendingActionQueue(packet);
                ci.cancel();
            } else if (ProfessionScanner.shouldTriggerScan()) {
                int entityId = ((PlayerInteractEntityC2SPacketAccessor) p).getEntityId();

                if(!isEntityProfessionStation(entityId)) return;

                PendingAction pa = new InteractEntityAction(p);

                ProfessionScanner.attemptTriggerScan(pa); 

                ci.cancel();
            }
        }
    }

    /*
     * Checks if the entity is a profession station by
     * verifying that the displayed profession text is visible right above it;
     * at worst we get false positives and read the profession data too early.
     */
    private boolean isEntityProfessionStation(int entityId) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;

        if (world == null) return false;
            
        Entity entity = world.getEntity(entityId);

        if (entity == null) return false;
        if (entity.getType() != EntityType.INTERACTION) return false;

        List<TextDisplay> displayEntities = world.getEntitiesOfClass(
            TextDisplay.class, 
            entity.getBoundingBox().move(0,1,0), 
            (textDisplay) -> true
        );


        for(TextDisplay textDisplayEntity : displayEntities) {
            Component displayedText = textDisplayEntity.getText();
            if (displayedText != null) {
                if(Profession.professionMentioned(displayedText.getString())) {
                    return true;
                }
            }
        }

        return false;

    }
}
