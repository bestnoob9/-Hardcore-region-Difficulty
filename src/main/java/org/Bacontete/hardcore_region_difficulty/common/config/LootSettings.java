package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonObject;

/**
 * Loot that grows with the level of the region ("loot" object inside scaling_settings).
 * Every bonus is "percent per level above 1", so level 1 is unchanged and own levels (6-98) work too.
 * The chance of better item qualities per level is not here: it is quality_bonus_percent of the level.
 */
public final class LootSettings {

    private boolean enabled = true;
    private boolean chestsEnabled = true;
    private boolean mobsEnabled = true;
    private int minLevel = 2;
    // chests
    private double extraRollChancePerLevel = 10.0;
    private int maxExtraRolls = 3;
    private double countPercentPerLevel = 10.0;
    private double enchantChancePerLevel = 4.0;
    // mob drops
    private double mobExtraDropChancePerLevel = 6.0;
    private int mobMaxExtraDrops = 2;
    private double mobCountPercentPerLevel = 6.0;
    private double mobEnchantChancePerLevel = 2.0;

    public static LootSettings fromJson(JsonObject o) {
        LootSettings s = new LootSettings();
        if (o == null) return s;
        s.enabled = bool(o, "enabled", s.enabled);
        s.chestsEnabled = bool(o, "chests", s.chestsEnabled);
        s.mobsEnabled = bool(o, "mobs", s.mobsEnabled);
        s.minLevel = (int) clamp(num(o, "min_level", s.minLevel), 1, 98);
        s.extraRollChancePerLevel = clamp(num(o, "chest_extra_roll_chance_percent_per_level", s.extraRollChancePerLevel), 0, 100);
        s.maxExtraRolls = (int) clamp(num(o, "chest_max_extra_rolls", s.maxExtraRolls), 0, 10);
        s.countPercentPerLevel = clamp(num(o, "chest_count_percent_per_level", s.countPercentPerLevel), 0, 1000);
        s.enchantChancePerLevel = clamp(num(o, "chest_enchant_chance_percent_per_level", s.enchantChancePerLevel), 0, 100);
        s.mobExtraDropChancePerLevel = clamp(num(o, "mob_extra_drop_chance_percent_per_level", s.mobExtraDropChancePerLevel), 0, 100);
        s.mobMaxExtraDrops = (int) clamp(num(o, "mob_max_extra_drops", s.mobMaxExtraDrops), 0, 10);
        s.mobCountPercentPerLevel = clamp(num(o, "mob_count_percent_per_level", s.mobCountPercentPerLevel), 0, 1000);
        s.mobEnchantChancePerLevel = clamp(num(o, "mob_enchant_chance_percent_per_level", s.mobEnchantChancePerLevel), 0, 100);
        return s;
    }

    /** The global settings with the keys of a region's "loot" object replacing the same keys. */
    public LootSettings overriddenBy(JsonObject override) {
        if (override == null || override.size() == 0) return this;
        JsonObject merged = toJson();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : override.entrySet()) {
            merged.add(e.getKey(), e.getValue());
        }
        return fromJson(merged);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("chests", chestsEnabled);
        o.addProperty("mobs", mobsEnabled);
        o.addProperty("min_level", minLevel);
        o.addProperty("chest_extra_roll_chance_percent_per_level", extraRollChancePerLevel);
        o.addProperty("chest_max_extra_rolls", maxExtraRolls);
        o.addProperty("chest_count_percent_per_level", countPercentPerLevel);
        o.addProperty("chest_enchant_chance_percent_per_level", enchantChancePerLevel);
        o.addProperty("mob_extra_drop_chance_percent_per_level", mobExtraDropChancePerLevel);
        o.addProperty("mob_max_extra_drops", mobMaxExtraDrops);
        o.addProperty("mob_count_percent_per_level", mobCountPercentPerLevel);
        o.addProperty("mob_enchant_chance_percent_per_level", mobEnchantChancePerLevel);
        return o;
    }

    public boolean appliesToChests(int level) { return enabled && chestsEnabled && level >= minLevel; }
    public boolean appliesToMobs(int level) { return enabled && mobsEnabled && level >= minLevel; }

    public boolean isEnabled() { return enabled; }
    public boolean isChestsEnabled() { return chestsEnabled; }
    public boolean isMobsEnabled() { return mobsEnabled; }
    public int getMinLevel() { return minLevel; }
    public double getExtraRollChancePerLevel() { return extraRollChancePerLevel; }
    public int getMaxExtraRolls() { return maxExtraRolls; }
    public double getCountPercentPerLevel() { return countPercentPerLevel; }
    public double getEnchantChancePerLevel() { return enchantChancePerLevel; }
    public double getMobExtraDropChancePerLevel() { return mobExtraDropChancePerLevel; }
    public int getMobMaxExtraDrops() { return mobMaxExtraDrops; }
    public double getMobCountPercentPerLevel() { return mobCountPercentPerLevel; }
    public double getMobEnchantChancePerLevel() { return mobEnchantChancePerLevel; }

    /** Chance (0-1) of one bonus, from a "percent per level" value at this level. */
    public static double chance(double percentPerLevel, int level) {
        return Math.max(0.0D, Math.min(1.0D, percentPerLevel * Math.max(0, level - 1) / 100.0D));
    }

    /** Stack size multiplier, from a "percent per level" value at this level (at most x10). */
    public static double countMultiplier(double percentPerLevel, int level) {
        return Math.min(10.0D, 1.0D + percentPerLevel * Math.max(0, level - 1) / 100.0D);
    }

    private static double num(JsonObject o, String key, double def) {
        try {
            return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsDouble() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        try {
            return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsBoolean() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
