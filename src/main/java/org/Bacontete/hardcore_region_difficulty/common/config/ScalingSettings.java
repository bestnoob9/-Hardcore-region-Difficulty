package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static final double DEFAULT_BOSS_HEALTH = 150.0;

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

    public ScalingSettings() {
        this(DEFAULT_HEALTH_PERCENT, DEFAULT_DAMAGE_PERCENT, DEFAULT_ARMOR_PERCENT, DEFAULT_SPEED_PERCENT, DEFAULT_ARMOR_FLAT,
                Collections.emptyList(), DEFAULT_BOSS_HEALTH,
                DEFAULT_MINI_BOSS_HEALTH, DEFAULT_MINI_BOSS_ARMOR, DEFAULT_MINI_BOSS_DAMAGE,
                DEFAULT_BOSS_HEALTH_BONUS, DEFAULT_BOSS_ARMOR, DEFAULT_BOSS_DAMAGE);
    }

    public ScalingSettings(double healthPercent, double damagePercent, double armorPercent,
                           double speedPercent, double armorFlatPerLevel,
                           List<String> excludePatterns, double bossHealthThreshold,
                           double miniBossHealthPercent, double miniBossArmorPercent, double miniBossDamagePercent,
                           double bossHealthPercent, double bossArmorPercent, double bossDamagePercent) {
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
        return new ScalingSettings(health, damage, armor, speed, armorFlat, exclude, bossHealth,
                miniBossHealth, miniBossArmor, miniBossDamage, bossHealthBonus, bossArmor, bossDamage);
    }

    private static double num(JsonObject obj, String key, double def) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static int steps(int difficultyLevel) {
        return Math.max(0, difficultyLevel - 1);
    }

    private static double compound(double percent, int difficultyLevel) {
        int n = steps(difficultyLevel);
        if (n == 0 || percent == 0.0) return 0.0;
        return Math.pow(1.0 + percent / 100.0, n) - 1.0;
    }

    public double healthAmount(int difficultyLevel) { return compound(healthPercent, difficultyLevel); }

    public double armorMultiplyAmount(int difficultyLevel) { return compound(armorPercent, difficultyLevel); }

    public double armorFlatAmount(int difficultyLevel) { return steps(difficultyLevel) * armorFlatPerLevel; }

    public double damageAmount(int difficultyLevel) { return compound(damagePercent, difficultyLevel); }

    public double speedAmount(int difficultyLevel) { return compound(speedPercent, difficultyLevel); }

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
}
