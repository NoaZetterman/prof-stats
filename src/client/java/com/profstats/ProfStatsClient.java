package com.profstats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.profstats.apidata.TerritoryManager;
import com.profstats.craft.ActiveCraft;
import com.profstats.gather.ActiveGather;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class ProfStatsClient implements ClientModInitializer {
    public static final String MOD_ID = "profstats";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    static ProfStatsClient instance;

    private ActiveCraft activeCraft;
    private ActiveGather activeGather;

    private TerritoryManager territoryManager;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("[ProfStats] Starting init");
        instance = this;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            instance.waitForCharacterData();
        });

        territoryManager = new TerritoryManager();

        LOGGER.info("[ProfStats] Initialized");
    }

    public static ProfStatsClient getInstance() {
        return instance;
    }

    public ActiveCraft getActiveCraft() {
        return activeCraft;
    }

    public ActiveGather getActiveGather() {
        return activeGather;
    }

    public TerritoryManager getTerritoryManager() {
        return this.territoryManager;
    }

    public ActiveCraft startNewCraft(Profession profession) {
        activeCraft = new ActiveCraft(profession);
        return activeCraft;
    }

    public void waitForCharacterData() {
        activeCraft = null;
        ActiveGather.reset();
        ProfessionScanner.startWaitForScan();

    }

    public void cancelActiveCraft() {
        activeCraft = null;
    }

    public void finishActiveCraft(ItemStack craftReward, PlayerEntity player) {
        Integer foundLevel = activeCraft.setCraftLevel(craftReward, player);

        // May happen when the user clicks in the area where new crafted items are placed
        if(foundLevel == null) {
            return;
        }

        // Save it
        activeCraft.store();

        // Create a new craft instance with a new active craft; required when shift-clicking to do multiple crafts with the same action.
        // When not shit-clicking it will be overwritten by a new craft when starting the new craft.
        activeCraft = activeCraft.copyCraftSetup();
    }
}
/*
Locations are equal for the holograms (thank Salted for this)

Can have multiple gathers active at once.
Ideally, self-destruct once completed.
Must be able to get a craft based on a location.

Plan:
Step 1:
Detect all crafts within a certain area, send all to our craft as potential items.
Step 2:
DetectEntity -> For each 'active craft' check if location matches (and inner logic will select the correct craft; or if it doesn't find one _right away_ it will end itself)



*/
