package org.Bacontete.hardcore_region_difficulty.common.difficulty;

import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyRegion;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer;
import org.Bacontete.hardcore_region_difficulty.common.danger.DimensionDangerEvaluator;

import java.util.Collections;
import java.util.List;

public class LocationMatcher {

    public record Match(DifficultyRegion region, String structureName, boolean dimensionWide) {
        public Match(DifficultyRegion region, String structureName) {
            this(region, structureName, false);
        }
    }

    public static final int MAX_REGION_LEVEL = 5;

    private static final int TIER_DIMENSION = 1;
    private static final int TIER_BIOME_TAG = 2;
    private static final int TIER_BIOME = 3;
    private static final int TIER_STRUCTURE = 4;

    public static Match match(Level level, BlockPos pos) {
        if (!ForgeConfig.isEnabled()) {
            return null;
        }
        Match raw = rawMatch(level, pos);
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (raw == null || config == null || level == null) {
            return raw;
        }

        int max = Math.min(MAX_REGION_LEVEL, DimensionDangerEvaluator.getMaxDifficulty(dimensionBaseLevel(level, config)));
        DifficultyRegion region = raw.region();
        if (region.getDifficultyLevel() <= max) {
            return raw;
        }

        DifficultyRegion capped = new DifficultyRegion(region.getId(), region.getRegionName(),
                "Difficulty " + DangerAnalyzer.roman(max), max,
                region.getDimensions(), region.getBiomes(), region.getBiomeTags(), region.getStructures());
        return new Match(capped, raw.structureName(), raw.dimensionWide());
    }

    public static int dimensionBaseLevel(Level level, DifficultyConfig config) {
        ResourceLocation dimensionId = level.dimension().location();
        DifficultyRegion manual = manualDimensionRegion(config, dimensionId);
        int base = manual != null ? manual.getDifficultyLevel() : 0;
        if (base <= 0 && config.isSmartAutoEnabled()) {
            base = DangerAnalyzer.analyzeDimension(level).difficultyLevel;
        }
        return Math.max(1, base);
    }

