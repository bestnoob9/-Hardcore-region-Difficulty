package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DifficultyRegion {
    private final String id;
    private final String regionName;
    private final String difficultyTitle;
    private final int difficultyLevel;
    private final List<String> dimensions;
    private final List<String> biomes;
    private final List<String> biomeTags;
    private final List<String> structures;

    public DifficultyRegion(String id, String regionName, String difficultyTitle,
                            int difficultyLevel, List<String> dimensions, List<String> biomes,
                            List<String> biomeTags, List<String> structures) {
        this.id = id;
        this.regionName = regionName;
        this.difficultyTitle = difficultyTitle;
        this.difficultyLevel = difficultyLevel;
        this.dimensions = immutable(dimensions);
        this.biomes = immutable(biomes);
        this.biomeTags = immutable(biomeTags);
        this.structures = immutable(structures);
    }

    private static List<String> immutable(List<String> list) {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    public static DifficultyRegion fromJson(JsonObject obj) {
        if (!obj.has("id") || obj.get("id").isJsonNull()) {
            throw new IllegalArgumentException("region without \"id\"");
        }
        String id = obj.get("id").getAsString();
        int level = obj.has("difficulty_level") ? obj.get("difficulty_level").getAsInt() : 1;
        String name = obj.has("region_name") ? obj.get("region_name").getAsString() : id;
        String title = obj.has("difficulty_title") ? obj.get("difficulty_title").getAsString() : "Difficulty " + level;

        return new DifficultyRegion(id, name, title, level,
                list(obj, "dimensions"), list(obj, "biomes"), list(obj, "biome_tags"), list(obj, "structures"));
    }

    private static List<String> list(JsonObject obj, String key) {
        List<String> out = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray(key)) {
                if (e.isJsonPrimitive()) out.add(e.getAsString());
            }
        }
        return out;
    }

    public boolean hasCriteria() {
        return !dimensions.isEmpty() || !biomes.isEmpty() || !biomeTags.isEmpty() || !structures.isEmpty();
    }

    public String getId() { return id; }
    public String getRegionName() { return regionName; }
    public String getDifficultyTitle() { return difficultyTitle; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public List<String> getDimensions() { return dimensions; }
    public List<String> getBiomes() { return biomes; }
    public List<String> getBiomeTags() { return biomeTags; }
    public List<String> getStructures() { return structures; }
}
