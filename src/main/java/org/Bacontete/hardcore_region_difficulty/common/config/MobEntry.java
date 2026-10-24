package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * One entry of a mob whitelist: a pattern (id, #tag, ~regex, *, !not) plus optional per-mob details.
 * In the config file an entry is either a plain string (no details) or an object:
 * {"entity": "minecraft:zombie", "level": 4, "health_bonus_percent": 200, ...}.
 * A detail that is null = automatic (the normal level formula).
 * Details of a mob replace the values of its level: the *_bonus_percent / armor_flat_bonus values are the
 * total bonus of this mob (100 = double the base value), "level" replaces the difficulty level of the mob.
 */
public final class MobEntry {
    private final String pattern;
    private final Integer level;
    private final Double health;
    private final Double damage;
    private final Double armor;
    private final Double armorFlat;
    private final Double speed;
    private final Double knockbackResistance;
    private final Double followRange;

    public MobEntry(String pattern, Integer level, Double health, Double damage, Double armor, Double armorFlat,
                    Double speed, Double knockbackResistance, Double followRange) {
        this.pattern = pattern == null ? "" : pattern.trim();
        this.level = level != null && level >= 1 ? level : null;
        this.health = health;
        this.damage = damage;
        this.armor = armor;
        this.armorFlat = armorFlat;
        this.speed = speed;
        this.knockbackResistance = knockbackResistance;
        this.followRange = followRange;
    }

    public static MobEntry plain(String pattern) {
        return new MobEntry(pattern, null, null, null, null, null, null, null, null);
    }

    /** Reads a string or an object; null when the element is not a usable entry. */
    public static MobEntry fromJson(JsonElement e) {
        if (e == null) {
            return null;
        }
        if (e.isJsonPrimitive()) {
            String s = e.getAsString().trim();
            return s.isEmpty() ? null : plain(s);
        }
        if (!e.isJsonObject()) {
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        String pattern = o.has("entity") && o.get("entity").isJsonPrimitive() ? o.get("entity").getAsString().trim() : "";
        if (pattern.isEmpty()) {
            return null;
        }
        Integer level = null;
        try {
            if (o.has("level") && o.get("level").isJsonPrimitive()) {
                level = o.get("level").getAsInt();
            }
        } catch (Exception ignored) {
            // not a number: automatic
        }
        return new MobEntry(pattern, level, dbl(o, "health_bonus_percent"), dbl(o, "damage_bonus_percent"),
                dbl(o, "armor_bonus_percent"), dbl(o, "armor_flat_bonus"), dbl(o, "speed_bonus_percent"),
                dbl(o, "knockback_resistance"), dbl(o, "follow_range_bonus_percent"));
    }

    private static Double dbl(JsonObject o, String key) {
        try {
            return o.has(key) && o.get(key).isJsonPrimitive() ? Double.valueOf(o.get(key).getAsDouble()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** A plain string when there are no details, else an object. */
    public JsonElement toJson() {
        if (!hasDetails()) {
            return new JsonPrimitive(pattern);
        }
        JsonObject o = new JsonObject();
        o.addProperty("entity", pattern);
        if (level != null) o.addProperty("level", level);
        if (health != null) o.addProperty("health_bonus_percent", health);
        if (damage != null) o.addProperty("damage_bonus_percent", damage);
        if (armor != null) o.addProperty("armor_bonus_percent", armor);
        if (armorFlat != null) o.addProperty("armor_flat_bonus", armorFlat);
        if (speed != null) o.addProperty("speed_bonus_percent", speed);
        if (knockbackResistance != null) o.addProperty("knockback_resistance", knockbackResistance);
        if (followRange != null) o.addProperty("follow_range_bonus_percent", followRange);
        return o;
    }

    public String getPattern() { return pattern; }
    /** Difficulty level of this mob instead of the level of its region, or null. */
    public Integer getLevel() { return level; }
    public Double getHealthBonusPercent() { return health; }
    public Double getDamageBonusPercent() { return damage; }
    public Double getArmorBonusPercent() { return armor; }
    public Double getArmorFlatBonus() { return armorFlat; }
    public Double getSpeedBonusPercent() { return speed; }
    /** Knockback resistance added (0 to 1, 1 = immune to knockback), or null. */
    public Double getKnockbackResistance() { return knockbackResistance; }
    /** Follow range bonus in % (how far the mob notices targets), or null. */
    public Double getFollowRangeBonusPercent() { return followRange; }

    /** Any stat (not the level) is set. */
    public boolean hasStatOverride() {
        return health != null || damage != null || armor != null || armorFlat != null
                || speed != null || knockbackResistance != null || followRange != null;
    }

    public boolean hasDetails() {
        return level != null || hasStatOverride();
    }
}
