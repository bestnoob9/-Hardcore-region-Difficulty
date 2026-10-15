package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class QualityConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_QUALITY;
    public static final ForgeConfigSpec.IntValue WEIGHT_CRACKED;
    public static final ForgeConfigSpec.IntValue WEIGHT_CRUMBLING;
    public static final ForgeConfigSpec.IntValue WEIGHT_CHIPPED;
    public static final ForgeConfigSpec.IntValue WEIGHT_WORN;
    public static final ForgeConfigSpec.IntValue WEIGHT_RUSTED;
    public static final ForgeConfigSpec.IntValue WEIGHT_DAMAGED;
    public static final ForgeConfigSpec.IntValue WEIGHT_WEATHERED;
    public static final ForgeConfigSpec.IntValue WEIGHT_STANDARD;
    public static final ForgeConfigSpec.IntValue WEIGHT_POLISHED;
    public static final ForgeConfigSpec.IntValue WEIGHT_REFINED;
    public static final ForgeConfigSpec.IntValue WEIGHT_SUPERIOR;
    public static final ForgeConfigSpec.IntValue WEIGHT_MASTERWORK;
    public static final ForgeConfigSpec.IntValue WEIGHT_LEGENDARY;
    public static final ForgeConfigSpec.IntValue WEIGHT_EPIC;
    public static final ForgeConfigSpec.IntValue WEIGHT_MYTHIC;
    public static final ForgeConfigSpec.IntValue WEIGHT_DIVINE;
    public static final ForgeConfigSpec.IntValue WEIGHT_ANCIENT;
    public static final ForgeConfigSpec.IntValue WEIGHT_PRISTINE;
    public static final ForgeConfigSpec.IntValue WEIGHT_FLAWLESS;
    public static final ForgeConfigSpec.IntValue WEIGHT_BRITTLE;
    public static final ForgeConfigSpec.IntValue WEIGHT_SLUGGISH;
    public static final ForgeConfigSpec.IntValue WEIGHT_CUMBERSOME;
    public static final ForgeConfigSpec.IntValue WEIGHT_HEAVY;
    public static final ForgeConfigSpec.IntValue WEIGHT_THICK;
    public static final ForgeConfigSpec.IntValue WEIGHT_SOLID;
    public static final ForgeConfigSpec.IntValue WEIGHT_FORTIFIED;
    public static final ForgeConfigSpec.IntValue WEIGHT_REINFORCED;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_STEP;
    public static final ForgeConfigSpec.DoubleValue SPEED_STEP;
    public static final ForgeConfigSpec.DoubleValue MINING_STEP;
    public static final ForgeConfigSpec.DoubleValue DURABILITY_STEP;
    public static final ForgeConfigSpec.DoubleValue DRAW_STEP;
    public static final ForgeConfigSpec.IntValue MATERIAL_COST;
    public static final ForgeConfigSpec.IntValue XP_COST;
    public static final ForgeConfigSpec.BooleanValue ROLL_IN_LOOT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MATERIAL_OVERRIDES;
    public static final ForgeConfigSpec.BooleanValue ROLL_IN_INVENTORY;
    public static final ForgeConfigSpec.DoubleValue DIFFICULTY_QUALITY_BONUS;
    public static final ForgeConfigSpec.DoubleValue HIGH_TIER_BONUS_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue UNLOCK_HIGH_QUALITIES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("General quality settings").push("general");
        ENABLE_QUALITY = b.comment("Master switch of the equipment quality system. false = no new qualities are rolled (loot, inventory, villagers), existing qualities have no effect and no tooltip, and the Quality Forge is locked. Items keep their quality tag, so turning it back on restores everything")
                .define("enabled", true);
        b.pop();
        b.comment("Chance weights of each quality when a quality is rolled").push("rolling");
        b.comment("Negative qualities").push("negative");
        WEIGHT_CRACKED = b.defineInRange("weight_cracked", 5, 0, 10000);
        WEIGHT_CRUMBLING = b.defineInRange("weight_crumbling", 8, 0, 10000);
        WEIGHT_CHIPPED = b.defineInRange("weight_chipped", 10, 0, 10000);
        WEIGHT_WORN = b.defineInRange("weight_worn", 12, 0, 10000);
        WEIGHT_RUSTED = b.defineInRange("weight_rusted", 5, 0, 10000);
        WEIGHT_DAMAGED = b.defineInRange("weight_damaged", 8, 0, 10000);
        WEIGHT_WEATHERED = b.defineInRange("weight_weathered", 10, 0, 10000);
        b.pop();
        b.comment("Normal qualities").push("normal");
        WEIGHT_STANDARD = b.defineInRange("weight_standard", 50, 0, 10000);
        b.pop();
        b.comment("Positive qualities").push("positive");
        WEIGHT_POLISHED = b.defineInRange("weight_polished", 15, 0, 10000);
        WEIGHT_REFINED = b.defineInRange("weight_refined", 10, 0, 10000);
        WEIGHT_SUPERIOR = b.defineInRange("weight_superior", 6, 0, 10000);
        WEIGHT_MASTERWORK = b.defineInRange("weight_masterwork", 3, 0, 10000);
        WEIGHT_LEGENDARY = b.defineInRange("weight_legendary", 1, 0, 10000);
        WEIGHT_EPIC = b.defineInRange("weight_epic", 1, 0, 10000);
        WEIGHT_MYTHIC = b.defineInRange("weight_mythic", 0, 0, 10000);
        WEIGHT_DIVINE = b.defineInRange("weight_divine", 0, 0, 10000);
        WEIGHT_ANCIENT = b.defineInRange("weight_ancient", 0, 0, 10000);
        WEIGHT_PRISTINE = b.defineInRange("weight_pristine", 0, 0, 10000);
        WEIGHT_FLAWLESS = b.defineInRange("weight_flawless", 0, 0, 10000);
        b.pop();
        b.comment("Shield qualities").push("shield");
        WEIGHT_BRITTLE = b.defineInRange("weight_brittle", 3, 0, 10000);
        WEIGHT_SLUGGISH = b.defineInRange("weight_sluggish", 6, 0, 10000);
        WEIGHT_CUMBERSOME = b.defineInRange("weight_cumbersome", 6, 0, 10000);
        WEIGHT_HEAVY = b.defineInRange("weight_heavy", 8, 0, 10000);
        WEIGHT_THICK = b.defineInRange("weight_thick", 50, 0, 10000);
        WEIGHT_SOLID = b.defineInRange("weight_solid", 15, 0, 10000);
        WEIGHT_FORTIFIED = b.defineInRange("weight_fortified", 6, 0, 10000);
        WEIGHT_REINFORCED = b.defineInRange("weight_reinforced", 3, 0, 10000);
        b.pop();
        ROLL_IN_LOOT = b.comment("Weapons and tools found in chest loot get a random quality").define("roll_in_loot", true);
        ROLL_IN_INVENTORY = b.comment("A weapon or tool without a quality that is in a player's inventory (crafted, taken from the creative menu, picked up...) gets a random quality")
                .define("roll_in_inventory", true);
        DIFFICULTY_QUALITY_BONUS = b.comment("Bonus to quality roll chance per difficulty level (1-5). Higher difficulty = better quality. 0.1 = 10% bonus per level").defineInRange("difficulty_quality_bonus", 0.1D, 0.0D, 1.0D);
        HIGH_TIER_BONUS_MULTIPLIER = b.comment("High qualities (Superior and above, Fortified / Reinforced shields) use difficulty_quality_bonus multiplied by this, so they appear much more often in dangerous regions. Does not apply to the Quality Forge (always difficulty 1)")
                .defineInRange("high_tier_bonus_multiplier", 1.5D, 1.0D, 20.0D);
        UNLOCK_HIGH_QUALITIES = b.comment("Qualities whose weight is 0 can still appear in high difficulty regions: Mythic from level 4, Divine / Ancient / Pristine and Flawless at level 5 (weight 1, +1 every 2 further levels). Set to false to disable")
                .define("unlock_high_qualities", true);
        b.comment("Bonus per quality step away from 3 (quality 1 / 5 = 2 steps, 2 / 4 = 1 step)").push("effects");
        DAMAGE_STEP = b.comment("Weapon damage (and arrow damage of bows / crossbows), 0.10 = 10 percent per step").defineInRange("damage_step", 0.10D, 0.0D, 1.0D);
        SPEED_STEP = b.comment("Weapon attack speed").defineInRange("speed_step", 0.08D, 0.0D, 1.0D);
        MINING_STEP = b.comment("Tool mining speed").defineInRange("mining_step", 0.10D, 0.0D, 1.0D);
        DURABILITY_STEP = b.comment("Maximum durability of every weapon, tool, bow and crossbow per step: quality 4 / 5 last longer, quality 2 / 1 wear out faster. 0.15 = +/-15 percent per step")
                .defineInRange("durability_step", 0.15D, 0.0D, 0.9D);
        DRAW_STEP = b.comment("Bow draw speed and crossbow loading speed, 0.10 = 10 percent faster (quality 4 / 5) or slower (quality 2 / 1) per step")
                .defineInRange("draw_step", 0.10D, 0.0D, 0.9D);
        b.pop();
        b.push("forge_table");
        MATERIAL_COST = b.comment("Materials used by one forging").defineInRange("material_cost", 1, 1, 64);
        XP_COST = b.comment("Experience LEVELS taken from the player by one forging (like the anvil). 0 = no XP needed. Creative players forge for free. NOT BALANCED YET").defineInRange("xp_cost", 3, 0, 100);
        MATERIAL_OVERRIDES = b.comment("Extra accepted materials, one entry per item as \"tool_id=material_id\", e.g. \"mod:great_sword=minecraft:netherite_ingot\".",
                "Without an entry, the table accepts the item's repair material and every ingredient (except handles) of its crafting recipe.")
                .defineListAllowEmpty("material_overrides", List.of(), o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    /** Master switch of the whole quality system. */
    public static boolean isEnabled() {
        return ENABLE_QUALITY.get();
    }

    private QualityConfig() {}
}
