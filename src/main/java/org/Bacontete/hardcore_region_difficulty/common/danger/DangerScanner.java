package org.Bacontete.hardcore_region_difficulty.common.danger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyRegion;
import org.Bacontete.hardcore_region_difficulty.common.config.SmartAutoSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DangerScanner {
    private DangerScanner() {}

    public static final String FILE_NAME = "hardcore_region_difficulty_scan.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Summary(int structures, int dangerous, int ignored, int unknown, int biomes,
                          int dangerousBiomes, int dimensions, int dangerousDimensions,
                          List<Map.Entry<String, Integer>> unknownByNamespace, File file) {}

    private record Row(ResourceLocation id, int level, String source) {}

    public static Summary run(MinecraftServer server, File dir) throws IOException {
        DifficultyConfig config = DifficultyConfig.getInstance();
        SmartAutoSettings settings = config == null ? SmartAutoSettings.DEFAULT : config.getSmartAutoSettings();
        RegistryAccess access = server.registryAccess();

        Registry<Structure> structureRegistry = access.registryOrThrow(Registries.STRUCTURE);
        List<Row> rows = new ArrayList<>();
        int dangerous = 0, ignored = 0, unknown = 0;
        Map<String, Integer> unknownByNs = new TreeMap<>();
        List<String> unknownIds = new ArrayList<>();
        for (ResourceLocation id : structureRegistry.keySet()) {
            Holder<Structure> holder = structureRegistry.getHolder(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
            if (holder == null) continue;
            DangerAnalyzer.Resolved r = DangerAnalyzer.resolve(id, holder, settings);
            rows.add(new Row(id, r.rating().level(), r.source()));
            DangerCache.putStructure(id, r.rating());
            if (r.rating().isDangerous()) {
                dangerous++;
            } else if (r.rating().type() == StructureType.UNKNOWN) {
                unknown++;
                unknownIds.add(id.toString());
                unknownByNs.merge(id.getNamespace(), 1, Integer::sum);
            } else {
                ignored++;
            }
        }
        rows.sort(Comparator.comparingInt(Row::level).reversed().thenComparing(r -> r.id().toString()));
        unknownIds.sort(String::compareTo);

        JsonObject structures = new JsonObject();
        for (Row row : rows) {
            JsonObject o = new JsonObject();
            o.addProperty("level", row.level());
            o.addProperty("source", row.source());
            structures.add(row.id().toString(), o);
        }

        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        List<ResourceLocation> biomeIds = new ArrayList<>(biomeRegistry.keySet());
        biomeIds.sort(Comparator.comparingInt((ResourceLocation id) -> id.getNamespace().equals("minecraft") ? 1 : 0)
                .thenComparing(ResourceLocation::toString));
        JsonObject biomes = new JsonObject();
        int dangerousBiomes = 0;
        for (ResourceLocation id : biomeIds) {
            Holder<Biome> holder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, id)).orElse(null);
            if (holder == null) continue;
            int level = BiomeDangerEvaluator.getDangerLevel(holder);
            if (level > 0) dangerousBiomes++;
            biomes.addProperty(id.toString(), level);
        }

        Registry<LevelStem> stemRegistry = access.registryOrThrow(Registries.LEVEL_STEM);
        List<ResourceLocation> dimIds = new ArrayList<>(stemRegistry.keySet());
        dimIds.sort(Comparator.comparing(ResourceLocation::toString));
        JsonObject dimensions = new JsonObject();
        int dangerousDimensions = 0;
        for (ResourceLocation id : dimIds) {
            DimensionDangerEvaluator.Rating rating = DimensionDangerEvaluator.rate(id, stemRegistry.get(id), settings);
            DangerCache.putDimension(id, rating);
            if (rating.isDangerous()) dangerousDimensions++;
            JsonObject o = new JsonObject();
            o.addProperty("level", rating.level());
            o.addProperty("source", rating.source());
            DifficultyRegion manual = config == null ? null : LocationMatcher.manualDimensionRegion(config, id);
            if (manual != null) {

                o.addProperty("manual_region", manual.getId());
                o.addProperty("manual_level", manual.getDifficultyLevel());
            }
            dimensions.add(id.toString(), o);
        }

        JsonObject summary = new JsonObject();
        summary.addProperty("structures", rows.size());
        summary.addProperty("dangerous", dangerous);
        summary.addProperty("ignored_safe", ignored);
        summary.addProperty("unknown", unknown);
        summary.addProperty("biomes", biomeIds.size());
        summary.addProperty("dangerous_biomes", dangerousBiomes);
        summary.addProperty("dimensions", dimIds.size());
        summary.addProperty("dangerous_dimensions", dangerousDimensions);

        JsonObject paste = new JsonObject();
        JsonObject pasteStructures = new JsonObject();
        unknownIds.forEach(id -> pasteStructures.addProperty(id, 0));
        paste.add("structure_levels", pasteStructures);

        JsonObject root = new JsonObject();
        root.addProperty("_help", "Generated automatically at server start (or by /hrd scan). 'source': override = your config, tag = datapack tag hardcore_region_difficulty:level_1..5 / bosses / ignore, table = vanilla table, "
                + "words = guessed from the id, spawns = structure defines monster spawns, unknown_level = unknown_structure_level, "
                + "village_tag = tagged minecraft:village, unknown = not recognised (ignored). To rate a structure copy "
                + "its id into smart_auto_settings.structure_levels (0 = ignore, 1-5 = level), or add it to one of the datapack tags (no config edit needed). Dimensions are rated too: "
                + "source override = smart_auto_settings.dimension_levels, table = vanilla, words = guessed from the id, "
                + "biomes = average danger of the biomes it generates, type = dimension type flags (ultra-warm / no sky light), "
                + "unknown_level = unknown_dimension_level. The level is a minimum for the whole dimension. To change one, "
                + "put its id into smart_auto_settings.dimension_levels (0 = no automatic level); a region with only "
                + "\"dimensions\": [id] (manual_region) always wins.");
        root.add("summary", summary);

        JsonObject spawnerInfo = new JsonObject();
        spawnerInfo.addProperty("analyze_spawners", settings.analyzeSpawners());
        spawnerInfo.addProperty("spawner_scan_radius", settings.spawnerScanRadius());
        JsonObject thresholds = new JsonObject();
        settings.spawnerLevelThresholds().forEach((lvl, min) -> thresholds.addProperty(String.valueOf(lvl), min));
        spawnerInfo.add("spawner_level_thresholds", thresholds);
        spawnerInfo.addProperty("_help", "Monster spawner blocks within the radius raise the level (key = level, value = minimum spawner count). "
                + "Spawners are counted around a position, so the numbers for your location are shown by /hrd scan in chat.");
        root.add("spawner_analysis", spawnerInfo);
        root.add("paste_into_smart_auto_settings_for_unknown_structures", paste);
        root.add("structures", structures);
        root.add("biomes", biomes);
        root.add("dimensions", dimensions);

        Files.createDirectories(dir.toPath());
        File file = new File(dir, FILE_NAME);
        try (Writer w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }

        List<Map.Entry<String, Integer>> byNs = new ArrayList<>(unknownByNs.entrySet());
        byNs.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        return new Summary(rows.size(), dangerous, ignored, unknown, biomeIds.size(), dangerousBiomes,
                dimIds.size(), dangerousDimensions, byNs, file);
    }
}
