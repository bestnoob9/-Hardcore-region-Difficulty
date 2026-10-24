package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonObject;

/**
 * Definition of one difficulty level (entry of "difficulty_levels" in hardcore_region_difficulty.json):
 * the default title, icon and title color of every region of that level.
 * A level above 5 that is defined here is allowed for regions (otherwise region levels stop at 5).
 */
public final class DifficultyLevelDef {
    private final int level;
    private final String title;
    private final String icon;
    private final String titleColor;
    // optional: total bonus of this level (in %, armor flat in points); null = use the scaling formula
    private final Double healthBonus;
    private final Double damageBonus;
    private final Double armorBonus;
    private final Double speedBonus;
    private final Double armorFlatBonus;
    // optional: item quality chances of this level (in %, see ItemQuality); null = use difficulty_quality_bonus
    private final Double qualityBonus;
    private final Double highQualityBonus;

    public DifficultyLevelDef(int level, String title, String icon, String titleColor) {
        this(level, title, icon, titleColor, null, null, null, null, null, null, null);
    }

    public DifficultyLevelDef(int level, String title, String icon, String titleColor,
                              Double healthBonus, Double damageBonus, Double armorBonus,
                              Double speedBonus, Double armorFlatBonus) {
        this(level, title, icon, titleColor, healthBonus, damageBonus, armorBonus, speedBonus, armorFlatBonus, null, null);
    }

    public DifficultyLevelDef(int level, String title, String icon, String titleColor,
                              Double healthBonus, Double damageBonus, Double armorBonus,
                              Double speedBonus, Double armorFlatBonus,
                              Double qualityBonus, Double highQualityBonus) {
        this.qualityBonus = qualityBonus;
        this.highQualityBonus = highQualityBonus;
        this.healthBonus = healthBonus;
        this.damageBonus = damageBonus;
        this.armorBonus = armorBonus;
        this.speedBonus = speedBonus;
        this.armorFlatBonus = armorFlatBonus;
        this.level = level;
        this.title = title == null ? "" : title;
        this.icon = icon == null ? "" : icon;
        this.titleColor = titleColor == null ? "" : titleColor;
    }

    public static DifficultyLevelDef fromJson(JsonObject obj) {
        if (!obj.has("level") || !obj.get("level").isJsonPrimitive()) {
            throw new IllegalArgumentException("difficulty level without \"level\"");
        }
        int level = obj.get("level").getAsInt();
        if (level < 1) {
            throw new IllegalArgumentException("difficulty level must be 1 or more");
        }
        return new DifficultyLevelDef(level,
                str(obj, "title"), str(obj, "icon"), str(obj, "title_color"),
                dbl(obj, "health_bonus_percent"), dbl(obj, "damage_bonus_percent"), dbl(obj, "armor_bonus_percent"),
                dbl(obj, "speed_bonus_percent"), dbl(obj, "armor_flat_bonus"),
                dbl(obj, "quality_bonus_percent"), dbl(obj, "high_quality_bonus_percent"));
    }

    private static Double dbl(JsonObject obj, String key) {
        try {
            return obj.has(key) && obj.get(key).isJsonPrimitive() ? Double.valueOf(obj.get(key).getAsDouble()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : "";
    }

    public int getLevel() { return level; }
    /** Empty = "Difficulty N". */
    public String getTitle() { return title; }
    /** Empty = default skull. */
    public String getIcon() { return icon; }
    /** Empty = default color. */
    public String getTitleColor() { return titleColor; }
    /** Total health bonus of the level in % (e.g. 150 = x2.5 health), or null = use the scaling formula. */
    public Double getHealthBonusPercent() { return healthBonus; }
    public Double getDamageBonusPercent() { return damageBonus; }
    public Double getArmorBonusPercent() { return armorBonus; }
    public Double getSpeedBonusPercent() { return speedBonus; }
    /** Total flat armor points added at this level, or null = use the scaling formula. */
    public Double getArmorFlatBonus() { return armorFlatBonus; }

    /**
     * Total bonus of this level to the chance of good item qualities in % (and the matching cut of bad ones),
     * e.g. 80 = x1.8, -50 = good qualities half as likely. Null = use the formula
     * (difficulty_quality_bonus per level) of the quality config.
     */
    public Double getQualityBonusPercent() { return qualityBonus; }
    /** Same for the high qualities (Superior and above); null = formula with high_tier_bonus_multiplier. */
    public Double getHighQualityBonusPercent() { return highQualityBonus; }

    public boolean hasStatOverride() {
        return healthBonus != null || damageBonus != null || armorBonus != null
                || speedBonus != null || armorFlatBonus != null;
    }
}
