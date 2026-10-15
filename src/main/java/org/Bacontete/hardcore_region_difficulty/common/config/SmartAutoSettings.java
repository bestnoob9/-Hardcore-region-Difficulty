package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class SmartAutoSettings {
    private static final int MAX_LEVEL = 10;

    public static final int DEFAULT_SPAWNER_SCAN_RADIUS = 48;

    public static final int MAX_SPAWNER_SCAN_RADIUS = 128;

    public static final int DEFAULT_UNKNOWN_DIMENSION_LEVEL = 2;

    public static final Map<Integer, Integer> DEFAULT_SPAWNER_THRESHOLDS = Collections.unmodifiableMap(
            new TreeMap<>(Map.of(2, 1, 3, 3, 4, 5, 5, 8)));
    public static final int DEFAULT_ENEMY_SCAN_RADIUS = 32;

    public static final int MAX_ENEMY_SCAN_RADIUS = 128;

    public static final Map<Integer, Integer> DEFAULT_ENEMY_THRESHOLDS = Collections.unmodifiableMap(
            new TreeMap<>(Map.of(2, 5, 3, 10, 4, 15, 5, 20)));

    public static final SmartAutoSettings DEFAULT = new SmartAutoSettings(true, true, 0, 3,
            Collections.emptyMap(), Collections.emptyMap());

    private final boolean analyzeStructures;
    private final boolean analyzeBiomes;
    private final int unknownStructureLevel;
    private final int structureSpawnLevel;
    private final Map<String, Integer> structureLevels;
    private final Map<String, Integer> biomeLevels;
    private final boolean analyzeSpawners;
    private final int spawnerScanRadius;

    private final TreeMap<Integer, Integer> spawnerLevelThresholds;
    private final boolean analyzeEnemies;
    private final int enemyScanRadius;

    private final TreeMap<Integer, Integer> enemyLevelThresholds;
    private final boolean analyzeDimensions;
    private final int unknownDimensionLevel;
    private final Map<String, Integer> dimensionLevels;

    public SmartAutoSettings(boolean analyzeStructures, boolean analyzeBiomes, int unknownStructureLevel,
                             Map<String, Integer> structureLevels, Map<String, Integer> biomeLevels) {
        this(analyzeStructures, analyzeBiomes, unknownStructureLevel, 3, structureLevels, biomeLevels);
    }

    public SmartAutoSettings(boolean analyzeStructures, boolean analyzeBiomes, int unknownStructureLevel,
                             int structureSpawnLevel,
                             Map<String, Integer> structureLevels, Map<String, Integer> biomeLevels) {
        this(analyzeStructures, analyzeBiomes, unknownStructureLevel, structureSpawnLevel,
                structureLevels, biomeLevels, true, DEFAULT_SPAWNER_SCAN_RADIUS, DEFAULT_SPAWNER_THRESHOLDS);
    }

    public SmartAutoSettings(boolean analyzeStructures, boolean analyzeBiomes, int unknownStructureLevel,
                             int structureSpawnLevel,
                             Map<String, Integer> structureLevels, Map<String, Integer> biomeLevels,
                             boolean analyzeSpawners, int spawnerScanRadius,
                             Map<Integer, Integer> spawnerLevelThresholds) {
        this(analyzeStructures, analyzeBiomes, unknownStructureLevel, structureSpawnLevel,
                structureLevels, biomeLevels, analyzeSpawners, spawnerScanRadius, spawnerLevelThresholds,
                true, DEFAULT_ENEMY_SCAN_RADIUS, DEFAULT_ENEMY_THRESHOLDS,
                true, DEFAULT_UNKNOWN_DIMENSION_LEVEL, Collections.emptyMap());
    }

    public SmartAutoSettings(boolean analyzeStructures, boolean analyzeBiomes, int unknownStructureLevel,
                             int structureSpawnLevel,
                             Map<String, Integer> structureLevels, Map<String, Integer> biomeLevels,
                             boolean analyzeSpawners, int spawnerScanRadius,
                             Map<Integer, Integer> spawnerLevelThresholds,
                             boolean analyzeEnemies, int enemyScanRadius,
                             Map<Integer, Integer> enemyLevelThresholds,
                             boolean analyzeDimensions, int unknownDimensionLevel,
                             Map<String, Integer> dimensionLevels) {
        this.analyzeStructures = analyzeStructures;
        this.analyzeBiomes = analyzeBiomes;
        this.unknownStructureLevel = clamp(unknownStructureLevel);
        this.structureSpawnLevel = clamp(structureSpawnLevel);
        this.structureLevels = Collections.unmodifiableMap(new LinkedHashMap<>(structureLevels));
        this.biomeLevels = Collections.unmodifiableMap(new LinkedHashMap<>(biomeLevels));
        this.analyzeSpawners = analyzeSpawners;
        this.spawnerScanRadius = Math.max(1, Math.min(MAX_SPAWNER_SCAN_RADIUS, spawnerScanRadius));
        this.spawnerLevelThresholds = new TreeMap<>(spawnerLevelThresholds == null || spawnerLevelThresholds.isEmpty()
                ? DEFAULT_SPAWNER_THRESHOLDS : spawnerLevelThresholds);
        this.analyzeEnemies = analyzeEnemies;
        this.enemyScanRadius = Math.max(1, Math.min(MAX_ENEMY_SCAN_RADIUS, enemyScanRadius));
        this.enemyLevelThresholds = new TreeMap<>(enemyLevelThresholds == null || enemyLevelThresholds.isEmpty()
                ? DEFAULT_ENEMY_THRESHOLDS : enemyLevelThresholds);
        this.analyzeDimensions = analyzeDimensions;
        this.unknownDimensionLevel = clamp(unknownDimensionLevel);
        this.dimensionLevels = Collections.unmodifiableMap(new LinkedHashMap<>(dimensionLevels));
    }

    public static SmartAutoSettings fromJson(JsonObject obj) {
        boolean structures = bool(obj, "analyze_structures", true);
        boolean biomes = bool(obj, "analyze_biomes", true);
        int unknown = intOf(obj, "unknown_structure_level", 0);
        int spawn = intOf(obj, "structure_spawn_level", 3);
        boolean spawners = bool(obj, "analyze_spawners", true);
        int radius = intOf(obj, "spawner_scan_radius", DEFAULT_SPAWNER_SCAN_RADIUS);
        boolean enemies = bool(obj, "analyze_enemies", true);
        int enemyRadius = intOf(obj, "enemy_scan_radius", DEFAULT_ENEMY_SCAN_RADIUS);

        boolean dimensions = bool(obj, "analyze_dimensions", true);
        int unknownDimension = intOf(obj, "unknown_dimension_level", DEFAULT_UNKNOWN_DIMENSION_LEVEL);
        return new SmartAutoSettings(structures, biomes, unknown, spawn,
                levels(obj, "structure_levels"), levels(obj, "biome_levels"),
                spawners, radius, spawnerThresholds(obj),
                enemies, enemyRadius, enemyThresholds(obj),
                dimensions, unknownDimension, levels(obj, "dimension_levels"));
    }

    private static Map<Integer, Integer> spawnerThresholds(JsonObject obj) {
        String key = "spawner_level_thresholds";
        if (!obj.has(key) || !obj.get(key).isJsonObject()) return DEFAULT_SPAWNER_THRESHOLDS;
        Map<Integer, Integer> out = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject(key).entrySet()) {
            try {
                int level = Integer.parseInt(e.getKey().trim());
                int min = e.getValue().getAsInt();
                if (level < 1 || level > MAX_LEVEL || min < 1) {
                    throw new IllegalArgumentException("level must be 1-" + MAX_LEVEL + " and the count at least 1");
                }
                out.put(level, min);
            } catch (Exception ex) {
                HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{}: '{}' ignored (key = level, value = minimum spawner count)",
                        key, e.getKey());
            }
        }
        if (out.isEmpty()) {
            HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{} has no valid entry, using the defaults", key);
            return DEFAULT_SPAWNER_THRESHOLDS;
        }
        return out;
    }

    private static Map<Integer, Integer> enemyThresholds(JsonObject obj) {
        String key = "enemy_level_thresholds";
        if (!obj.has(key) || !obj.get(key).isJsonObject()) return DEFAULT_ENEMY_THRESHOLDS;
        Map<Integer, Integer> out = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject(key).entrySet()) {
            try {
                int level = Integer.parseInt(e.getKey().trim());
                int min = e.getValue().getAsInt();
                if (level < 1 || level > MAX_LEVEL || min < 1) {
                    throw new IllegalArgumentException("level must be 1-" + MAX_LEVEL + " and the count at least 1");
                }
                out.put(level, min);
            } catch (Exception ex) {
                HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{}: '{}' ignored (key = level, value = minimum enemy count)",
                        key, e.getKey());
            }
        }
        if (out.isEmpty()) {
            HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{} has no valid entry, using the defaults", key);
            return DEFAULT_ENEMY_THRESHOLDS;
        }
        return out;
    }

    private static int intOf(JsonObject obj, String key, int def) {
        try {
            return obj.has(key) ? obj.get(key).getAsInt() : def;
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{} must be a number", key);
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

    private static Map<String, Integer> levels(JsonObject obj, String key) {
        Map<String, Integer> out = new LinkedHashMap<>(); 
        if (!obj.has(key) || !obj.get(key).isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject(key).entrySet()) {
            String pattern = e.getKey().trim();
            if (pattern.isEmpty() || pattern.startsWith("!")) {
                HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{}: '{}' ignored (empty or negated key)", key, e.getKey());
                continue;
            }
            try {
                out.put(pattern, clamp(e.getValue().getAsInt()));
            } catch (Exception ex) {
                HardcoreRegionDifficulty.LOGGER.warn("smart_auto_settings.{}: '{}' needs a number level", key, pattern);
            }
        }
        return out;
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    public boolean analyzeStructures() { return analyzeStructures; }
    public boolean analyzeBiomes() { return analyzeBiomes; }

    public int unknownStructureLevel() { return unknownStructureLevel; }

    public int structureSpawnLevel() { return structureSpawnLevel; }
    public Map<String, Integer> structureLevels() { return structureLevels; }
    public Map<String, Integer> biomeLevels() { return biomeLevels; }

    public boolean analyzeDimensions() { return analyzeDimensions; }

    public int unknownDimensionLevel() { return unknownDimensionLevel; }

    public Map<String, Integer> dimensionLevels() { return dimensionLevels; }

    public boolean analyzeSpawners() { return analyzeSpawners; }

    public int spawnerScanRadius() { return spawnerScanRadius; }

    public Map<Integer, Integer> spawnerLevelThresholds() { return Collections.unmodifiableMap(spawnerLevelThresholds); }

    public boolean analyzeEnemies() { return analyzeEnemies; }

    public int enemyScanRadius() { return enemyScanRadius; }

    public Map<Integer, Integer> enemyLevelThresholds() { return Collections.unmodifiableMap(enemyLevelThresholds); }

    public int spawnerLevelFor(int spawnerCount) {
        int level = 0;
        for (Map.Entry<Integer, Integer> e : spawnerLevelThresholds.entrySet()) {
            if (spawnerCount >= e.getValue()) level = Math.max(level, e.getKey());
        }
        return level;
    }

    public int enemyLevelFor(int enemyCount) {
        int level = 0;
        for (Map.Entry<Integer, Integer> e : enemyLevelThresholds.entrySet()) {
            if (enemyCount >= e.getValue()) level = Math.max(level, e.getKey());
        }
        return level;
    }
}