    private static Match rawMatch(Level level, BlockPos pos) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || level == null) {
            return null;
        }

        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation biomeId = biome.unwrapKey().map(k -> k.location()).orElse(null);
        ResourceLocation dimensionId = level.dimension().location();

        List<StructureLookup.Hit> hits = (config.hasStructureRegions()
                || (config.isSmartAutoEnabled() && config.getSmartAutoSettings().analyzeStructures()))
                ? StructureLookup.at(level, pos)
                : Collections.emptyList();

        Match best = null;       
        int bestTier = 0;
        Match dimensionBest = null;
        int dimensionLevel = Integer.MIN_VALUE;

        for (DifficultyRegion region : config.getRegionList()) {
            if (!region.hasCriteria()) continue;

            int tier = 0;
            String structureName = "";
            boolean ok = true;

            if (!region.getStructures().isEmpty()) {
                StructureLookup.Hit matched = matchStructure(region, hits);
                if (matched == null) {
                    ok = false;
                } else {
                    tier = TIER_STRUCTURE;
                    structureName = DangerAnalyzer.prettify(matched.id().getPath());
                }
            }
            if (ok && !region.getBiomes().isEmpty()) {
                if (biomeId != null && PatternMatcher.matches(region.getBiomes(), biomeId, tag -> biome.is(TagKey.create(Registries.BIOME, tag)))) {
                    tier = Math.max(tier, TIER_BIOME);
                } else {
                    ok = false;
                }
            }
            if (ok && !region.getBiomeTags().isEmpty()) {
                if (matchesBiomeTag(biome, region.getBiomeTags())) {
                    tier = Math.max(tier, TIER_BIOME_TAG);
                } else {
                    ok = false;
                }
            }
            if (ok && !region.getDimensions().isEmpty()) {
                if (PatternMatcher.matches(region.getDimensions(), dimensionId, null)) {
                    tier = Math.max(tier, TIER_DIMENSION);
                } else {
                    ok = false;
                }
            }
            if (!ok || tier == 0) continue;

            if (tier == TIER_DIMENSION) {
                if (region.getDifficultyLevel() > dimensionLevel) {
                    dimensionBest = new Match(region, "", true);
                    dimensionLevel = region.getDifficultyLevel();
                }
            } else if (best == null || tier > bestTier
                    || (tier == bestTier && region.getDifficultyLevel() > best.region().getDifficultyLevel())) {
                best = new Match(region, structureName);
                bestTier = tier;
            }
        }

        if (best != null) {
            return best;
        }

        if (config.isSmartAutoEnabled()) {

            int spawners = DangerAnalyzer.countSpawners(level, pos);

            DangerAnalyzer.DangerResult dimension = DangerAnalyzer.analyzeDimension(level);
            DangerAnalyzer.DangerResult structureDanger = DangerAnalyzer.analyzeStructure(hits);

            int enemies = 0;
            if (!structureDanger.isDangerous || structureDanger.difficultyLevel < 5) {
                enemies = DangerAnalyzer.countEnemies(level, pos);
            }

            DangerAnalyzer.DangerResult danger = DangerAnalyzer.analyze(hits, biome, spawners, dimension,
                    spawners > 0 ? () -> DangerAnalyzer.structureHitsAtNearestSpawner(level, pos) : null, enemies);
            if (danger.isDangerous && danger.difficultyLevel > 0) {

                if (dimensionBest != null && dimensionLevel >= danger.difficultyLevel) {
                    return dimensionBest;
                }

                return new Match(createSyntheticRegion(danger), danger.structureName, danger == dimension);
            }
        }

        return dimensionBest;
    }

    public static DifficultyRegion manualDimensionRegion(DifficultyConfig config, ResourceLocation dimensionId) {
        DifficultyRegion best = null;
        for (DifficultyRegion region : config.getRegionList()) {
            if (region.getDimensions().isEmpty() || !region.getStructures().isEmpty()
                    || !region.getBiomes().isEmpty() || !region.getBiomeTags().isEmpty()) {
                continue;
            }
            if (!PatternMatcher.matches(region.getDimensions(), dimensionId, null)) continue;
            if (best == null || region.getDifficultyLevel() > best.getDifficultyLevel()) best = region;
        }
        return best;
    }

    private static StructureLookup.Hit matchStructure(DifficultyRegion region, List<StructureLookup.Hit> hits) {
        for (StructureLookup.Hit hit : hits) {
            if (PatternMatcher.matches(region.getStructures(), hit.id(),
                    tag -> hit.holder().is(TagKey.<Structure>create(Registries.STRUCTURE, tag)))) {
                return hit;
            }
        }
        return null;
    }

    private static boolean matchesBiomeTag(Holder<Biome> biome, List<String> biomeTags) {
        for (String tagStr : biomeTags) {
            if (tagStr == null) continue;
            String s = tagStr.trim();
            if (s.startsWith("#")) s = s.substring(1);
            ResourceLocation tagLoc = ResourceLocation.tryParse(s);
            if (tagLoc != null && biome.is(TagKey.create(Registries.BIOME, tagLoc))) {
                return true;
            }
        }
        return false;
    }

    private static DifficultyRegion createSyntheticRegion(DangerAnalyzer.DangerResult result) {
        return new DifficultyRegion(
                "smart_auto_" + result.regionName.toLowerCase().replace(' ', '_'),
                result.regionName,
                result.difficultyTitle,
                result.difficultyLevel,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static int getDifficultyLevel(Level level, BlockPos pos) {
        Match m = match(level, pos);
        return m != null ? m.region().getDifficultyLevel() : 1;
    }

    public static String getRegionName(Level level, BlockPos pos) {
        Match m = match(level, pos);
        return m != null ? m.region().getRegionName() : "";
    }

    public static String getDifficultyTitle(Level level, BlockPos pos) {
        Match m = match(level, pos);
        return m != null ? m.region().getDifficultyTitle() : "";
    }

    public static String getStructureName(Level level, BlockPos pos) {
        Match m = match(level, pos);
        return m != null ? m.structureName() : "";
    }
}
