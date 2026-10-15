package org.Bacontete.hardcore_region_difficulty.forge.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.forge.quality.QualityForgeRegistry;

@Mod.EventBusSubscriber(modid = HardcoreRegionDifficulty.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QualityClientSetup {

    private QualityClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(QualityForgeRegistry.QUALITY_FORGE_MENU.get(), QualityForgeScreen::new));
    }
}
