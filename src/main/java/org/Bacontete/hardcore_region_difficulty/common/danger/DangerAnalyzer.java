package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.SmartAutoSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PatternMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.StructureLookup;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DangerAnalyzer {

    public static class DangerResult {
        public final int difficultyLevel;
        public final String regionName;
        public final String difficultyTitle;
        public final String structureName;
        public final boolean isDangerous;

        public DangerResult(int difficultyLevel, String regionName, String difficultyTitle,
                            String structureName, boolean isDangerous) {
            this.difficultyLevel = difficultyLevel;
            this.regionName = regionName;
            this.difficultyTitle = difficultyTitle;
            this.structureName = structureName;
            this.isDangerous = isDangerous;
        }

        public static DangerResult safe() {
            return new DangerResult(0, "", "", "", false);
        }
    }

    private static SmartAutoSettings settings() {
        DifficultyConfig config = DifficultyConfig.getInstance();
        return config == null ? SmartAutoSettings.DEFAULT : config.getSmartAutoSettings();
    }

    public static DangerResult analyze(List<StructureLookup.Hit> hits, Holder<Biome> biome) {
        return analyze(hits, biome, 0);
    }

    public static DangerResult analyze(List<StructureLookup.Hit> hits, Holder<Biome> biome, int spawnerCount) {
        return analyze(hits, biome, spawnerCount, DangerResult.safe());
    }

    public static DangerResult analyze(List<StructureLookup.Hit> hits, Holder<Biome> biome, int spawnerCount,
                                       DangerResult dimension) {
        return analyze(hits, biome, spawnerCount, dimension, null, 0);
    }

    public static DangerResult analyze(List<StructureLookup.Hit> hits, Holder<Biome> biome, int spawnerCount,
                                       DangerResult dimension, Supplier<List<StructureLookup.Hit>> spawnerStructureHits,
                                       int enemyCount) {
        DangerResult best = analyzeStructure(hits);
        DangerResult spawners = analyzeSpawners(spawnerCount);
        if (spawners.isDangerous && spawners.difficultyLevel > best.difficultyLevel) {

            best = withStructureName(spawners, hits);
            if (best == spawners && spawnerStructureHits != null) {
                best = withStructureName(spawners, spawnerStructureHits.get());
            }
        }
        DangerResult enemies = analyzeEnemies(enemyCount);
        if (enemies.isDangerous && enemies.difficultyLevel > best.difficultyLevel) best = enemies;
        DangerResult biomeResult = biome == null ? DangerResult.safe() : analyzeBiome(biome);
        if (biomeResult.isDangerous && biomeResult.difficultyLevel > best.difficultyLevel) best = biomeResult;
        if (dimension != null && dimension.isDangerous && dimension.difficultyLevel > best.difficultyLevel) best = dimension;
        return best;
    }

    public static DangerResult analyzeDimension(Level level) {
        if (level == null) return DangerResult.safe();
        SmartAutoSettings settings = settings();
        if (!settings.analyzeDimensions()) return DangerResult.safe();

        ResourceLocation dimensionId = level.dimension().location();
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null && LocationMatcher.manualDimensionRegion(config, dimensionId) != null) {
            return DangerResult.safe();
        }

        DimensionDangerEvaluator.Rating rating = DangerCache.getDimension(dimensionId);
        if (rating == null) {

            LevelStem stem = level.registryAccess().registry(Registries.LEVEL_STEM)
                    .map(registry -> registry.get(ResourceKey.create(Registries.LEVEL_STEM, dimensionId)))
                    .orElse(null);
            rating = DimensionDangerEvaluator.rate(dimensionId, stem, settings);

            if (stem != null) DangerCache.putDimension(dimensionId, rating);
        }
        if (!rating.isDangerous()) return DangerResult.safe();
        String name = prettify(dimensionId.getPath());
        return new DangerResult(rating.level(), name, "Dimension " + roman(rating.level()), "", true);
    }

    public static DangerResult analyzeSpawners(int spawnerCount) {
        SmartAutoSettings settings = settings();
        if (!settings.analyzeSpawners() || spawnerCount < 1) return DangerResult.safe();
        int level = settings.spawnerLevelFor(spawnerCount);
        if (level <= 0) return DangerResult.safe();

        return new DangerResult(level, "", "", "", true);
    }

    public static DangerResult analyzeEnemies(int enemyCount) {
        SmartAutoSettings settings = settings();
        if (!settings.analyzeEnemies() || enemyCount < 1) return DangerResult.safe();
        int level = settings.enemyLevelFor(enemyCount);
        if (level <= 0) return DangerResult.safe();

        return new DangerResult(level, "", "", "", true);
    }

    private static DangerResult withStructureName(DangerResult spawners, List<StructureLookup.Hit> hits) {
        if (hits == null || hits.isEmpty() || !settings().analyzeStructures()) return spawners;
        SmartAutoSettings settings = settings();
        StructureLookup.Hit named = null;
        int namedLevel = -1;
        for (StructureLookup.Hit hit : hits) {
            StructureClassifier.Rating rating = rate(hit, settings);
            if (rating.type() == StructureType.SAFE) continue;

            int rank = rating.isDangerous() ? rating.level() : 0;
            if (named == null || rank > namedLevel) {
                named = hit;
                namedLevel = rank;
            }
        }
        if (named == null) return spawners;
        String name = prettify(named.id().getPath());
        return new DangerResult(spawners.difficultyLevel, name, "Dungeon " + roman(spawners.difficultyLevel), name, true);
    }

    public static List<StructureLookup.Hit> structureHitsAtNearestSpawner(Level level, BlockPos pos) {
        SmartAutoSettings settings = settings();
        if (!settings.analyzeSpawners() || !settings.analyzeStructures()) return List.of();
        BlockPos spawner = SpawnerScanner.nearest(level, pos, settings.spawnerScanRadius());
        return spawner == null ? List.of() : StructureLookup.at(level, spawner);
    }

    public static int countSpawners(Level level, BlockPos pos) {
        SmartAutoSettings settings = settings();
        if (!settings.analyzeSpawners()) return 0;
        return SpawnerScanner.count(level, pos, settings.spawnerScanRadius());
    }

    public static int countEnemies(Level level, BlockPos pos) {
        SmartAutoSettings settings = settings();
        if (!settings.analyzeEnemies()) return 0;
        return EnemyScanner.count(level, pos, settings.enemyScanRadius());
    }

    public record SpawnerReport(boolean enabled, int radius, int count, int spawnerLevel,
                                boolean decisive, DangerResult overall,
                                boolean enemiesEnabled, int enemyRadius, int enemyCount, int enemyLevel,
                                boolean enemyDecisive) {}

    public static SpawnerReport reportAt(Level level, BlockPos pos) {
        SmartAutoSettings settings = settings();
        int spawnerCount = countSpawners(level, pos);
        DangerResult spawners = analyzeSpawners(spawnerCount);
        int enemyCount = countEnemies(level, pos);
        DangerResult enemies = analyzeEnemies(enemyCount);
        List<StructureLookup.Hit> hits = settings.analyzeStructures() ? StructureLookup.at(level, pos) : List.of();
        DangerResult overall = analyze(hits, level.getBiome(pos), spawnerCount, analyzeDimension(level),
                () -> structureHitsAtNearestSpawner(level, pos), enemyCount);

        DangerResult structure = settings.analyzeStructures() ? analyzeStructure(hits) : DangerResult.safe();
        DangerResult biomeOnly = analyzeBiome(level.getBiome(pos));
        DangerResult dimensionOnly = analyzeDimension(level);
        boolean decisive = spawners.isDangerous && overall.isDangerous
                && overall.difficultyLevel == spawners.difficultyLevel
                && spawners.difficultyLevel > structure.difficultyLevel
                && spawners.difficultyLevel > biomeOnly.difficultyLevel
                && spawners.difficultyLevel > dimensionOnly.difficultyLevel
                && spawners.difficultyLevel > enemies.difficultyLevel;
        boolean enemyDecisive = enemies.isDangerous && overall.isDangerous
                && overall.difficultyLevel == enemies.difficultyLevel
                && enemies.difficultyLevel > structure.difficultyLevel
                && enemies.difficultyLevel > biomeOnly.difficultyLevel
                && enemies.difficultyLevel > dimensionOnly.difficultyLevel
                && enemies.difficultyLevel > spawners.difficultyLevel;
        return new SpawnerReport(settings.analyzeSpawners(), settings.spawnerScanRadius(), spawnerCount,
                spawners.difficultyLevel, decisive, overall,
                settings.analyzeEnemies(), settings.enemyScanRadius(), enemyCount, enemies.difficultyLevel,
                enemyDecisive);
    }

    public static DangerResult analyzeStructure(List<StructureLookup.Hit> hits) {
        SmartAutoSettings settings = settings();
        DangerResult best = DangerResult.safe();
        if (!settings.analyzeStructures()) return best;

        for (StructureLookup.Hit hit : hits) {
            StructureClassifier.Rating rating = rate(hit, settings);
            if (!rating.isDangerous() || rating.level() <= best.difficultyLevel) continue;
            String name = prettify(hit.id().getPath());
            String label = rating.type() == StructureType.BOSS ? "Boss " : "Dungeon ";
            best = new DangerResult(rating.level(), name, label + roman(rating.level()), name, true);
        }
        return best;
    }

    /** True if a structure at this spot has a level you set yourself (structure_levels) equal to the given one. */
    public static boolean hasOverrideLevel(List<StructureLookup.Hit> hits, int level) {
        if (hits == null) return false;
        SmartAutoSettings settings = settings();
        for (StructureLookup.Hit hit : hits) {
            Predicate<ResourceLocation> tagTest = tag -> hit.holder().is(TagKey.<Structure>create(Registries.STRUCTURE, tag));
            Integer override = firstMatch(settings.structureLevels(), hit.id(), tagTest);
            if (override != null && override == level) return true;
        }
        return false;
    }

    /** Same result with another level; the number at the end of the title is replaced. */
    public static DangerResult withLevel(DangerResult r, int level) {
        if (r.difficultyLevel == level) return r;
        String title = r.difficultyTitle;
        int space = title == null ? -1 : title.lastIndexOf(' ');
        if (space >= 0) title = title.substring(0, space + 1) + roman(level);
        return new DangerResult(level, r.regionName, title, r.structureName, r.isDangerous);
    }

    public record Resolved(StructureClassifier.Rating rating, String source) {}

    private static StructureClassifier.Rating rate(StructureLookup.Hit hit, SmartAutoSettings settings) {
        StructureClassifier.Rating cached = DangerCache.getStructure(hit.id());
        if (cached != null) return cached;
        StructureClassifier.Rating rating = resolve(hit.id(), hit.holder(), settings).rating();
        DangerCache.putStructure(hit.id(), rating);
        return rating;
    }

    public static Resolved resolve(ResourceLocation id, Holder<Structure> holder, SmartAutoSettings settings) {
        Predicate<ResourceLocation> tagTest = tag -> holder.is(TagKey.<Structure>create(Registries.STRUCTURE, tag));

        Integer override = firstMatch(settings.structureLevels(), id, tagTest);
        if (override != null) {
            return new Resolved(StructureClassifier.Rating.ofLevel(override), "override");
        }

        StructureClassifier.Rating tagged = rateByTag(holder);
        if (tagged != null) {
            return new Resolved(tagged, "tag");
        }

        StructureClassifier.Rating rating = StructureClassifier.rate(id);
        if (rating.type() != StructureType.UNKNOWN) {
            boolean vanilla = "minecraft".equals(id.getNamespace());
            return new Resolved(rating, vanilla ? "table" : "words");
        }

        if (tagTest.test(ResourceLocation.fromNamespaceAndPath("minecraft", "village"))) {
            return new Resolved(StructureClassifier.Rating.SAFE, "village_tag"); 
        }
        if (settings.structureSpawnLevel() > 0 && definesMonsterSpawns(holder)) {
            return new Resolved(StructureClassifier.Rating.ofLevel(settings.structureSpawnLevel()), "spawns");
        }
        if (settings.unknownStructureLevel() > 0) {
            return new Resolved(StructureClassifier.Rating.ofLevel(settings.unknownStructureLevel()), "unknown_level");
        }
        return new Resolved(StructureClassifier.Rating.UNKNOWN, "unknown");
    }

    private static StructureClassifier.Rating rateByTag(Holder<Structure> holder) {
        if (holder == null) return null;
        try {
            if (inStructureTag(holder, "ignore")) return StructureClassifier.Rating.SAFE;
            int level = 0;
            DifficultyConfig cfg = DifficultyConfig.getInstance();
            int top = Math.max(5, cfg == null ? 5 : cfg.getScalingSettings().maxRegionLevel());
            // tags level_6, level_7... work for the levels the player adds
            for (int l = top; l >= 1; l--) {
                if (inStructureTag(holder, "level_" + l)) {
                    level = l;
                    break;
                }
            }
            boolean boss = inStructureTag(holder, "bosses");
            if (level == 0 && !boss) return null;
            if (level == 0) level = 5;
            return new StructureClassifier.Rating(boss || level >= 5 ? StructureType.BOSS : StructureType.DUNGEON, level);
        } catch (Exception e) {

            return null;
        }
    }

    private static boolean inStructureTag(Holder<Structure> holder, String path) {
        return holder.is(TagKey.<Structure>create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(HardcoreRegionDifficulty.MODID, path)));
    }

    private static boolean definesMonsterSpawns(Holder<Structure> holder) {
        try {
            return holder.value().spawnOverrides().containsKey(MobCategory.MONSTER);
        } catch (Exception e) {
            return false;
        }
    }

    public static DangerResult analyzeBiome(Holder<Biome> biome) {
        if (!settings().analyzeBiomes()) return DangerResult.safe();

        ResourceLocation biomeId = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (biomeId != null) {
            DangerResult cached = DangerCache.getBiome(biomeId);
            if (cached != null) return cached;
        }

        int level = BiomeDangerEvaluator.getDangerLevel(biome);
        DangerResult result;
        if (level <= 0) {
            result = DangerResult.safe();
        } else {
            String biomeName = biomeId != null ? prettify(biomeId.getPath()) : "Unknown Biome";
            result = new DangerResult(level, biomeName, "Adventure " + roman(level), "", true);
        }

        if (biomeId != null) DangerCache.putBiome(biomeId, result);
        return result;
    }

    static Integer firstMatch(Map<String, Integer> levels, ResourceLocation id, Predicate<ResourceLocation> tagTest) {
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            if (PatternMatcher.matches(List.of(e.getKey()), id, tagTest)) return e.getValue();
        }
        return null;
    }

    public static String prettify(String path) {
        StringBuilder sb = new StringBuilder();
        for (String word : path.replace('/', ' ').replace('_', ' ').split(" ")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return sb.toString();
    }

    public static String roman(int level) {
        return switch (level) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(level); };
    }
}
