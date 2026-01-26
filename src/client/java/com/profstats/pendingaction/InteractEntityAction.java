package com.profstats.pendingaction;

import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class InteractEntityAction extends PendingAction {
    private Queue<PlayerInteractEntityC2SPacket> packetQueue = new LinkedList<PlayerInteractEntityC2SPacket>();

    /*
     * PlayerInteractEntityC2SPacket is triggered when right-clicking on an entity
     * This includes when interacting with profession stations
     */
    public InteractEntityAction(PlayerInteractEntityC2SPacket packet) {
        packetQueue.add(packet);
    }

    @Override
    public void execute(MinecraftClient client) {
        while(!packetQueue.isEmpty()) {
            client.getNetworkHandler().sendPacket(
                this.packetQueue.remove()
            );
        }
    }

    public void addToQueue(PlayerInteractEntityC2SPacket packet) {
        packetQueue.add(packet);
    }
}
