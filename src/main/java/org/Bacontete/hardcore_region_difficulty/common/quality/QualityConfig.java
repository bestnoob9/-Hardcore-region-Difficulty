package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class QualityConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_QUALITY;
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
    public static final ForgeConfigSpec.DoubleValue LUCK_QUALITY_BONUS;
    public static final ForgeConfigSpec.DoubleValue LOW_QUALITY_REDUCTION;
    public static final ForgeConfigSpec.BooleanValue UNLOCK_HIGH_QUALITIES;
    public static final ForgeConfigSpec.IntValue VILLAGER_MIN_QUALITY_LEVEL;
    public static final ForgeConfigSpec.DoubleValue TIER_STANDARD_CHANCE;
    public static final ForgeConfigSpec.DoubleValue TIER_BAD_CHANCE;
    public static final ForgeConfigSpec.DoubleValue TIER_BAD_FALLOFF;
    public static final ForgeConfigSpec.DoubleValue TIER_GOOD_CHANCE;
    public static final ForgeConfigSpec.DoubleValue TIER_GOOD_FALLOFF;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("General quality settings").push("general");
        ENABLE_QUALITY = b.comment("Master switch of the equipment quality system. false = no new qualities are rolled (loot, inventory, villagers), existing qualities have no effect and no tooltip, and the Quality Forge is locked. Items keep their quality tag, so turning it back on restores everything")
                .define("enabled", true);
        b.pop();
        b.comment("Quality rolling. The chance weight, color, name and effects of EACH quality are in config/hardcore_region_difficulty-qualities.json").push("rolling");
        ROLL_IN_LOOT = b.comment("Weapons and tools found in chest loot get a random quality").define("roll_in_loot", true);
        ROLL_IN_INVENTORY = b.comment("A weapon or tool without a quality that is in a player's inventory (crafted, taken from the creative menu, picked up...) gets a random quality")
                .define("roll_in_inventory", true);
        DIFFICULTY_QUALITY_BONUS = b.comment("Bonus to quality roll chance per difficulty level (1-5). Higher difficulty = better quality. 0.1 = 10% bonus per level").defineInRange("difficulty_quality_bonus", 0.1D, 0.0D, 1.0D);
        HIGH_TIER_BONUS_MULTIPLIER = b.comment("High qualities (Superior and above, Fortified / Reinforced shields) use difficulty_quality_bonus multiplied by this, so they appear much more often in dangerous regions. Only used by rolls that depend on the region / villager level (loot, inventory, villagers): the Quality Forge ignores the level")
                .defineInRange("high_tier_bonus_multiplier", 1.5D, 1.0D, 20.0D);
        LUCK_QUALITY_BONUS = b.comment("Effect of the player's Luck (Potion of Luck, Luck attribute) on EVERY quality roll: the Quality Forge, chest loot, mob drops, villager trades and the inventory roll.",
                        "Each point of Luck adds this much to the multiplier of the good qualities (0.5 = +50% chance of positive qualities per point, high qualities use high_tier_bonus_multiplier on top) and divides the chance of the negative ones by the same multiplier.",
                        "Negative Luck (Potion of Bad Luck) does the opposite. 0 = Luck has no effect on qualities")
                .defineInRange("luck_quality_bonus", 0.5D, 0.0D, 5.0D);
        LOW_QUALITY_REDUCTION = b.comment("How much the chance of LOW qualities (the negative ones) is cut in the most dangerous regions. The cut grows with the level and reaches this value at the highest level of the mod,",
                        "so levels you add yourself (6, 7...) stretch the scale: at level 1 nothing changes, at the top level the low qualities keep (1 - value) of their weight. 0 = off, 0.85 = only 15 percent of them are left at the top.",
                        "Not used by a level that sets its own quality_bonus_percent")
                .defineInRange("low_quality_reduction", 0.85D, 0.0D, 1.0D);
        UNLOCK_HIGH_QUALITIES = b.comment("Qualities whose weight is 0 can still appear in high difficulty regions: Mythic from level 4, Divine / Ancient / Pristine and Flawless at level 5 (weight 1, +1 every 2 further levels). When you add levels above 5 these unlock levels are stretched to the new highest level (level 4 of 5 = 80 percent of the way up). Set to false to disable")
                .define("unlock_high_qualities", true);
        VILLAGER_MIN_QUALITY_LEVEL = b.comment("Villager level (1 Novice, 2 Apprentice, 3 Journeyman, 4 Expert, 5 Master) from which traded equipment can get a quality other than the standard one.",
                "Below this level every traded weapon / tool / armor / bow / shield is always the standard quality (the tier 0 quality of its category). 6 = villagers always sell the standard quality")
                .defineInRange("villager_min_quality_level", 4, 1, 6);
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
        b.comment("Chances of the Quality Forge and of crafted / inventory items. They do NOT use the weights of the quality file: each TIER gets a share, split equally between the qualities of that tier.",
                "Only the ratio between the numbers matters. To keep standard under 30 percent, leave it at about a third of the total of the others.").push("tier_chances");
        TIER_STANDARD_CHANCE = b.comment("Share of the standard quality (tier 0)").defineInRange("standard_chance", 13.0D, 0.0D, 1000.0D);
        TIER_BAD_CHANCE = b.comment("Share of all tier -1 qualities together (qualities that lower stats)").defineInRange("bad_chance", 14.0D, 0.0D, 1000.0D);
        TIER_BAD_FALLOFF = b.comment("Each further negative tier (-2, -3...) gets this fraction of the previous tier's share. 0.5 = half")
                .defineInRange("bad_falloff", 0.5D, 0.0D, 1.0D);
        TIER_GOOD_CHANCE = b.comment("Share of all tier 1 qualities together (qualities that raise stats)").defineInRange("good_chance", 9.0D, 0.0D, 1000.0D);
        TIER_GOOD_FALLOFF = b.comment("Each further positive tier (2, 3...) gets this fraction of the previous tier's share. 0.4 = 40 percent. Mythic, Divine and Flawless sit in the highest tiers")
                .defineInRange("good_falloff", 0.4D, 0.0D, 1.0D);
        b.pop();
        SPEC = b.build();
    }

    /** Master switch of the whole quality system. */
    public static boolean isEnabled() {
        return ENABLE_QUALITY.get();
    }

    private QualityConfig() {}
}
