package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DangerCache {
    private static final Map<ResourceLocation, DangerAnalyzer.DangerResult> BIOME_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, StructureClassifier.Rating> STRUCTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, DimensionDangerEvaluator.Rating> DIMENSION_CACHE = new ConcurrentHashMap<>();
    private static final Map<ChunkKey, SpawnerChunk> SPAWNER_CACHE = new ConcurrentHashMap<>();

    public record ChunkKey(ResourceLocation dimension, long chunk) {}

    public record SpawnerChunk(long[] positions, long scannedAt) {}

    public static DangerAnalyzer.DangerResult getBiome(ResourceLocation biomeId) {
        return BIOME_CACHE.get(biomeId);
    }

    public static void putBiome(ResourceLocation biomeId, DangerAnalyzer.DangerResult result) {
        BIOME_CACHE.put(biomeId, result);
    }

    public static StructureClassifier.Rating getStructure(ResourceLocation structureId) {
        return STRUCTURE_CACHE.get(structureId);
    }

    public static void putStructure(ResourceLocation structureId, StructureClassifier.Rating rating) {
        STRUCTURE_CACHE.put(structureId, rating);
    }

    public static DimensionDangerEvaluator.Rating getDimension(ResourceLocation dimensionId) {
        return DIMENSION_CACHE.get(dimensionId);
    }

    public static void putDimension(ResourceLocation dimensionId, DimensionDangerEvaluator.Rating rating) {
        DIMENSION_CACHE.put(dimensionId, rating);
    }

    public static SpawnerChunk getSpawnerChunk(ChunkKey key) {
        return SPAWNER_CACHE.get(key);
    }

    public static void putSpawnerChunk(ChunkKey key, SpawnerChunk value) {
        SPAWNER_CACHE.put(key, value);
    }

    public static void invalidateSpawnerChunk(ChunkKey key) {
        SPAWNER_CACHE.remove(key);
    }

    public static void clear() {
        BIOME_CACHE.clear();
        STRUCTURE_CACHE.clear();
        DIMENSION_CACHE.clear();
        SPAWNER_CACHE.clear();
    }
}
