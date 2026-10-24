package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.Bacontete.hardcore_region_difficulty.common.config.SmartAutoSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class DimensionDangerEvaluator {
    private DimensionDangerEvaluator() {}

    public record Rating(int level, String source) {
        public static final Rating NONE = new Rating(0, "none");

        public boolean isDangerous() {
            return level > 0;
        }
    }

    private static final Map<String, Integer> VANILLA = new HashMap<>();
    static {
        VANILLA.put("minecraft:overworld", 0); 
        VANILLA.put("minecraft:the_nether", 2);
        VANILLA.put("minecraft:the_end", 3);
    }

    private static final Map<String, Integer> WORDS = new HashMap<>();
    static {
        for (String w : new String[]{"abyss", "hell", "inferno", "underworld", "doom", "apocalypse"}) WORDS.put(w, 5);
        for (String w : new String[]{"nether", "end", "void", "shadow", "chaos", "wasteland", "blight"}) WORDS.put(w, 4);
        for (String w : new String[]{"dark", "darkness", "cave", "cavern", "ruins", "twilight"}) WORDS.put(w, 3);
    }

    private static final int LEVEL_ULTRAWARM = 4;
    private static final int LEVEL_NO_SKYLIGHT = 3;

    static int classifyId(String namespace, String path) {
        if (path == null || path.isEmpty()) return -1;
        if ("minecraft".equals(namespace)) {
            Integer exact = VANILLA.get(namespace + ":" + path);
            if (exact != null) return exact;
        }
        int best = 0;
        for (String w : StructureClassifier.words(path)) {
            Integer level = WORDS.get(w);
            if (level != null && level > best) best = level;
        }
        return best > 0 ? best : -1;
    }

    public static Rating rate(ResourceLocation id, LevelStem stem, SmartAutoSettings settings) {
        Integer override = DangerAnalyzer.firstMatch(settings.dimensionLevels(), id, null);
        if (override != null) {
            return new Rating(Math.max(0, override), "override");
        }
        if (!settings.analyzeDimensions()) {
            return Rating.NONE;
        }

        if ("minecraft".equals(id.getNamespace())) {
            Integer exact = VANILLA.get(id.toString());
            if (exact != null) return new Rating(exact, "table");
        }

        int level = 0;
        String source = "none";

        int words = classifyId(id.getNamespace(), id.getPath());
        if (words > level) {
            level = words;
            source = "words";
        }
        if (stem != null) {
            int biomes = biomeLevel(stem);
            if (biomes > level) {
                level = biomes;
                source = "biomes";
            }
            int type = typeLevel(stem);
            if (type > level) {
                level = type;
                source = "type";
            }
        }

        if (!"minecraft".equals(id.getNamespace()) && settings.unknownDimensionLevel() > level) {
            level = settings.unknownDimensionLevel();
            source = "unknown_level";
        }
        return level > 0 ? new Rating(level, source) : Rating.NONE;
    }

    private static int biomeLevel(LevelStem stem) {
        try {
            Set<Holder<Biome>> biomes = stem.generator().getBiomeSource().possibleBiomes();
            if (biomes == null || biomes.isEmpty()) return 0;
            long sum = 0;
            for (Holder<Biome> biome : biomes) {
                sum += BiomeDangerEvaluator.getDangerLevel(biome);
            }
            return (int) Math.round((double) sum / biomes.size());
        } catch (Exception e) {

            return 0;
        }
    }

    private static int typeLevel(LevelStem stem) {
        try {
            DimensionType type = stem.type().value();
            if (type.ultraWarm()) return LEVEL_ULTRAWARM;
            if (!type.hasSkyLight()) return LEVEL_NO_SKYLIGHT;
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Highest region level inside a dimension of the given base level (see ScalingSettings#levelCapFor):
     * base + dimension_level_bonus (default +2), or the dimension_level_caps entry, never above the highest level
     * of the mod.
     */
    public static int getMaxDifficulty(ResourceLocation dimensionId, int baseDifficulty,
                                       org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings scaling) {
        return scaling.levelCapFor(dimensionId, baseDifficulty);
    }
}
