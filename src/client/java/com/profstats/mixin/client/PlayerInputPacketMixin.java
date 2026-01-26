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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.text.Text;

@Mixin(ClientCommonNetworkHandler.class)
public class PlayerInputPacketMixin {
    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof PlayerInteractEntityC2SPacket p) {
            if(ProfessionScanner.isScanInProgress()) {
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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;

        if (world == null) return false;
            
        Entity entity = world.getEntityById(entityId);

        if (entity == null) return false;
        if (entity.getType() != EntityType.INTERACTION) return false;

        List<TextDisplayEntity> displayEntities = world.getEntitiesByClass(
            TextDisplayEntity.class, 
            entity.getBoundingBox().offset(0,1,0), 
            (textDisplay) -> true);


        for(TextDisplayEntity textDisplayEntity : displayEntities) {
            Text displayedText = textDisplayEntity.getText();
            if (displayedText != null) {
                if(Profession.professionMentioned(displayedText.getString())) {
                    return true;
                }
            }
        }

        return false;

    }
}