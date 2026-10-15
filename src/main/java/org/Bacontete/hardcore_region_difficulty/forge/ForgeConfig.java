package org.Bacontete.hardcore_region_difficulty.forge;

import net.minecraftforge.common.ForgeConfigSpec;

/** Master switch of the region difficulty evaluation (file: config/hardcore_region_difficulty-common.toml). */
public class ForgeConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
        .comment("Master switch of the region difficulty evaluation. false = the whole difficulty system is off:",
                 "no mob scaling (health / armor / damage / speed), no region and boss titles, no difficulty based loot,",
                 "enchanted books or quality bonus (everything behaves like difficulty level 1).",
                 "WARNING for existing worlds: mobs that were ALREADY scaled keep their boosted stats (they are saved in the world),",
                 "chests that were already opened keep their loot, and mobs spawned while this is off stay unscaled.",
                 "Turning it back on scales mobs that load without the tag again.")
        .define("enabled", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean isEnabled() {
        return ENABLED.get();
    }
}
