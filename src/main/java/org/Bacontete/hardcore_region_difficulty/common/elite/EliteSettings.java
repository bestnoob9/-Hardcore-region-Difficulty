package org.Bacontete.hardcore_region_difficulty.common.elite;

import com.google.gson.JsonObject;

/**
 * Elite mob settings, read from the flat "elite_*" keys of "scaling_settings".
 * Every key is optional: a missing key (old config) falls back to its default.
 */
public final class EliteSettings {

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MIN_LEVEL = 4;
    public static final double DEFAULT_BASE_CHANCE = 0.15D;
    public static final double DEFAULT_CHANCE_PER_LEVEL = 0.08D;
    public static final int DEFAULT_MAX_AFFIXES = 2;
    /** At most this many living elites per difficulty level at the same time (0 = no limit). */
    public static final int DEFAULT_MAX_PER_LEVEL = 1;
    public static final boolean DEFAULT_PARTICLES = true;
    public static final boolean DEFAULT_SHOW_AFFIX_NAMES = true;

    /** Settings used when the config has no elite keys at all. */
    public static final EliteSettings DEFAULT = new EliteSettings(DEFAULT_ENABLED, DEFAULT_MIN_LEVEL,
            DEFAULT_BASE_CHANCE, DEFAULT_CHANCE_PER_LEVEL, DEFAULT_MAX_AFFIXES, DEFAULT_MAX_PER_LEVEL, DEFAULT_PARTICLES, DEFAULT_SHOW_AFFIX_NAMES);

    private final boolean enabled;
    private final int minLevel;
    private final double baseChance;
    private final double chancePerLevel;
    private final int maxAffixes;
    private final int maxPerLevel;
    private final boolean particles;
    private final boolean showAffixNames;

    public EliteSettings(boolean enabled, int minLevel, double baseChance, double chancePerLevel,
                         int maxAffixes, int maxPerLevel, boolean particles, boolean showAffixNames) {
        this.enabled = enabled;
        this.minLevel = Math.max(1, minLevel);
        this.baseChance = clamp01(baseChance);
        this.chancePerLevel = Math.max(0.0D, chancePerLevel);
        this.maxAffixes = Math.max(1, Math.min(EliteAffix.values().length, maxAffixes));
        this.maxPerLevel = Math.max(0, Math.min(1000, maxPerLevel));
        this.particles = particles;
        this.showAffixNames = showAffixNames;
    }

    public static EliteSettings fromJson(JsonObject obj) {
        return new EliteSettings(
                bool(obj, "elite_enabled", DEFAULT_ENABLED),
                (int) num(obj, "elite_min_level", DEFAULT_MIN_LEVEL),
                num(obj, "elite_base_chance", DEFAULT_BASE_CHANCE),
                num(obj, "elite_chance_per_level", DEFAULT_CHANCE_PER_LEVEL),
                (int) num(obj, "elite_max_affixes", DEFAULT_MAX_AFFIXES),
                (int) num(obj, "elite_max_per_level", DEFAULT_MAX_PER_LEVEL),
                bool(obj, "elite_particles", DEFAULT_PARTICLES),
                bool(obj, "elite_show_affix_names", DEFAULT_SHOW_AFFIX_NAMES));
    }

    /** Chance (0..1) that a mob of this level becomes elite: base + (level - min_level) * per_level. */
    public double chanceFor(int level) {
        return chanceFor(level, enabled);
    }

    /** Same, with the elite switch given (a region can force it on or off instead of the global elite_enabled). */
    public double chanceFor(int level, boolean spawnEnabled) {
        if (!spawnEnabled || level < minLevel) {
            return 0.0D;
        }
        return clamp01(baseChance + (level - minLevel) * chancePerLevel);
    }

    /**
     * Number of affixes of an elite of this level: 1 at the first two levels (4-5 by default), +1 every
     * two levels after that (2 from level 6), never above elite_max_affixes.
     */
    public int affixCountFor(int level) {
        int count = 1 + Math.max(0, level - minLevel) / 2;
        return Math.max(1, Math.min(maxAffixes, count));
    }

    public boolean isEnabled() { return enabled; }
    public int getMinLevel() { return minLevel; }
    public double getBaseChance() { return baseChance; }
    public double getChancePerLevel() { return chancePerLevel; }
    public int getMaxAffixes() { return maxAffixes; }
    public int getMaxPerLevel() { return maxPerLevel; }
    public boolean isParticles() { return particles; }
    public boolean isShowAffixNames() { return showAffixNames; }

    private static double clamp01(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }

    private static double num(JsonObject obj, String key, double def) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean bool(JsonObject obj, String key, boolean def) {
        try {
            return obj.has(key) ? obj.get(key).getAsBoolean() : def;
        } catch (Exception e) {
            return def;
        }
    }
}
