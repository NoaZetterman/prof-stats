package com.profstats.pendingaction;

import java.util.LinkedList;
import java.util.Queue;

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
        if (minecraft.getConnection() != null) {
            while(!packetQueue.isEmpty()) {
                minecraft.getConnection().send(
                    this.packetQueue.remove()
                );
            }
        }
    }

    public void addToQueue(ServerboundInteractPacket packet) {
        packetQueue.add(packet);
    }
}
