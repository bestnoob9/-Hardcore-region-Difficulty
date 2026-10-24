package org.Bacontete.hardcore_region_difficulty.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Registers the "Config" button of the mod list, but only when Cloth Config API is installed.
 * This class must NOT import anything from me.shedaniel: it is loaded even without Cloth Config.
 * {@link HrdConfigScreen} (which does import Cloth Config) is only touched inside the lambda,
 * i.e. only after the player actually presses the button.
 */
public final class ClientConfigHooks {

    private ClientConfigHooks() {}

    public static void register() {
        IconFolder.init();
        if (!ModList.get().isLoaded("cloth_config")) {
            return;
        }
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> HrdConfigScreen.create(parent)));
    }
}
