package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.Bacontete.hardcore_region_difficulty.common.elite.EliteSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScalingSettings {
    private final double healthPercent;
    private final double damagePercent;
    private final double armorPercent;
    private final double speedPercent;
    private final double armorFlatPerLevel;
    private final List<String> excludePatterns;
    private final double bossHealthThreshold;

    private final double miniBossHealthPercent;
    private final double miniBossArmorPercent;
    private final double miniBossDamagePercent;
    private final double bossHealthPercent;
    private final double bossArmorPercent;
    private final double bossDamagePercent;
    private final int raidDifficultyLevel;
    /** false (default): every level multiplies on the previous one; true: every level adds the same amount. */
    private volatile boolean linear = false;
    /** Soft cap: from this level on the % bonus grows more slowly (0 = soft cap off). */
    private volatile int softCapLevel = DEFAULT_SOFT_CAP_LEVEL;
    /** Share of a normal step that every level above the soft cap still counts for (1.0 = no reduction, 0.75 = 75%). */
    private volatile double diminishingFactor = DEFAULT_DIMINISHING_FACTOR;
    /** Extra multiplier of Boss / Mini-Boss (1.0 = unchanged), applied on top of the whole bonus already computed. */
    private volatile double bossHealthMultiplier = DEFAULT_BOSS_MULTIPLIER;
    private volatile double bossDamageMultiplier = DEFAULT_BOSS_MULTIPLIER;
    private volatile double bossArmorMultiplier = DEFAULT_BOSS_MULTIPLIER;
    private volatile double miniBossHealthMultiplier = DEFAULT_BOSS_MULTIPLIER;
    private volatile double miniBossDamageMultiplier = DEFAULT_BOSS_MULTIPLIER;
    private volatile double miniBossArmorMultiplier = DEFAULT_BOSS_MULTIPLIER;
    /** Elite mobs with random affixes (elite_* keys), see the elite package. */
    private volatile EliteSettings eliteSettings = EliteSettings.DEFAULT;
    /** Level cap of the regions by dimension (see {@link #levelCapFor}). */
    private volatile LootSettings lootSettings = new LootSettings();
    private volatile boolean levelCapEnabled = true;
    private volatile int dimensionLevelBonus = DEFAULT_DIMENSION_LEVEL_BONUS;
    /** 0 = automatic (the highest defined level), else a fixed highest level of the mod. */
    private volatile int maxRegionLevelSetting = 0;
    private volatile Map<String, Integer> dimensionLevelCaps = Collections.emptyMap();
    /** Smart auto ratings (structures, spawners, enemies) follow the level of the dimension, see adjustForDimension. */
    private volatile boolean dangerScaleEnabled = true;
    private volatile int dangerScaleReference = DEFAULT_DANGER_SCALE_REFERENCE;
    private volatile boolean dangerScaleDown = false;
    /** Only mobs matching these patterns are scaled (empty = all). Set by fromJson. */
    private volatile List<String> whitelistPatterns = Collections.emptyList();
    private volatile List<MobEntry> whitelistEntries = Collections.emptyList();
    /** Per-level custom values (see DifficultyLevelDef), set by DifficultyConfig after loading. */
    private volatile Map<Integer, DifficultyLevelDef> levelDefs = Map.of();

    public static final double DEFAULT_BOSS_HEALTH = 150.0;
    public static final int DEFAULT_DIMENSION_LEVEL_BONUS = 2;
    public static final int BUILT_IN_MAX_LEVEL = 5;
    public static final int DEFAULT_DANGER_SCALE_REFERENCE = 3;
    public static final int ABSOLUTE_MAX_LEVEL = 98;

    public static final double DEFAULT_HEALTH_PERCENT = 25.0;
    public static final double DEFAULT_DAMAGE_PERCENT = 20.0;
    public static final double DEFAULT_ARMOR_PERCENT = 20.0;
    public static final double DEFAULT_SPEED_PERCENT = 2.0;

    public static final double DEFAULT_ARMOR_FLAT = 1.0;

    public static final double DEFAULT_MINI_BOSS_HEALTH = 25.0;
    public static final double DEFAULT_MINI_BOSS_ARMOR = 20.0;
    public static final double DEFAULT_MINI_BOSS_DAMAGE = 15.0;
    public static final double DEFAULT_BOSS_HEALTH_BONUS = 50.0;
    public static final double DEFAULT_BOSS_ARMOR = 35.0;
    public static final double DEFAULT_BOSS_DAMAGE = 30.0;
    public static final int DEFAULT_RAID_LEVEL = 3;

    /** Default soft cap: levels 1-5 stay as before, only own levels (6+) are reduced. */
    public static final int DEFAULT_SOFT_CAP_LEVEL = 5;
    public static final double DEFAULT_DIMINISHING_FACTOR = 0.75;
    /** Default multiplier in code = 1.0 so old configs keep their balance (the defaults file has its own values). */
    public static final double DEFAULT_BOSS_MULTIPLIER = 1.0;
    public static final double MIN_BOSS_MULTIPLIER = 0.1;
    public static final double MAX_BOSS_MULTIPLIER = 100.0;

    public ScalingSettings() {
        this(DEFAULT_HEALTH_PERCENT, DEFAULT_DAMAGE_PERCENT, DEFAULT_ARMOR_PERCENT, DEFAULT_SPEED_PERCENT, DEFAULT_ARMOR_FLAT,
                Collections.emptyList(), DEFAULT_BOSS_HEALTH,
                DEFAULT_MINI_BOSS_HEALTH, DEFAULT_MINI_BOSS_ARMOR, DEFAULT_MINI_BOSS_DAMAGE,
                DEFAULT_BOSS_HEALTH_BONUS, DEFAULT_BOSS_ARMOR, DEFAULT_BOSS_DAMAGE, DEFAULT_RAID_LEVEL);
    }

    public ScalingSettings(double healthPercent, double damagePercent, double armorPercent,
                           double speedPercent, double armorFlatPerLevel,
                           List<String> excludePatterns, double bossHealthThreshold,
                           double miniBossHealthPercent, double miniBossArmorPercent, double miniBossDamagePercent,
                           double bossHealthPercent, double bossArmorPercent, double bossDamagePercent,
                           int raidDifficultyLevel) {
        this.excludePatterns = Collections.unmodifiableList(new ArrayList<>(excludePatterns));
        this.bossHealthThreshold = bossHealthThreshold;
        this.healthPercent = healthPercent;
        this.damagePercent = damagePercent;
        this.armorPercent = armorPercent;
        this.speedPercent = speedPercent;
        this.armorFlatPerLevel = armorFlatPerLevel;
        this.miniBossHealthPercent = miniBossHealthPercent;
        this.miniBossArmorPercent = miniBossArmorPercent;
        this.miniBossDamagePercent = miniBossDamagePercent;
        this.bossHealthPercent = bossHealthPercent;
        this.bossArmorPercent = bossArmorPercent;
        this.bossDamagePercent = bossDamagePercent;
        this.raidDifficultyLevel = Math.max(0, raidDifficultyLevel);
    }

    public static ScalingSettings fromJson(JsonObject obj) {
        double health = num(obj, "health_percent_per_level", DEFAULT_HEALTH_PERCENT);
        double damage = num(obj, "damage_percent_per_level", DEFAULT_DAMAGE_PERCENT);
        double armor = num(obj, "armor_percent_per_level", DEFAULT_ARMOR_PERCENT);
        double speed = num(obj, "speed_percent_per_level", DEFAULT_SPEED_PERCENT);
        double armorFlat = num(obj, "armor_flat_per_level", DEFAULT_ARMOR_FLAT);
        List<String> exclude = new ArrayList<>();
        if (obj.has("exclude_entities") && obj.get("exclude_entities").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("exclude_entities")) {
                if (e.isJsonPrimitive()) exclude.add(e.getAsString());
            }
        }
        double bossHealth = num(obj, "boss_health_threshold", DEFAULT_BOSS_HEALTH);

        double miniBossHealth = num(obj, "mini_boss_health_percent", DEFAULT_MINI_BOSS_HEALTH);
        double miniBossArmor = num(obj, "mini_boss_armor_percent", DEFAULT_MINI_BOSS_ARMOR);
        double miniBossDamage = num(obj, "mini_boss_damage_percent", DEFAULT_MINI_BOSS_DAMAGE);
        double bossHealthBonus = num(obj, "boss_health_percent", DEFAULT_BOSS_HEALTH_BONUS);
        double bossArmor = num(obj, "boss_armor_percent", DEFAULT_BOSS_ARMOR);
        double bossDamage = num(obj, "boss_damage_percent", DEFAULT_BOSS_DAMAGE);
        int raidLevel = (int) num(obj, "raid_difficulty_level", DEFAULT_RAID_LEVEL);
        ScalingSettings settings = new ScalingSettings(health, damage, armor, speed, armorFlat, exclude, bossHealth,
                miniBossHealth, miniBossArmor, miniBossDamage, bossHealthBonus, bossArmor, bossDamage, raidLevel);
        settings.linear = obj.has("scaling_mode") && obj.get("scaling_mode").isJsonPrimitive()
                && "linear".equalsIgnoreCase(obj.get("scaling_mode").getAsString().trim());
        // soft cap + diminishing returns (missing in old configs -> defaults)
        settings.softCapLevel = (int) Math.max(0, Math.min(ABSOLUTE_MAX_LEVEL,
                num(obj, "scaling_soft_cap_level", DEFAULT_SOFT_CAP_LEVEL)));
        settings.diminishingFactor = Math.max(0.0, Math.min(1.0,
                num(obj, "diminishing_factor", DEFAULT_DIMINISHING_FACTOR)));
        settings.bossHealthMultiplier = multiplier(obj, "boss_health_multiplier");
        settings.bossDamageMultiplier = multiplier(obj, "boss_damage_multiplier");
        settings.bossArmorMultiplier = multiplier(obj, "boss_armor_multiplier");
        settings.miniBossHealthMultiplier = multiplier(obj, "mini_boss_health_multiplier");
        settings.miniBossDamageMultiplier = multiplier(obj, "mini_boss_damage_multiplier");
        settings.miniBossArmorMultiplier = multiplier(obj, "mini_boss_armor_multiplier");
        settings.eliteSettings = EliteSettings.fromJson(obj);
        if (obj.has("scale_danger_with_dimension") && obj.get("scale_danger_with_dimension").isJsonPrimitive()) {
            settings.dangerScaleEnabled = obj.get("scale_danger_with_dimension").getAsBoolean();
        }
        settings.dangerScaleReference = (int) Math.max(1, Math.min(ABSOLUTE_MAX_LEVEL,
                num(obj, "danger_scale_reference_level", DEFAULT_DANGER_SCALE_REFERENCE)));
        if (obj.has("danger_scale_down") && obj.get("danger_scale_down").isJsonPrimitive()) {
            settings.dangerScaleDown = obj.get("danger_scale_down").getAsBoolean();
        }
        settings.lootSettings = LootSettings.fromJson(obj.has("loot") && obj.get("loot").isJsonObject()
                ? obj.getAsJsonObject("loot") : null);
        if (obj.has("level_cap_enabled") && obj.get("level_cap_enabled").isJsonPrimitive()) {
            settings.levelCapEnabled = obj.get("level_cap_enabled").getAsBoolean();
        }
        settings.dimensionLevelBonus = (int) Math.max(0, Math.min(ABSOLUTE_MAX_LEVEL,
                num(obj, "dimension_level_bonus", DEFAULT_DIMENSION_LEVEL_BONUS)));
        settings.maxRegionLevelSetting = (int) Math.max(0, Math.min(ABSOLUTE_MAX_LEVEL, num(obj, "max_region_level", 0)));
        if (obj.has("dimension_level_caps") && obj.get("dimension_level_caps").isJsonObject()) {
            Map<String, Integer> caps = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("dimension_level_caps").entrySet()) {
                try {
                    int v = e.getValue().getAsInt();
                    if (v >= 1) caps.put(e.getKey().trim(), Math.min(v, ABSOLUTE_MAX_LEVEL));
                } catch (Exception ignored) {
                }
            }
            settings.dimensionLevelCaps = Collections.unmodifiableMap(caps);
        }
        if (obj.has("whitelist_entities") && obj.get("whitelist_entities").isJsonArray()) {
            List<MobEntry> entries = new ArrayList<>();
            List<String> white = new ArrayList<>();
            for (JsonElement e : obj.getAsJsonArray("whitelist_entities")) {
                MobEntry entry = MobEntry.fromJson(e);
                if (entry != null) {
                    entries.add(entry);
                    white.add(entry.getPattern());
                }
            }
            settings.whitelistEntries = Collections.unmodifiableList(entries);
            settings.whitelistPatterns = Collections.unmodifiableList(white);
        }
        return settings;
    }

    private static double num(JsonObject obj, String key, double def) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : def;
        } catch (Exception e) {
            return def;
        }
    }

    /** Reads one multiplier, limited to [MIN, MAX] so it can never be negative or absurd. */
    private static double multiplier(JsonObject obj, String key) {
        return Math.max(MIN_BOSS_MULTIPLIER, Math.min(MAX_BOSS_MULTIPLIER, num(obj, key, DEFAULT_BOSS_MULTIPLIER)));
    }

    /**
     * Effective level after the soft cap: min(level, softCap) + (level - softCap) * diminishing_factor.
     * Up to the soft cap (or with the soft cap off) it equals the level. Only used by the per-level formula;
     * levels with their own stats (difficulty_levels) still use exactly the values typed by the user.
     */
    public double effectiveLevel(int level) {
        if (softCapLevel < 1 || level <= softCapLevel) return level;
        return softCapLevel + (level - softCapLevel) * diminishingFactor;
    }

    /** True when this level is really reduced by the soft cap (used for debug logs). */
    public boolean isSoftCapActive(int level) {
        return softCapLevel >= 1 && level > softCapLevel && diminishingFactor < 1.0;
    }

    /** Number of stat steps: level 1 is the base (0 steps); uses the effective level, so it can be fractional. */
    private double steps(int difficultyLevel) {
        return Math.max(0.0, effectiveLevel(difficultyLevel) - 1.0);
    }

    /** Multiplies the total stat (1 + bonus) by the multiplier; the bonus never goes below -90%. */
    public static double multiplyBonus(double bonus, double multiplier) {
        return Math.max(-0.9, (1.0 + bonus) * multiplier - 1.0);
    }

    public void setLevelDefs(Map<Integer, DifficultyLevelDef> defs) {
        this.levelDefs = defs == null ? Map.of() : defs;
    }

    public boolean isLinear() { return linear; }

    public int getSoftCapLevel() { return softCapLevel; }
    public double getDiminishingFactor() { return diminishingFactor; }
    public double getBossHealthMultiplier() { return bossHealthMultiplier; }
    public double getBossDamageMultiplier() { return bossDamageMultiplier; }
    public double getBossArmorMultiplier() { return bossArmorMultiplier; }
    public double getMiniBossHealthMultiplier() { return miniBossHealthMultiplier; }
    public double getMiniBossDamageMultiplier() { return miniBossDamageMultiplier; }
    public double getMiniBossArmorMultiplier() { return miniBossArmorMultiplier; }
    public EliteSettings getEliteSettings() { return eliteSettings; }

    public boolean isDangerScaleEnabled() { return dangerScaleEnabled; }
    public int getDangerScaleReference() { return dangerScaleReference; }
    public boolean isDangerScaleDown() { return dangerScaleDown; }

    /**
     * The automatic ratings (structures, spawners, enemies) are made for dimensions up to level
     * danger_scale_reference_level (3 = The End). In a dimension above that, they would never reach the levels
     * the dimension allows, so they are raised by the difference (dimension level 6, reference 3: +3).
     * With danger_scale_down they are also lowered in a dimension below the reference (never under level 1).
     */
    public int adjustForDimension(int level, int dimensionBaseLevel) {
        if (!dangerScaleEnabled || level <= 0) return level;
        int shift = dimensionBaseLevel - dangerScaleReference;
        if (shift > 0) return Math.min(ABSOLUTE_MAX_LEVEL, level + shift);
        if (shift < 0 && dangerScaleDown) return Math.max(1, level + shift);
        return level;
    }

    public LootSettings getLootSettings() { return lootSettings; }

    public boolean isLevelCapEnabled() { return levelCapEnabled; }
    public int getDimensionLevelBonus() { return dimensionLevelBonus; }
    public int getMaxRegionLevelSetting() { return maxRegionLevelSetting; }
    public Map<String, Integer> getDimensionLevelCaps() { return dimensionLevelCaps; }

    /**
     * Highest level of the mod: the fixed "max_region_level" if set, else the highest own level defined in
     * "difficulty_levels" (6-98), else 5. Creating level 6 raises it to 6, creating level 7 to 7...
     */
    public int maxRegionLevel() {
        if (maxRegionLevelSetting > 0) return maxRegionLevelSetting;
        int highest = BUILT_IN_MAX_LEVEL;
        for (int lvl : levelDefs.keySet()) {
            if (lvl > highest && lvl <= ABSOLUTE_MAX_LEVEL) highest = lvl;
        }
        return highest;
    }

    /**
     * Highest level a region can have inside a dimension whose own level is baseLevel:
     * the entry of dimension_level_caps that matches the dimension (exact level), else baseLevel +
     * dimension_level_bonus; never above {@link #maxRegionLevel()}. No limit when level_cap_enabled is false.
     */
    public int levelCapFor(net.minecraft.resources.ResourceLocation dimensionId, int baseLevel) {
        if (!levelCapEnabled) return Integer.MAX_VALUE;
        int cap = Math.max(1, baseLevel) + dimensionLevelBonus;
        for (Map.Entry<String, Integer> e : dimensionLevelCaps.entrySet()) {
            if (org.Bacontete.hardcore_region_difficulty.common.difficulty.PatternMatcher
                    .matches(List.of(e.getKey()), dimensionId, null)) {
                cap = e.getValue();
                break;
            }
        }
        return Math.max(1, Math.min(cap, maxRegionLevel()));
    }

    /** Total multiplier of the chance of good qualities at this level (see ItemQuality), or null = formula. */
    public Double qualityMultiplier(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d == null || d.getQualityBonusPercent() == null ? null
                : Math.max(0.1D, 1.0D + d.getQualityBonusPercent() / 100.0D);
    }

    public Double highQualityMultiplier(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d == null || d.getHighQualityBonusPercent() == null ? null
                : Math.max(0.1D, 1.0D + d.getHighQualityBonusPercent() / 100.0D);
    }

    /** Global mob whitelist (patterns of exclude_entities). Empty = no restriction. */
    public List<String> getWhitelistPatterns() { return whitelistPatterns; }

    /** Global whitelist entries with their per-mob details. */
    public List<MobEntry> getWhitelistEntries() { return whitelistEntries; }

    /** True when the level has its own custom stat values (then it is scaled even at level 1). */
    public boolean hasCustom(int difficultyLevel) {
        DifficultyLevelDef def = levelDefs.get(difficultyLevel);
        return def != null && def.hasStatOverride();
    }

    private double formula(double percent, int difficultyLevel) {
        double n = steps(difficultyLevel);
        if (n == 0.0 || percent == 0.0) return 0.0;
        // both modes are kept: linear (additive) or compound (multiplicative); only n went through the soft cap
        return linear ? n * percent / 100.0 : Math.pow(1.0 + percent / 100.0, n) - 1.0;
    }

    /** Custom bonuses are limited to -90% so a stat can never become zero or negative. */
    private static double percentOf(Double custom) {
        return Math.max(-0.9, custom / 100.0);
    }

    public double healthAmount(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d != null && d.getHealthBonusPercent() != null ? percentOf(d.getHealthBonusPercent())
                : formula(healthPercent, difficultyLevel);
    }

    public double armorMultiplyAmount(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d != null && d.getArmorBonusPercent() != null ? percentOf(d.getArmorBonusPercent())
                : formula(armorPercent, difficultyLevel);
    }

    public double armorFlatAmount(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d != null && d.getArmorFlatBonus() != null ? d.getArmorFlatBonus()
                : steps(difficultyLevel) * armorFlatPerLevel;
    }

    public double damageAmount(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d != null && d.getDamageBonusPercent() != null ? percentOf(d.getDamageBonusPercent())
                : formula(damagePercent, difficultyLevel);
    }

    public double speedAmount(int difficultyLevel) {
        DifficultyLevelDef d = levelDefs.get(difficultyLevel);
        return d != null && d.getSpeedBonusPercent() != null ? percentOf(d.getSpeedBonusPercent())
                : formula(speedPercent, difficultyLevel);
    }

    public List<String> getExcludePatterns() { return excludePatterns; }

    public double getBossHealthThreshold() { return bossHealthThreshold; }
    public double getHealthPercent() { return healthPercent; }
    public double getDamagePercent() { return damagePercent; }
    public double getArmorPercent() { return armorPercent; }
    public double getSpeedPercent() { return speedPercent; }
    public double getArmorFlatPerLevel() { return armorFlatPerLevel; }

    public double getMiniBossHealthPercent() { return miniBossHealthPercent; }
    public double getMiniBossArmorPercent() { return miniBossArmorPercent; }
    public double getMiniBossDamagePercent() { return miniBossDamagePercent; }
    public double getBossHealthPercent() { return bossHealthPercent; }
    public double getBossArmorPercent() { return bossArmorPercent; }
    public double getBossDamagePercent() { return bossDamagePercent; }
    /** Fixed difficulty level for every raid mob (0 = disabled, use region level). */
    public int getRaidDifficultyLevel() { return raidDifficultyLevel; }
}
