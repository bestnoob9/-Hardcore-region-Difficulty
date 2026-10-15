package org.Bacontete.hardcore_region_difficulty.forge.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Optional integration: Cataclysm gear is upgraded from Deeper and Darker Warden gear
 * instead of netherite gear.
 *
 * How it works (no compile-time dependency on either mod, only item ids in JSON):
 *  1. The new recipes live in data/hardcore_region_difficulty/recipes/compat/ and are guarded by
 *     the CompatCondition, so they only exist when the switch is on.
 *  2. The original Cataclysm netherite-based recipes cannot be "overridden" conditionally
 *     (a failed condition would delete them even when the switch is off), so this class removes
 *     them from the RecipeManager after the normal recipe load, only when the switch is on.
 */
public final class DeeperDarkerCataclysmCompat {
    public static final String DEEPER_DARKER = "deeperdarker";
    public static final String CATACLYSM = "cataclysm";

    /** Cataclysm recipes whose input is netherite gear; replaced by the Warden versions. */
    private static final Set<ResourceLocation> REPLACED_RECIPES = Set.of(
            id("smithing/ignitium_helmet"),
            id("smithing/ignitium_chestplate"),
            id("smithing/ignitium_leggings"),
            id("smithing/ignitium_boots"),
            id("smithing/cursium_helmet"),
            id("smithing/cursium_chestplate"),
            id("smithing/cursium_leggings"),
            id("smithing/cursium_boots"),
            id("smithing/monstrous_helm"),
            id("the_incinerator"));

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CATACLYSM, path);
    }

    /** True when the config switch is on and both mods are present. */
    public static boolean isActive() {
        return isEnabledInConfig() && modsPresent();
    }

    public static boolean isEnabledInConfig() {
        DifficultyConfig config = DifficultyConfig.getInstance();
        return config != null && config.getCompatSettings().isDeeperDarkerCataclysm();
    }

    /** Both Deeper and Darker and Cataclysm are installed. */
    public static boolean modsPresent() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(DEEPER_DARKER) && mods.isLoaded(CATACLYSM);
    }

    /**
     * Called once at startup. If the switch is on, both mods become hard requirements:
     * a missing mod stops the game with a clear message instead of silently doing nothing.
     */
    public static void verifyDependencies() {
        if (!isEnabledInConfig()) {
            return;
        }
        ModList mods = ModList.get();
        List<String> missing = new ArrayList<>();
        if (!mods.isLoaded(DEEPER_DARKER)) missing.add("Deeper and Darker (" + DEEPER_DARKER + ")");
        if (!mods.isLoaded(CATACLYSM)) missing.add("L_Ender's Cataclysm (" + CATACLYSM + ")");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Hardcore Region Difficulty: mod_compat_settings.deeperdarker_cataclysm "
                    + "is enabled but these mods are missing: " + String.join(", ", missing)
                    + ". Install them or set the option to false in config/hardcore_region_difficulty.json.");
        }
        HardcoreRegionDifficulty.LOGGER.info(
                "Deeper and Darker + Cataclysm integration is ON: Cataclysm gear upgrades from Warden gear");
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        RecipeManager manager = event.getServerResources().getRecipeManager();
        // Listeners added through this event apply after the vanilla ones, so recipes are already loaded.
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void nothing, ResourceManager resourceManager, ProfilerFiller profiler) {
                removeNetheriteRecipes(manager);
            }
        });
    }

    private static void removeNetheriteRecipes(RecipeManager manager) {
        if (!isActive()) {
            return;
        }
        List<Recipe<?>> kept = new ArrayList<>();
        int removed = 0;
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (REPLACED_RECIPES.contains(recipe.getId())) {
                removed++;
            } else {
                kept.add(recipe);
            }
        }
        if (removed > 0) {
            manager.replaceRecipes(kept);
        }
        if (removed != REPLACED_RECIPES.size()) {
            HardcoreRegionDifficulty.LOGGER.warn(
                    "Deeper and Darker + Cataclysm integration: removed {} of {} Cataclysm netherite recipes "
                            + "(a different Cataclysm version may use other recipe ids)",
                    removed, REPLACED_RECIPES.size());
        } else {
            HardcoreRegionDifficulty.LOGGER.info(
                    "Deeper and Darker + Cataclysm integration: removed {} Cataclysm netherite recipes", removed);
        }
    }
}
