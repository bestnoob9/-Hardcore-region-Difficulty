package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteMode;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerScanner;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PatternMatcher;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Scan" tab: the dimensions and structures found by the last scan (hardcore_region_difficulty_scan.json,
 * written at server start and by /hrd scan). Each one can get its own level and, if wanted, its own mob stats.
 *
 * Saving: a changed level goes to smart_auto_settings.structure_levels / dimension_levels. Custom stats make a
 * region "scan_..." (that structure or dimension, entity_whitelist "*" with the stats); turning them off removes it.
 */
public final class ScanEditorTabs {

    private static final String K = "hardcore_region_difficulty.config.";
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final String ID_PREFIX = "scan_";

    private ScanEditorTabs() {}

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    /** One row of the tab. */
    private static final class Row {
        boolean dimension;
        String id;
        int scannedLevel;
        String source;
        /** Level shown when the screen opened (override of the config file, or a region of this tab, or the scan). */
        int initialLevel;
        /** The config file already has a level override for this entry. */
        boolean hasOverride;
        /** Level and source shown in the title (an override of the config file wins over the scan file, which can be stale). */
        int shownLevel;
        String shownSource;
        int level;
        boolean customStats;
        double health;
        double damage;
        double armor;
        double speed;
        /** Elite spawn here: follow the global switch, or forced on / off (saved in the scan_ region of the entry). */
        EliteMode elite = EliteMode.FOLLOW;
        EliteMode initialElite = EliteMode.FOLLOW;
        boolean hadRegion;
        String regionId;
        boolean reset;
    }

