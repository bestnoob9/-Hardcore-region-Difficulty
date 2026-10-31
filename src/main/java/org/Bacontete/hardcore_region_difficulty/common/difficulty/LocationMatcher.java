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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocationMatcher {

    public record Match(DifficultyRegion region, String structureName, boolean dimensionWide) {
        public Match(DifficultyRegion region, String structureName) {
            this(region, structureName, false);
        }
    }

    /**
     * Smart Auto used to analyse the surroundings (spawners, enemies, structures, dimension danger) every time the player
     * or a mob was checked. The regions are now saved in the config before the world is entered, so this runtime scan is
     * switched off: only the saved regions are matched. It is switched on again (true) because the scans are now cheap:
     * the structure / spawner lookups are cached and mobs / loot / trades reuse a short cache (see {@link #matchCheap}).
     * The hostile-mob count was removed, Smart Auto no longer looks at the mobs around.
     */
    private static final boolean RUNTIME_SMART_AUTO = true;

    private static boolean smartAuto(DifficultyConfig config) {
        return RUNTIME_SMART_AUTO && config.isSmartAutoEnabled();
    }

    private static final int TIER_DIMENSION = 1;
    private static final int TIER_BIOME_TAG = 2;
    private static final int TIER_BIOME = 3;
    private static final int TIER_STRUCTURE = 4;

    /** Full check (used for players, chests, commands). */
    public static Match match(Level level, BlockPos pos) {
        return match(level, pos, true);
    }

    /**
     * Cheap check for mobs, loot and trades (called very often, e.g. for every mob that spawns): the result is cached
     * for one second per 8x8x8 block cell. Same result as {@link #match} everywhere else.
     */
    public static Match matchCheap(Level level, BlockPos pos) {
        return match(level, pos, false);
    }

    private static Match match(Level level, BlockPos pos, boolean full) {
        if (!ForgeConfig.isEnabled()) {
            return null;
        }
        Match raw = rawMatch(level, pos, full);
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (raw == null || config == null || level == null) {
            return raw;
        }

        int max = DimensionDangerEvaluator.getMaxDifficulty(level.dimension().location(),
                dimensionBaseLevel(level, config), config.getScalingSettings());
        DifficultyRegion region = raw.region();
        if (region.getDifficultyLevel() <= max) {
            return raw;
        }

        DifficultyRegion capped = new DifficultyRegion(region.getId(), region.getRegionName(),
                "Difficulty " + DangerAnalyzer.roman(max), max,
                region.getDimensions(), region.getBiomes(), region.getBiomeTags(), region.getStructures(),
                config.levelIcon(max), config.levelColor(max), region.getMobEntries())
                .withLootOverride(region.getLootOverride())
                .withEliteEnabled(region.getEliteEnabled());
        return new Match(capped, raw.structureName(), raw.dimensionWide());
    }

    public static int dimensionBaseLevel(Level level, DifficultyConfig config) {
        ResourceLocation dimensionId = level.dimension().location();
        DifficultyRegion manual = manualDimensionRegion(config, dimensionId);
        int base = manual != null ? manual.getDifficultyLevel() : 0;
        if (base <= 0 && smartAuto(config)) {
            base = DangerAnalyzer.analyzeDimension(level).difficultyLevel;
        }
        return Math.max(1, base);
    }

    /** Result of the region match per dimension + biome, valid for one loaded region list (see rawMatch). */
    private static final Map<String, Optional<Match>> MATCH_CACHE = new ConcurrentHashMap<>();
    private static volatile List<DifficultyRegion> matchCacheFor;

    /**
     * Without structure regions (and without the runtime Smart Auto) the result only depends on the dimension and the
     * biome, so it is computed once per biome and reused for every player check and every mob. The cache is dropped
     * when the config is reloaded (a new region list).
     */
    private static Match rawMatch(Level level, BlockPos pos, boolean full) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null && level != null && smartAuto(config)) {
            return full ? computeMatch(level, pos) : cheapSmartMatch(level, pos);
        }
        if (config == null || level == null || config.hasStructureRegions()) {
            return computeMatch(level, pos);
        }
        ResourceLocation biomeId = level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId == null) {
            return computeMatch(level, pos);
        }
        List<DifficultyRegion> regions = config.getRegionList();
        if (matchCacheFor != regions) {
            MATCH_CACHE.clear();
            matchCacheFor = regions;
        }
        String key = level.dimension().location() + "|" + biomeId;
        Optional<Match> cached = MATCH_CACHE.get(key);
        if (cached == null) {
            cached = Optional.ofNullable(computeMatch(level, pos));
            if (MATCH_CACHE.size() > 512) {
                MATCH_CACHE.clear();
            }
            MATCH_CACHE.put(key, cached);
        }
        return cached.orElse(null);
    }

    private record SmartKey(ResourceLocation dimension, int x, int y, int z) {}

    private record SmartEntry(Optional<Match> match, long time) {}

    /** Short cache of the smart-auto result for the cheap check (mobs, loot, trades). */
    private static final Map<SmartKey, SmartEntry> SMART_CACHE = new ConcurrentHashMap<>();
    private static final long SMART_TTL_TICKS = 20L;
    private static final int SMART_CACHE_MAX = 4096;
    private static volatile List<DifficultyRegion> smartCacheFor;

    private static Match cheapSmartMatch(Level level, BlockPos pos) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        List<DifficultyRegion> regions = config.getRegionList();
        if (smartCacheFor != regions) {
            SMART_CACHE.clear();
            smartCacheFor = regions;
        }
        long now = level.getGameTime();
        SmartKey key = new SmartKey(level.dimension().location(), pos.getX() >> 3, pos.getY() >> 3, pos.getZ() >> 3);
        SmartEntry entry = SMART_CACHE.get(key);
        if (entry != null && now >= entry.time() && now - entry.time() < SMART_TTL_TICKS) {
            return entry.match().orElse(null);
        }
        Match m = computeMatch(level, pos);
        if (SMART_CACHE.size() > SMART_CACHE_MAX) {
            SMART_CACHE.clear();
        }
        SMART_CACHE.put(key, new SmartEntry(Optional.ofNullable(m), now));
        return m;
    }

    /** Drops the cached results (called together with {@link org.Bacontete.hardcore_region_difficulty.common.danger.DangerCache#clear()}). */
    public static void clearCaches() {
        SMART_CACHE.clear();
        MATCH_CACHE.clear();
    }

    private static Match computeMatch(Level level, BlockPos pos) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || level == null) {
            return null;
        }

        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation biomeId = biome.unwrapKey().map(k -> k.location()).orElse(null);
        ResourceLocation dimensionId = level.dimension().location();

        // Structure starts are only looked up when a region really needs them (after its dimension / biome filters
        // matched), and only once per match - like Dungeon Difficulty resolves them lazily once per location.
        List<StructureLookup.Hit> hits = null;

        Match best = null;       
        int bestTier = 0;
        Match dimensionBest = null;
        int dimensionLevel = Integer.MIN_VALUE;

        for (DifficultyRegion region : config.getRegionList()) {
            if (!region.hasCriteria()) continue;

            int tier = 0;
            String structureName = "";
            boolean ok = true;

            if (!region.getBiomes().isEmpty()) {
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
            if (ok && !region.getStructures().isEmpty()) {
                if (hits == null) {
                    hits = StructureLookup.at(level, pos);
                }
                StructureLookup.Hit matched = matchStructure(region, hits);
                if (matched == null) {
                    ok = false;
                } else {
                    tier = TIER_STRUCTURE;
                    structureName = DangerAnalyzer.prettify(matched.id().getPath());
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

        if (smartAuto(config)) {
            if (hits == null) {
                hits = (config.hasStructureRegions() || config.getSmartAutoSettings().analyzeStructures())
                        ? StructureLookup.at(level, pos)
                        : Collections.emptyList();
            }

            int spawners = DangerAnalyzer.countSpawners(level, pos);

            DangerAnalyzer.DangerResult dimension = DangerAnalyzer.analyzeDimension(level);

            // hostile mobs around: only when the option is on, and not when a structure already gives the top level
            int enemies = 0;
            if (config.getSmartAutoSettings().analyzeEnemies()) {
                DangerAnalyzer.DangerResult structureDanger = DangerAnalyzer.analyzeStructure(hits);
                if (!structureDanger.isDangerous || structureDanger.difficultyLevel < 5) {
                    enemies = DangerAnalyzer.countEnemies(level, pos);
                }
            }

            DangerAnalyzer.DangerResult danger = DangerAnalyzer.analyze(hits, biome, spawners, dimension,
                    spawners > 0 ? () -> DangerAnalyzer.structureHitsAtNearestSpawner(level, pos) : null, enemies);
            if (danger.isDangerous && danger.difficultyLevel > 0) {
                if (danger != dimension && config.getScalingSettings().isDangerScaleEnabled()
                        && !DangerAnalyzer.hasOverrideLevel(hits, danger.difficultyLevel)) {
                    // ratings made for levels 1-5 follow the level of the dimension (see ScalingSettings#adjustForDimension)
                    danger = DangerAnalyzer.withLevel(danger, config.getScalingSettings()
                            .adjustForDimension(danger.difficultyLevel, dimensionBaseLevel(level, config)));
                }

                if (dimensionBest != null && dimensionLevel >= danger.difficultyLevel) {
                    if (danger != dimension && danger.structureName != null && !danger.structureName.isEmpty()) {
                        // a structure inside a dimension of the same or a higher level: the level stays the one of
                        // the dimension, but the player is still in a structure (its title must show)
                        DifficultyRegion d = dimensionBest.region();
                        DifficultyRegion named = new DifficultyRegion(
                                "smart_auto_" + danger.regionName.toLowerCase().replace(' ', '_'),
                                danger.regionName, d.getDifficultyTitle(), d.getDifficultyLevel(),
                                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList(), d.getIcon(), d.getTitleColor(), d.getMobEntries())
                                .withLootOverride(d.getLootOverride())
                                .withEliteEnabled(d.getEliteEnabled());
                        return new Match(named, danger.structureName, false);
                    }
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
        DifficultyConfig cfg = DifficultyConfig.getInstance();
        String icon = cfg == null ? "" : cfg.levelIcon(result.difficultyLevel);
        String color = cfg == null ? "" : cfg.levelColor(result.difficultyLevel);
        return new DifficultyRegion(
                "smart_auto_" + result.regionName.toLowerCase().replace(' ', '_'),
                result.regionName,
                result.difficultyTitle,
                result.difficultyLevel,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                icon, color);
    }

    public static int getDifficultyLevel(Level level, BlockPos pos) {
        Match m = matchCheap(level, pos);
        return m != null ? m.region().getDifficultyLevel() : 1;
    }

    public static String getRegionName(Level level, BlockPos pos) {
        Match m = matchCheap(level, pos);
        return m != null ? m.region().getRegionName() : "";
    }

    public static String getDifficultyTitle(Level level, BlockPos pos) {
        Match m = matchCheap(level, pos);
        return m != null ? m.region().getDifficultyTitle() : "";
    }

    public static String getStructureName(Level level, BlockPos pos) {
        Match m = matchCheap(level, pos);
        return m != null ? m.structureName() : "";
    }
}
