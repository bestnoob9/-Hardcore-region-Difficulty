package org.Bacontete.hardcore_region_difficulty.forge.compat;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

/**
 * Recipe condition "hardcore_region_difficulty:deeperdarker_cataclysm".
 * True only when the config switch is on AND both Deeper and Darker and Cataclysm are loaded.
 */
public final class CompatCondition implements ICondition {
    public static final ResourceLocation ID =
            new ResourceLocation(HardcoreRegionDifficulty.MODID, "deeperdarker_cataclysm");
    public static final CompatCondition INSTANCE = new CompatCondition();

    private CompatCondition() {}

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return DeeperDarkerCataclysmCompat.isActive();
    }

    @Override
    public String toString() {
        return "deeperdarker_cataclysm_enabled";
    }

    public static final IConditionSerializer<CompatCondition> SERIALIZER = new IConditionSerializer<>() {
        @Override
        public void write(JsonObject json, CompatCondition value) {
            // No extra fields.
        }

        @Override
        public CompatCondition read(JsonObject json) {
            return INSTANCE;
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    };
}
