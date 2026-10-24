package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A set of difficulty values: per-level scaling percents, the extra bonus of mini bosses / bosses, the vanilla
 * bosses (Ender Dragon, Wither, Warden) and the raid level. Three are built in (NORMAL is the default of the mod);
 * players can create their own (see {@link PresetStore}).
 */
public final class DifficultyPreset {

    /** JSON keys, in the order of the value array. */
    public static final String[] KEYS = {
            "health", "damage", "armor", "speed", "armor_flat",
            "mini_health", "mini_armor", "mini_damage",
            "boss_health", "boss_armor", "boss_damage",
            "dragon_health", "dragon_damage", "wither_health", "wither_damage",
            "warden_health", "warden_damage", "raid_level"
    };
    public static final int HEALTH = 0, DAMAGE = 1, ARMOR = 2, SPEED = 3, ARMOR_FLAT = 4,
            MINI_HEALTH = 5, MINI_ARMOR = 6, MINI_DAMAGE = 7,
            BOSS_HEALTH = 8, BOSS_ARMOR = 9, BOSS_DAMAGE = 10,
            DRAGON_HEALTH = 11, DRAGON_DAMAGE = 12, WITHER_HEALTH = 13, WITHER_DAMAGE = 14,
            WARDEN_HEALTH = 15, WARDEN_DAMAGE = 16, RAID_LEVEL = 17;

    /** Built-in id (easy / normal / hardcore) or the name chosen by the player. */
    public final String id;
    public final boolean builtIn;
    public final double[] v;

    public DifficultyPreset(String id, boolean builtIn, double... values) {
        if (values.length != KEYS.length) throw new IllegalArgumentException("need " + KEYS.length + " values");
        this.id = id;
        this.builtIn = builtIn;
        this.v = values.clone();
    }

    //                                                  health damage armor speed flat | mini hp armor dmg | boss hp armor dmg | dragon hp x | wither hp x | warden hp x | raid
    public static final DifficultyPreset EASY = new DifficultyPreset("easy", true,
            15, 12, 12, 1.0, 0.5,   15, 12, 10,   30, 20, 20,   512, 1.5,   400, 1.5,   0, 1.0,   2);
    public static final DifficultyPreset NORMAL = new DifficultyPreset("normal", true,
            25, 20, 20, 2.0, 1.0,   25, 20, 15,   50, 35, 30,   1024, 2.0,  512, 2.0,   0, 1.0,   3);
    public static final DifficultyPreset HARDCORE = new DifficultyPreset("hardcore", true,
            40, 35, 35, 3.0, 1.5,   40, 30, 25,   80, 50, 45,   2048, 3.0,  1024, 3.0,  1000, 1.5, 4);

    public static List<DifficultyPreset> builtIns() {
        return List.of(EASY, NORMAL, HARDCORE);
    }

    public double get(int index) {
        return v[index];
    }

    public int raid() {
        return (int) Math.round(v[RAID_LEVEL]);
    }

    public boolean sameValues(double[] other) {
        for (int i = 0; i < v.length; i++) {
            if (Math.abs(v[i] - other[i]) >= 0.001D) return false;
        }
        return true;
    }

    /** The preset (built-in first, then custom) whose values equal the given ones, or null. */
    public static DifficultyPreset detect(List<DifficultyPreset> all, double[] values) {
        for (DifficultyPreset p : all) {
            if (p.sameValues(values)) return p;
        }
        return null;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", id);
        for (int i = 0; i < KEYS.length; i++) {
            if (i == RAID_LEVEL) o.addProperty(KEYS[i], (int) Math.round(v[i]));
            else o.addProperty(KEYS[i], v[i]);
        }
        return o;
    }

    /** @return the preset, or null if the entry has no name. Missing values take the NORMAL ones. */
    public static DifficultyPreset fromJson(JsonObject o) {
        if (!o.has("name") || !o.get("name").isJsonPrimitive() || o.get("name").getAsString().isBlank()) return null;
        double[] values = NORMAL.v.clone();
        for (int i = 0; i < KEYS.length; i++) {
            try {
                if (o.has(KEYS[i]) && o.get(KEYS[i]).isJsonPrimitive()) values[i] = o.get(KEYS[i]).getAsDouble();
            } catch (RuntimeException ignored) {
                // keep the default
            }
        }
        return new DifficultyPreset(o.get("name").getAsString().trim(), false, values);
    }
}
