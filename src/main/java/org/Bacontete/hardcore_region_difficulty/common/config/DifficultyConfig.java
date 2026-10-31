package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.entity.BossLevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DifficultyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson WRITE_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DEFAULT_RESOURCE = "/defaults/hardcore_region_difficulty.json";
    private static volatile DifficultyConfig INSTANCE;

    private final File configFile;

    private volatile Map<String, DifficultyRegion> difficultyRegions = new LinkedHashMap<>();
    private volatile List<DifficultyRegion> regionList = Collections.emptyList();
    private volatile Map<Integer, DifficultyLevelDef> levelDefs = Collections.emptyMap();
    private volatile ScalingSettings scalingSettings = new ScalingSettings();
    private volatile EnchantmentSettings enchantmentSettings = new EnchantmentSettings();
    private volatile TitleDisplaySettings titleDisplaySettings = new TitleDisplaySettings();
    private volatile SmartAutoSettings smartAutoSettings = SmartAutoSettings.DEFAULT;
    private volatile CompatSettings compatSettings = CompatSettings.DEFAULT;

    private volatile boolean smartAutoEnabled = true;
    private volatile boolean anyStructureRegion = false;

    private volatile long loadedModified = -1L;

    public DifficultyConfig(File configDir) {
        this.configFile = new File(configDir, "hardcore_region_difficulty.json");
        INSTANCE = this;
        loadConfig();
    }

    public boolean hasFileChanged() {
        return configFile.exists() && configFile.lastModified() != loadedModified;
    }

    public File getConfigDirectory() { return configFile.getParentFile(); }

    public static DifficultyConfig getInstance() {
        return INSTANCE;
    }

    public synchronized boolean loadConfig() {
        if (!configFile.exists()) {
            writeDefaultConfig();
        }
        loadedModified = configFile.lastModified();

        JsonObject data = null;
        boolean ok = false;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root != null && root.isJsonObject()) {
                data = root.getAsJsonObject();
                ok = true;
            } else {
                HardcoreRegionDifficulty.LOGGER.error("Config root must be a JSON object: {}", configFile);
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Failed to read config {} - keeping previous/default settings", configFile, e);
        }

        if (data == null) {

            if (difficultyRegions.isEmpty()) {
                data = readBundledDefault();
            } else {
                return false;
            }
        }

        parse(data);
        HardcoreRegionDifficulty.LOGGER.info("Loaded Hardcore Region Difficulty config ({} regions)", difficultyRegions.size());
        return ok;
    }

    private void parse(JsonObject data) {
        Map<Integer, DifficultyLevelDef> levels = new java.util.TreeMap<>();
        if (data.has("difficulty_levels") && data.get("difficulty_levels").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("difficulty_levels")) {
                try {
                    DifficultyLevelDef def = DifficultyLevelDef.fromJson(element.getAsJsonObject());
                    levels.put(def.getLevel(), def);
                } catch (Exception e) {
                    HardcoreRegionDifficulty.LOGGER.warn("Skipping invalid difficulty level entry: {}", e.getMessage());
                }
            }
        }
        for (DifficultyLevelDef def : levels.values()) {
            if (!DifficultyIcons.isValid(def.getIcon())) {
                HardcoreRegionDifficulty.LOGGER.warn("Difficulty level {} has an unknown icon '{}' (default skull is used)", def.getLevel(), def.getIcon());
            }
        }

        Map<String, DifficultyRegion> regions = new LinkedHashMap<>();
        if (data.has("difficulty_regions") && data.get("difficulty_regions").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("difficulty_regions")) {
                try {
                    DifficultyRegion region = DifficultyRegion.fromJson(element.getAsJsonObject(), levels);
                    if (!region.hasCriteria()) {
                        HardcoreRegionDifficulty.LOGGER.warn("Region '{}' has no dimensions/biomes/biome_tags/structures and will never match", region.getId());
                    }
                    regions.put(region.getId(), region);
                } catch (Exception e) {
                    HardcoreRegionDifficulty.LOGGER.warn("Skipping invalid region entry: {}", e.getMessage());
                }
            }
        }

        ScalingSettings scaling = new ScalingSettings();
        EnchantmentSettings enchant = new EnchantmentSettings();
        TitleDisplaySettings title = new TitleDisplaySettings();

        boolean smart = true;
        SmartAutoSettings smartSettings = SmartAutoSettings.DEFAULT;
        CompatSettings compat = CompatSettings.DEFAULT;
        try {
            if (data.has("scaling_settings")) scaling = ScalingSettings.fromJson(data.getAsJsonObject("scaling_settings"));
            if (data.has("enchantment_settings")) enchant = EnchantmentSettings.fromJson(data.getAsJsonObject("enchantment_settings"));
            if (data.has("title_display_settings")) title = TitleDisplaySettings.fromJson(data.getAsJsonObject("title_display_settings"));
            if (data.has("smart_auto_settings") && data.get("smart_auto_settings").isJsonObject()) {
                smartSettings = SmartAutoSettings.fromJson(data.getAsJsonObject("smart_auto_settings"));
            }
            if (data.has("mod_compat_settings") && data.get("mod_compat_settings").isJsonObject()) {
                compat = CompatSettings.fromJson(data.getAsJsonObject("mod_compat_settings"));
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.warn("Invalid settings section, using defaults for it: {}", e.getMessage());
        }

        boolean structureRegion = false;
        for (DifficultyRegion r : regions.values()) {
            if (!r.getStructures().isEmpty()) {
                structureRegion = true;
                break;
            }
        }

        scaling.setLevelDefs(levels);
        this.levelDefs = Collections.unmodifiableMap(levels);
        this.regionList = Collections.unmodifiableList(new ArrayList<>(regions.values()));
        this.difficultyRegions = regions;
        this.scalingSettings = scaling;
        this.enchantmentSettings = enchant;
        this.titleDisplaySettings = title;
        this.smartAutoEnabled = smart;
        this.smartAutoSettings = smartSettings;
        this.compatSettings = compat;
        this.anyStructureRegion = structureRegion;
    }

    private JsonObject readBundledDefault() {
        try (InputStream in = DifficultyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return GSON.fromJson(r, JsonObject.class);
                }
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Failed to read bundled default config", e);
        }
        return minimalDefault();
    }

    private JsonObject minimalDefault() {
        JsonObject root = new JsonObject();
        var regions = new com.google.gson.JsonArray();
        regions.add(dimensionRegion("overworld_default", "Overworld", 1, "minecraft:overworld"));
        regions.add(dimensionRegion("nether_default", "The Nether", 2, "minecraft:the_nether"));
        regions.add(dimensionRegion("end_default", "The End", 3, "minecraft:the_end"));
        root.add("difficulty_regions", regions);
        return root;
    }

    private static JsonObject dimensionRegion(String id, String name, int level, String dimension) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("region_name", name);
        o.addProperty("difficulty_title", "Difficulty " + toRoman(level));
        o.addProperty("difficulty_level", level);
        var dims = new com.google.gson.JsonArray();
        dims.add(dimension);
        o.add("dimensions", dims);
        return o;
    }

    private static String toRoman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; default -> String.valueOf(n); };
    }

    private void writeDefaultConfig() {
        try (InputStream in = DifficultyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.createDirectories(configFile.toPath().getParent());
                Files.copy(in, configFile.toPath());
                HardcoreRegionDifficulty.LOGGER.info("Created default config file");
                return;
            }
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Failed to create default config", e);
        }
    }

    /**
     * Writes mod_compat_settings.deeperdarker_cataclysm into the config file (everything else in the file is kept)
     * and reloads it. Used by the Cloth Config screen. Returns false if the file could not be read or written.
     */
    public synchronized boolean setDeeperDarkerCataclysm(boolean value) {
        if (!configFile.exists()) {
            writeDefaultConfig();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                HardcoreRegionDifficulty.LOGGER.error("Config root must be a JSON object, not changing {}", configFile);
                return false;
            }
            root = element.getAsJsonObject();
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read {} to change mod_compat_settings", configFile, e);
            return false;
        }
        JsonObject compat = root.has("mod_compat_settings") && root.get("mod_compat_settings").isJsonObject()
                ? root.getAsJsonObject("mod_compat_settings") : new JsonObject();
        compat.addProperty("deeperdarker_cataclysm", value);
        root.add("mod_compat_settings", compat);
        try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            WRITE_GSON.toJson(root, writer);
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", configFile, e);
            return false;
        }
        return loadConfig();
    }

    /**
     * Writes smart_auto_settings.analyze_enemies, enemy_scan_radius and enemy_level_thresholds into the config file
     * (everything else in the file is kept) and reloads it. Used by the Cloth Config screen.
     * Returns false if the file could not be read or written.
     */
    public synchronized boolean setEnemySettings(boolean analyze, int radius, Map<Integer, Integer> thresholds) {
        if (!configFile.exists()) {
            writeDefaultConfig();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                HardcoreRegionDifficulty.LOGGER.error("Config root must be a JSON object, not changing {}", configFile);
                return false;
            }
            root = element.getAsJsonObject();
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read {} to change smart_auto_settings", configFile, e);
            return false;
        }
        JsonObject smart = root.has("smart_auto_settings") && root.get("smart_auto_settings").isJsonObject()
                ? root.getAsJsonObject("smart_auto_settings") : new JsonObject();
        smart.addProperty("analyze_enemies", analyze);
        smart.addProperty("enemy_scan_radius", radius);
        JsonObject levels = new JsonObject();
        for (Map.Entry<Integer, Integer> e : new java.util.TreeMap<>(thresholds).entrySet()) {
            levels.addProperty(String.valueOf(e.getKey()), e.getValue());
        }
        smart.add("enemy_level_thresholds", levels);
        root.add("smart_auto_settings", smart);
        try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            WRITE_GSON.toJson(root, writer);
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", configFile, e);
            return false;
        }
        return loadConfig();
    }

    /** Custom level definitions of the file, by level. */
    public Map<Integer, DifficultyLevelDef> getLevelDefs() { return levelDefs; }

    /**
     * A level that the file defines in "difficulty_levels" above the levels taken by the boss classes
     * ({@link BossLevel#lastConfigLevel()}) is allowed for regions (regions are normally capped at 5;
     * the boss classes only use their entries for the boss title and never as a region level).
     */
    public boolean isUnlockedLevel(int level) {
        return level > BossLevel.lastConfigLevel() && level <= BossLevel.MAX_CUSTOM_LEVEL && levelDefs.containsKey(level);
    }

    /** Icon of the level definition, or "" (default skull). */
    public String levelIcon(int level) {
        DifficultyLevelDef def = levelDefs.get(level);
        return def == null ? "" : def.getIcon();
    }

    /** Title color of the level definition, or "". */
    public String levelColor(int level) {
        DifficultyLevelDef def = levelDefs.get(level);
        return def == null ? "" : def.getTitleColor();
    }

    /**
     * Replaces "difficulty_regions" and "difficulty_levels" in the file (everything else is kept).
     * The new data is used after the next reload (the server reloads on its own when the file changes);
     * call {@link #loadConfig()} to use it at once. Returns false if the file could not be read or written.
     */
    public synchronized boolean writeRegionsAndLevels(com.google.gson.JsonArray regions, com.google.gson.JsonArray levels) {
        return writeRegionsAndLevels(regions, levels, null);
    }

    /** Same, and also merges the given keys into "scaling_settings" (other keys of it are kept). */
    public synchronized boolean writeRegionsAndLevels(com.google.gson.JsonArray regions, com.google.gson.JsonArray levels,
                                                      JsonObject scalingPatch) {
        if (!configFile.exists()) {
            writeDefaultConfig();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                HardcoreRegionDifficulty.LOGGER.error("Config root must be a JSON object, not changing {}", configFile);
                return false;
            }
            root = element.getAsJsonObject();
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read {} to change the regions", configFile, e);
            return false;
        }
        if (regions != null) {
            root.add("difficulty_regions", regions); // null = keep the regions of the file
        }
        root.add("difficulty_levels", levels);
        if (scalingPatch != null) {
            JsonObject scaling = root.has("scaling_settings") && root.get("scaling_settings").isJsonObject()
                    ? root.getAsJsonObject("scaling_settings") : new JsonObject();
            for (var entry : scalingPatch.entrySet()) {
                scaling.add(entry.getKey(), entry.getValue());
            }
            root.add("scaling_settings", scaling);
        }
        try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            WRITE_GSON.toJson(root, writer);
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", configFile, e);
            return false;
        }
        return true;
    }

    /**
     * Saves what the Scan tab edits: level overrides into smart_auto_settings.structure_levels / dimension_levels,
     * and the scan_* regions (custom mob stats of one structure or dimension) into difficulty_regions: a region
     * with the same id is replaced, ids in removeRegionIds are deleted. Everything else is kept.
     * Call {@link #loadConfig()} afterwards to use it at once. Returns false if the file could not be read or written.
     */
    public synchronized boolean writeScanOverrides(Map<String, Integer> structureLevels, Map<String, Integer> dimensionLevels,
                                                   List<JsonObject> upsertRegions, java.util.Set<String> removeRegionIds,
                                                   java.util.Set<String> resetStructures, java.util.Set<String> resetDimensions) {
        if (!configFile.exists()) {
            writeDefaultConfig();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                HardcoreRegionDifficulty.LOGGER.error("Config root must be a JSON object, not changing {}", configFile);
                return false;
            }
            root = element.getAsJsonObject();
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read {} to save the scan edits", configFile, e);
            return false;
        }
        JsonObject smart = root.has("smart_auto_settings") && root.get("smart_auto_settings").isJsonObject()
                ? root.getAsJsonObject("smart_auto_settings") : new JsonObject();
        mergeLevels(smart, "structure_levels", structureLevels, resetStructures);
        mergeLevels(smart, "dimension_levels", dimensionLevels, resetDimensions);
        root.add("smart_auto_settings", smart);

        com.google.gson.JsonArray old = root.has("difficulty_regions") && root.get("difficulty_regions").isJsonArray()
                ? root.getAsJsonArray("difficulty_regions") : new com.google.gson.JsonArray();
        com.google.gson.JsonArray regions = new com.google.gson.JsonArray();
        java.util.Set<String> replaced = new java.util.HashSet<>();
        for (JsonObject r : upsertRegions) replaced.add(r.get("id").getAsString());
        for (JsonElement e : old) {
            String id = e.isJsonObject() && e.getAsJsonObject().has("id") && e.getAsJsonObject().get("id").isJsonPrimitive()
                    ? e.getAsJsonObject().get("id").getAsString() : null;
            if (id != null && (replaced.contains(id) || removeRegionIds.contains(id))) continue;
            regions.add(e);
        }
        for (JsonObject r : upsertRegions) regions.add(r);
        root.add("difficulty_regions", regions);
        try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            WRITE_GSON.toJson(root, writer);
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", configFile, e);
            return false;
        }
        return true;
    }

    private static void mergeLevels(JsonObject smart, String key, Map<String, Integer> values, java.util.Set<String> reset) {
        if ((values == null || values.isEmpty()) && (reset == null || reset.isEmpty())) return;
        JsonObject old = smart.has(key) && smart.get(key).isJsonObject() ? smart.getAsJsonObject(key) : new JsonObject();
        // The game takes the FIRST entry that matches, so the new exact ids go first: an older pattern such as
        // "deeperdarker:*" must not hide them.
        JsonObject target = new JsonObject();
        if (values != null) {
            for (Map.Entry<String, Integer> e : values.entrySet()) {
                target.addProperty(e.getKey(), e.getValue());
            }
        }
        for (Map.Entry<String, JsonElement> e : old.entrySet()) {
            if (target.has(e.getKey())) continue;
            if (reset != null && reset.contains(e.getKey())) continue;
            target.add(e.getKey(), e.getValue());
        }
        smart.add(key, target);
    }

    /**
     * Writes the mob whitelists: "whitelist_entities" of scaling_settings and "entity_whitelist" of the regions
     * with the given ids (an empty list removes the key of a region). Everything else is kept.
     */
    public synchronized boolean writeWhitelists(List<MobEntry> global, Map<String, List<MobEntry>> regionLists) {
        return writeWhitelists(global, regionLists, null);
    }

    /**
     * Same, and also the elite switch of the regions: "elite_enabled" is set to true / false for the given ids,
     * removed when the value is null (= follow the global setting). Regions not in the map are not touched.
     */
    public synchronized boolean writeWhitelists(List<MobEntry> global, Map<String, List<MobEntry>> regionLists,
                                                Map<String, Boolean> regionElite) {
        if (!configFile.exists()) {
            writeDefaultConfig();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                HardcoreRegionDifficulty.LOGGER.error("Config root must be a JSON object, not changing {}", configFile);
                return false;
            }
            root = element.getAsJsonObject();
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read {} to change the whitelists", configFile, e);
            return false;
        }
        JsonObject scaling = root.has("scaling_settings") && root.get("scaling_settings").isJsonObject()
                ? root.getAsJsonObject("scaling_settings") : new JsonObject();
        scaling.add("whitelist_entities", entriesToJson(global));
        root.add("scaling_settings", scaling);
        if (root.has("difficulty_regions") && root.get("difficulty_regions").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("difficulty_regions")) {
                if (!element.isJsonObject() || !element.getAsJsonObject().has("id")) {
                    continue;
                }
                JsonObject region = element.getAsJsonObject();
                List<MobEntry> list = regionLists.get(region.get("id").getAsString());
                if (regionElite != null && regionElite.containsKey(region.get("id").getAsString())) {
                    Boolean elite = regionElite.get(region.get("id").getAsString());
                    if (elite == null) {
                        region.remove("elite_enabled");
                    } else {
                        region.addProperty("elite_enabled", elite);
                    }
                }
                if (list == null) {
                    continue;
                }
                if (!list.isEmpty()) {
                    region.add("entity_whitelist", entriesToJson(list));
                } else {
                    region.remove("entity_whitelist");
                }
            }
        }
        try (Writer writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            WRITE_GSON.toJson(root, writer);
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", configFile, e);
            return false;
        }
        return true;
    }

    private static com.google.gson.JsonArray entriesToJson(List<MobEntry> entries) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (MobEntry entry : entries) {
            if (entry != null && !entry.getPattern().isBlank()) {
                array.add(entry.toJson());
            }
        }
        return array;
    }

    private static com.google.gson.JsonArray toJsonArray(List<String> values) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                array.add(v.trim());
            }
        }
        return array;
    }

    public File getConfigFile() { return configFile; }

    public Map<String, DifficultyRegion> getDifficultyRegions() { return difficultyRegions; }

    public List<DifficultyRegion> getRegionList() { return regionList; }
    public ScalingSettings getScalingSettings() { return scalingSettings; }
    public EnchantmentSettings getEnchantmentSettings() { return enchantmentSettings; }
    public TitleDisplaySettings getTitleDisplaySettings() { return titleDisplaySettings; }
    public boolean isSmartAutoEnabled() { return smartAutoEnabled; }
    public SmartAutoSettings getSmartAutoSettings() { return smartAutoSettings; }
    public CompatSettings getCompatSettings() { return compatSettings; }
    public boolean hasStructureRegions() { return anyStructureRegion; }
}
