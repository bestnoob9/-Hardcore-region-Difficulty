package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DifficultyRegion {
    private final String id;
    private final String regionName;
    private final String difficultyTitle;
    private final int difficultyLevel;
    private final List<String> dimensions;
    private final List<String> biomes;
    private final List<String> biomeTags;
    private final List<String> structures;
    private final String icon;
    private final String titleColor;
    private final List<MobEntry> mobEntries;
    /** Optional "loot" object of the region: only the keys it sets differ from the global loot settings. */
    private volatile JsonObject lootOverride;

    public DifficultyRegion(String id, String regionName, String difficultyTitle,
                            int difficultyLevel, List<String> dimensions, List<String> biomes,
                            List<String> biomeTags, List<String> structures) {
        this(id, regionName, difficultyTitle, difficultyLevel, dimensions, biomes, biomeTags, structures, "", "");
    }

    public DifficultyRegion(String id, String regionName, String difficultyTitle,
                            int difficultyLevel, List<String> dimensions, List<String> biomes,
                            List<String> biomeTags, List<String> structures, String icon, String titleColor) {
        this(id, regionName, difficultyTitle, difficultyLevel, dimensions, biomes, biomeTags, structures,
                icon, titleColor, Collections.emptyList());
    }

    public DifficultyRegion(String id, String regionName, String difficultyTitle,
                            int difficultyLevel, List<String> dimensions, List<String> biomes,
                            List<String> biomeTags, List<String> structures, String icon, String titleColor,
                            List<MobEntry> mobEntries) {
        this.mobEntries = mobEntries == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(mobEntries));
        this.id = id;
        this.regionName = regionName;
        this.difficultyTitle = difficultyTitle;
        this.difficultyLevel = difficultyLevel;
        this.dimensions = immutable(dimensions);
        this.biomes = immutable(biomes);
        this.biomeTags = immutable(biomeTags);
        this.structures = immutable(structures);
        this.icon = icon == null ? "" : icon;
        this.titleColor = titleColor == null ? "" : titleColor;
    }

    private static List<String> immutable(List<String> list) {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(list));
    }

    public static DifficultyRegion fromJson(JsonObject obj) {
        return fromJson(obj, Collections.emptyMap());
    }

    /**
     * @param levelDefs custom level definitions; they provide the title, icon and color a region does not set itself
     */
    public static DifficultyRegion fromJson(JsonObject obj, Map<Integer, DifficultyLevelDef> levelDefs) {
        if (!obj.has("id") || obj.get("id").isJsonNull()) {
            throw new IllegalArgumentException("region without \"id\"");
        }
        String id = obj.get("id").getAsString();
        int level = obj.has("difficulty_level") ? obj.get("difficulty_level").getAsInt() : 1;
        String name = obj.has("region_name") ? obj.get("region_name").getAsString() : id;
        DifficultyLevelDef def = levelDefs.get(level);

        String title = text(obj, "difficulty_title");
        if (title.isEmpty()) {
            title = def != null && !def.getTitle().isEmpty() ? def.getTitle() : "Difficulty " + level;
        }
        String icon = text(obj, "icon");
        if (icon.isEmpty() && def != null) {
            icon = def.getIcon();
        }
        String color = text(obj, "title_color");
        if (color.isEmpty() && def != null) {
            color = def.getTitleColor();
        }

        DifficultyRegion region = new DifficultyRegion(id, name, title, level,
                list(obj, "dimensions"), list(obj, "biomes"), list(obj, "biome_tags"), list(obj, "structures"),
                icon, color, mobEntries(obj, "entity_whitelist"));
        if (obj.has("loot") && obj.get("loot").isJsonObject()) {
            region.lootOverride = obj.getAsJsonObject("loot").deepCopy();
        }
        return region;
    }

    private static String text(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString().trim() : "";
    }

    private static List<MobEntry> mobEntries(JsonObject obj, String key) {
        List<MobEntry> out = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray(key)) {
                MobEntry entry = MobEntry.fromJson(e);
                if (entry != null) out.add(entry);
            }
        }
        return out;
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
    /** Icon of the region (own icon, else its level's icon). Empty = default skull. */
    public String getIcon() { return icon; }
    /** Title color of the region (own, else its level's). Empty = default. */
    public String getTitleColor() { return titleColor; }
    /**
     * Mob whitelist of this region (same patterns as exclude_entities: id, #tag, ~regex, *, !not).
     * Empty = every mob can be scaled here; otherwise only the matching mobs are.
     */
    public List<String> getEntityWhitelist() {
        List<String> out = new ArrayList<>();
        for (MobEntry e : mobEntries) out.add(e.getPattern());
        return out;
    }

    /** The whitelist entries of this region with their per-mob details. */
    public List<MobEntry> getMobEntries() { return mobEntries; }

    /** The "loot" object of this region (keys of LootSettings), or null if it uses the global loot settings. */
    public JsonObject getLootOverride() { return lootOverride; }

    /** Same override on a copy of the region made by the matcher (level cap, named structure). */
    public DifficultyRegion withLootOverride(JsonObject override) {
        this.lootOverride = override;
        return this;
    }
}
