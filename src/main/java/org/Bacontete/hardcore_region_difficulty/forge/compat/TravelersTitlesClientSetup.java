package org.Bacontete.hardcore_region_difficulty.forge.compat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

/** Checks for Traveler's Titles once at client startup, before any title can be shown. */
@Mod.EventBusSubscriber(modid = HardcoreRegionDifficulty.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TravelersTitlesClientSetup {
    private TravelersTitlesClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(TravelersTitlesCompat::init);
    }
}
