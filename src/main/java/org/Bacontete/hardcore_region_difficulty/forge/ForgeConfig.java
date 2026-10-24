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

    public static final ForgeConfigSpec.DoubleValue DRAGON_HEALTH;
    public static final ForgeConfigSpec.DoubleValue WITHER_HEALTH;
    public static final ForgeConfigSpec.DoubleValue DRAGON_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WITHER_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WARDEN_HEALTH;
    public static final ForgeConfigSpec.DoubleValue WARDEN_DAMAGE_MULTIPLIER;

    /** Ids of the enchantments of this mod that can be switched on/off (registry path). */
    public static final String[] ENCHANT_IDS = {
            "advanced_protection", "advanced_thorns", "advanced_feather_falling",
            "advanced_sharpness", "advanced_fire_aspect", "advanced_knockback", "advanced_looting",
            "advanced_power", "advanced_efficiency", "advanced_unbreaking",
            "swift_strike", "armor_piercing", "true_damage"
    };
    public static final ForgeConfigSpec.BooleanValue ENCHANTMENTS_ENABLED;
    public static final java.util.Map<String, ForgeConfigSpec.BooleanValue> ENCHANT_TOGGLES = new java.util.LinkedHashMap<>();

    static {
        BUILDER.comment("Stronger vanilla end-game bosses (ignored when 'enabled' above is false).",
                        "Already spawned bosses are adjusted when they load. 0 disables the health change of that boss.")
                .push("vanilla_bosses");
        DRAGON_HEALTH = BUILDER
                .comment("Effective health of the Ender Dragon. Minecraft caps max health at 1024, so a value above 1024",
                         "keeps max health 1024 and divides the damage the boss takes (2048 = it takes half damage).",
                         "0 = vanilla (200).")
                .defineInRange("ender_dragon_health", 1024.0D, 0.0D, 100000.0D);
        WITHER_HEALTH = BUILDER
                .comment("Effective health of the Wither (same rule as the dragon). 0 = vanilla (300).")
                .defineInRange("wither_health", 512.0D, 0.0D, 100000.0D);
        DRAGON_DAMAGE_MULTIPLIER = BUILDER
                .comment("Damage multiplier of everything the Ender Dragon hits (before armor). 1.0 = vanilla.")
                .defineInRange("ender_dragon_damage_multiplier", 2.0D, 0.1D, 50.0D);
        WITHER_DAMAGE_MULTIPLIER = BUILDER
                .comment("Damage multiplier of the Wither and its skulls (before armor). 1.0 = vanilla.")
                .defineInRange("wither_damage_multiplier", 2.0D, 0.1D, 50.0D);
        WARDEN_HEALTH = BUILDER
                .comment("Effective health of the Warden (same rule as the dragon). 0 = vanilla (500).")
                .defineInRange("warden_health", 0.0D, 0.0D, 100000.0D);
        WARDEN_DAMAGE_MULTIPLIER = BUILDER
                .comment("Damage multiplier of everything the Warden hits, including the sonic boom (before armor). 1.0 = vanilla.")
                .defineInRange("warden_damage_multiplier", 1.0D, 0.1D, 50.0D);
        BUILDER.pop();

        BUILDER.comment("Switches for the enchantments added by this mod.",
                        "WARNING: they can unbalance the game, so you may turn them off. Turning one off can affect worlds that",
                        "already exist: items that already carry it keep the enchantment but it has no effect, it is no longer",
                        "found in loot, books or villager trades, and it can no longer be applied. Loot that was already",
                        "generated is not changed. Turning it back on restores everything.")
                .push("enchantments");
        ENCHANTMENTS_ENABLED = BUILDER
                .comment("Master switch of all enchantments of this mod. false = every one of them is off.")
                .define("all_enchantments_enabled", true);
        for (String id : ENCHANT_IDS) {
            ENCHANT_TOGGLES.put(id, BUILDER.define(id, true));
        }
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean isEnabled() {
        return ENABLED.get();
    }
}
