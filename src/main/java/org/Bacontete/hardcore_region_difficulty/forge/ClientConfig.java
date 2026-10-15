package org.Bacontete.hardcore_region_difficulty.forge;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side switches (file: config/hardcore_region_difficulty-client.toml).
 * Both are on by default. They only decide what THIS player sees; the server keeps working the same.
 */
public final class ClientConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_REGION_TITLE;
    public static final ForgeConfigSpec.BooleanValue SHOW_BOSS_TITLE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Difficulty title display").push("title");
        SHOW_REGION_TITLE = b.comment("Show the region name and difficulty title when entering a region")
                .define("show_region_title", true);
        SHOW_BOSS_TITLE = b.comment("Show a warning title when a boss or mini boss is nearby")
                .define("show_boss_title", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