    /** @return the runnable to call from the saving runnable of the screen (writes the file). */
    public static Runnable add(ConfigEntryBuilder entries, ConfigCategory tab) {
        Minecraft mc = Minecraft.getInstance();
        boolean editable = mc.level == null || mc.getSingleplayerServer() != null;
        DifficultyConfig cfg = DifficultyConfig.getInstance();
        if (cfg == null) {
            return () -> {};
        }
        if (!editable) {
            tab.addEntry(entries.startTextDescription(
                    t("region_editor.remote").copy().withStyle(ChatFormatting.RED)).build());
            return () -> {};
        }

        File scanFile = new File(cfg.getConfigDirectory(), DangerScanner.FILE_NAME);
        JsonObject scan = readJson(scanFile);
        tab.addEntry(entries.startTextDescription(t("scan.note")).build());
        if (scan == null) {
            tab.addEntry(entries.startTextDescription(t("scan.missing").copy().withStyle(ChatFormatting.RED)).build());
            return () -> {};
        }

        // regions made by this tab earlier: their stats fill the fields
        JsonObject root = readJson(cfg.getConfigFile());
        Map<String, JsonObject> oldRegions = new LinkedHashMap<>();
        if (root != null && root.has("difficulty_regions") && root.get("difficulty_regions").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("difficulty_regions")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("id")) {
                    oldRegions.put(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject());
                }
            }
        }

        // The scan file is only rewritten when a world runs, so after a save made outside a world (or just before the
        // server reloads) it still holds the old level. The config file is the truth: read the overrides from it.
        Map<String, Integer> structureOverrides = readLevels(root, "structure_levels");
        Map<String, Integer> dimensionOverrides = readLevels(root, "dimension_levels");

        ScalingSettings sc = cfg.getScalingSettings();
        List<Row> rows = new ArrayList<>();
        List<Row> dimensions = new ArrayList<>();
        List<Row> structures = new ArrayList<>();
        List<Row> unknown = new ArrayList<>();
        if (scan.has("dimensions") && scan.get("dimensions").isJsonObject()) {
            for (var e : scan.getAsJsonObject("dimensions").entrySet()) {
                Row r = row(true, e.getKey(), e.getValue(), sc, oldRegions, dimensionOverrides);
                if (r != null) dimensions.add(r);
            }
        }
        if (scan.has("structures") && scan.get("structures").isJsonObject()) {
            for (var e : scan.getAsJsonObject("structures").entrySet()) {
                Row r = row(false, e.getKey(), e.getValue(), sc, oldRegions, structureOverrides);
                if (r == null) continue;
                if ("unknown".equals(r.source) || "unknown_level".equals(r.source)) {
                    unknown.add(r);
                } else if (r.scannedLevel > 0 || r.hadRegion) {
                    structures.add(r);
                }
            }
        }
        rows.addAll(dimensions);
        rows.addAll(structures);
        rows.addAll(unknown);

        addGroup(entries, tab, "scan.dimensions", dimensions, true, sc);
        addGroup(entries, tab, "scan.structures", structures, false, sc);
        addGroup(entries, tab, "scan.unknown", unknown, false, sc);

        return () -> save(cfg, rows, scanFile);
    }

    private static Row row(boolean dimension, String id, JsonElement value, ScalingSettings sc, Map<String, JsonObject> oldRegions,
                           Map<String, Integer> overrides) {
        if (!value.isJsonObject()) return null;
        JsonObject o = value.getAsJsonObject();
        Row r = new Row();
        r.dimension = dimension;
        r.id = id;
        r.scannedLevel = o.has("level") && o.get("level").isJsonPrimitive() ? o.get("level").getAsInt() : 0;
        r.source = o.has("source") && o.get("source").isJsonPrimitive() ? o.get("source").getAsString() : "";
        r.level = r.scannedLevel;
        r.shownLevel = r.scannedLevel;
        r.shownSource = r.source;
        Integer override = overrideOf(overrides, id);
        if (override != null) {
            r.hasOverride = true;
            r.level = override;
            r.shownLevel = override;
            r.shownSource = "override";
        }
        r.regionId = ID_PREFIX + (dimension ? "dimension_" : "structure_")
                + id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        JsonObject region = oldRegions.get(r.regionId);
        JsonObject stats = null;
        if (region != null) {
            r.hadRegion = true;
            if (region.has("elite_enabled") && region.get("elite_enabled").isJsonPrimitive()
                    && region.get("elite_enabled").getAsJsonPrimitive().isBoolean()) {
                r.elite = EliteMode.of(region.get("elite_enabled").getAsBoolean());
                r.initialElite = r.elite;
            }
            if (region.has("difficulty_level") && region.get("difficulty_level").isJsonPrimitive()) {
                r.level = region.get("difficulty_level").getAsInt();
            }
            if (region.has("entity_whitelist") && region.get("entity_whitelist").isJsonArray()) {
                for (JsonElement e : region.getAsJsonArray("entity_whitelist")) {
                    if (e.isJsonObject()) {
                        stats = e.getAsJsonObject();
                        break;
                    }
                }
            }
        }
        r.initialLevel = r.level;
        int lvl = Math.max(1, r.level);
        r.health = stats != null && stats.has("health_bonus_percent") ? stats.get("health_bonus_percent").getAsDouble()
                : round1(sc.healthAmount(lvl) * 100.0);
        r.damage = stats != null && stats.has("damage_bonus_percent") ? stats.get("damage_bonus_percent").getAsDouble()
                : round1(sc.damageAmount(lvl) * 100.0);
        r.armor = stats != null && stats.has("armor_bonus_percent") ? stats.get("armor_bonus_percent").getAsDouble()
                : round1(sc.armorMultiplyAmount(lvl) * 100.0);
        r.speed = stats != null && stats.has("speed_bonus_percent") ? stats.get("speed_bonus_percent").getAsDouble()
                : round1(sc.speedAmount(lvl) * 100.0);
        r.customStats = stats != null;
        return r;
    }

    private static void addGroup(ConfigEntryBuilder e, ConfigCategory tab, String key, List<Row> rows,
                                 boolean expanded, ScalingSettings sc) {
        if (rows.isEmpty()) return;
        SubCategoryBuilder group = e.startSubCategory(Component.translatable(K + key, rows.size())).setExpanded(expanded);
        for (Row r : rows) {
            String title = r.id + "  [" + (r.shownLevel > 0 ? "Lv " + DangerAnalyzer.roman(r.shownLevel) : "-")
                    + (r.shownSource.isEmpty() ? "" : ", " + r.shownSource) + "]";
            SubCategoryBuilder sub = e.startSubCategory(Component.literal(title)).setExpanded(false);
            sub.add(e.startBooleanToggle(t("scan.reset"), false)
                    .setDefaultValue(false)
                    .setTooltip(TooltipWrap.wrap(t("scan.reset.tooltip")))
                    .setSaveConsumer(v -> r.reset = v)
                    .build());
            sub.add(e.startIntField(t("scan.level"), r.level)
                    .setDefaultValue(r.scannedLevel)
                    .setMin(0)
                    .setMax(ScalingSettings.ABSOLUTE_MAX_LEVEL)
                    .setTooltip(TooltipWrap.wrap(t(r.dimension ? "scan.level.tooltip.dimension" : "scan.level.tooltip.structure")))
                    .setSaveConsumer(v -> r.level = v)
                    .build());
            sub.add(e.startEnumSelector(t("elite.region"), EliteMode.class, r.elite)
                    .setDefaultValue(EliteMode.FOLLOW)
                    .setEnumNameProvider(v -> t("elite.mode." + ((EliteMode) v).name().toLowerCase(Locale.ROOT)))
                    .setTooltip(TooltipWrap.wrap(t("elite.scan.tooltip")))
                    .setSaveConsumer(v -> r.elite = v)
                    .build());
            sub.add(e.startBooleanToggle(t("scan.custom_stats"), r.customStats)
                    .setDefaultValue(false)
                    .setTooltip(TooltipWrap.wrap(t("scan.custom_stats.tooltip")))
                    .setSaveConsumer(v -> r.customStats = v)
                    .build());
            sub.add(stat(e, "scan.health", r.health, v -> r.health = v));
            sub.add(stat(e, "scan.damage", r.damage, v -> r.damage = v));
            sub.add(stat(e, "scan.armor", r.armor, v -> r.armor = v));
            sub.add(stat(e, "scan.speed", r.speed, v -> r.speed = v));
            group.add(sub.build());
        }
        tab.addEntry(group.build());
    }

    private static AbstractConfigListEntry<?> stat(ConfigEntryBuilder e, String key, double value,
                                                   java.util.function.DoubleConsumer setter) {
        return e.startDoubleField(t(key), value)
                .setDefaultValue(value)
                .setMin(-90)
                .setMax(100000)
                .setTooltip(TooltipWrap.wrap(t("scan.stat.tooltip")))
                .setSaveConsumer(setter::accept)
                .build();
    }

    private static void save(DifficultyConfig cfg, List<Row> rows, File scanFile) {
        Map<String, Integer> structureLevels = new LinkedHashMap<>();
        Map<String, Integer> dimensionLevels = new LinkedHashMap<>();
        List<JsonObject> upsert = new ArrayList<>();
        Set<String> remove = new HashSet<>();
        Set<String> resetStructures = new HashSet<>();
        Set<String> resetDimensions = new HashSet<>();
        for (Row r : rows) {
            if (r.reset) {
                // back to automatic: drop the level override and the scan_* region of this entry
                (r.dimension ? resetDimensions : resetStructures).add(r.id);
                if (r.hadRegion) remove.add(r.regionId);
                continue;
            }
            boolean withStats = r.customStats && r.level >= 1;
            boolean withElite = r.elite != EliteMode.FOLLOW && r.level >= 1;
            if (withStats || withElite) {
                upsert.add(region(r, withStats));
            } else if (r.hadRegion) {
                remove.add(r.regionId);
            }
            // written when the user changed it, or when it differs from the scan and has no override yet
            if (r.level != r.initialLevel || (r.level != r.scannedLevel && !r.hasOverride)) {
                (r.dimension ? dimensionLevels : structureLevels).put(r.id, r.level);
            }
        }
        if (structureLevels.isEmpty() && dimensionLevels.isEmpty() && upsert.isEmpty() && remove.isEmpty()
                && resetStructures.isEmpty() && resetDimensions.isEmpty()) {
            return;
        }
        boolean ok = cfg.writeScanOverrides(structureLevels, dimensionLevels, upsert, remove,
                resetStructures, resetDimensions);
        if (ok) {
            patchScanFile(scanFile, structureLevels, dimensionLevels, resetStructures, resetDimensions);
        }
        // no world running: nothing else would reload it
        if (ok && Minecraft.getInstance().getSingleplayerServer() == null) {
            cfg.loadConfig();
        }
    }

    /**
     * Keeps the scan file in line with what was just saved (the real rescan only happens while a world runs):
     * a changed level shows as an override, a reset entry is marked until the next scan recalculates it.
     */
    private static void patchScanFile(File scanFile, Map<String, Integer> structureLevels, Map<String, Integer> dimensionLevels,
                                      Set<String> resetStructures, Set<String> resetDimensions) {
        JsonObject scan = readJson(scanFile);
        if (scan == null) return;
        boolean changed = patchSection(scan, "structures", structureLevels, resetStructures);
        changed |= patchSection(scan, "dimensions", dimensionLevels, resetDimensions);
        if (!changed) return;
        try (Writer w = Files.newBufferedWriter(scanFile.toPath(), StandardCharsets.UTF_8)) {
            PRETTY.toJson(scan, w);
        } catch (Exception ignored) {
            // only a cache of the last scan: the config file already has the change
        }
    }

    private static boolean patchSection(JsonObject scan, String key, Map<String, Integer> levels, Set<String> reset) {
        if (!scan.has(key) || !scan.get(key).isJsonObject()) return false;
        JsonObject section = scan.getAsJsonObject(key);
        boolean changed = false;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            if (!section.has(e.getKey()) || !section.get(e.getKey()).isJsonObject()) continue;
            JsonObject o = section.getAsJsonObject(e.getKey());
            o.addProperty("level", e.getValue());
            o.addProperty("source", "override");
            changed = true;
        }
        for (String id : reset) {
            if (!section.has(id) || !section.get(id).isJsonObject()) continue;
            section.getAsJsonObject(id).addProperty("source", "pending_rescan");
            changed = true;
        }
        return changed;
    }

    private static Map<String, Integer> readLevels(JsonObject root, String key) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (root == null || !root.has("smart_auto_settings") || !root.get("smart_auto_settings").isJsonObject()) return out;
        JsonObject smart = root.getAsJsonObject("smart_auto_settings");
        if (!smart.has(key) || !smart.get(key).isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> e : smart.getAsJsonObject(key).entrySet()) {
            try {
                out.put(e.getKey().trim(), Math.max(0, Math.min(10, e.getValue().getAsInt())));
            } catch (Exception ignored) {
                // same as the game: a bad value is ignored
            }
        }
        return out;
    }

    /** First override that matches the id (same order as the game: file order, exact ids and patterns). */
    private static Integer overrideOf(Map<String, Integer> levels, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            if (PatternMatcher.matches(List.of(e.getKey()), rl, null)) return e.getValue();
        }
        return null;
    }

    private static JsonObject region(Row r, boolean withStats) {
        JsonObject o = new JsonObject();
        o.addProperty("id", r.regionId);
        o.addProperty("region_name", DangerAnalyzer.prettify(r.id.contains(":") ? r.id.substring(r.id.indexOf(':') + 1) : r.id));
        o.addProperty("difficulty_level", r.level);
        JsonArray target = new JsonArray();
        target.add(r.id);
        o.add("dimensions", r.dimension ? target : new JsonArray());
        o.add("biomes", new JsonArray());
        o.add("biome_tags", new JsonArray());
        o.add("structures", r.dimension ? new JsonArray() : target);
        if (r.elite != EliteMode.FOLLOW) {
            o.addProperty("elite_enabled", r.elite == EliteMode.ON);
        }
        if (!withStats) {
            // only the elite switch: no mob whitelist, the mobs follow the formula of the level
            return o;
        }
        JsonObject stats = new JsonObject();
        stats.addProperty("entity", "*");
        stats.addProperty("health_bonus_percent", r.health);
        stats.addProperty("damage_bonus_percent", r.damage);
        stats.addProperty("armor_bonus_percent", r.armor);
        stats.addProperty("speed_bonus_percent", r.speed);
        JsonArray whitelist = new JsonArray();
        whitelist.add(stats);
        o.add("entity_whitelist", whitelist);
        return o;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static JsonObject readJson(File file) {
        if (file == null || !file.exists()) return null;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement e = GSON.fromJson(reader, JsonElement.class);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
