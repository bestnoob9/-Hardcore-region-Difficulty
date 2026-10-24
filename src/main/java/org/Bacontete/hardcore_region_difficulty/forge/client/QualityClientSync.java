package org.Bacontete.hardcore_region_difficulty.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityDefinition;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityRegistry;

import java.util.List;

/** Client side of the quality sync: uses the server's definitions while connected to a remote server. */
@Mod.EventBusSubscriber(modid = HardcoreRegionDifficulty.MODID, value = Dist.CLIENT)
public final class QualityClientSync {

    private QualityClientSync() {}

    public static void apply(List<QualityDefinition> definitions) {
        // Single player / LAN host: the integrated server shares this JVM and already uses the local file.
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            return;
        }
        QualityRegistry.setRemote(definitions);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        QualityRegistry.clearRemote();
    }
}
