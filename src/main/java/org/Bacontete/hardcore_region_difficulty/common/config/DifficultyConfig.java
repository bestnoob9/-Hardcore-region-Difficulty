package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

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
        Map<String, DifficultyRegion> regions = new LinkedHashMap<>();
        if (data.has("difficulty_regions") && data.get("difficulty_regions").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("difficulty_regions")) {
                try {
                    DifficultyRegion region = DifficultyRegion.fromJson(element.getAsJsonObject());
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
