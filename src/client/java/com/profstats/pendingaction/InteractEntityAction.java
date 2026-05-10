package com.profstats.pendingaction;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

public class InteractEntityAction extends PendingAction {
    private Queue<ServerboundInteractPacket> packetQueue = new LinkedList<>();

    /*
     * ServerboundInteractPacket is triggered when right-clicking on an entity
     * This includes when interacting with profession stations
     */
    public InteractEntityAction(ServerboundInteractPacket packet) {
        packetQueue.add(packet);
    }

    @Override
    public void execute(Minecraft minecraft) {
        // Must delay a bit to ensure the other screen is properly closed
        CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS).execute(() -> {
            minecraft.execute(() -> {
                if (minecraft.getConnection() != null) {
                    while(!packetQueue.isEmpty()) {
                        minecraft.getConnection().send(
                            this.packetQueue.remove()
                        );
                    }
                }
            });
        });
    }

    public void addToQueue(ServerboundInteractPacket packet) {
        packetQueue.add(packet);
    }
}
