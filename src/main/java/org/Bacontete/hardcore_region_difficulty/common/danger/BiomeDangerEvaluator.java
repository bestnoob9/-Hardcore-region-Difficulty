package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.SmartAutoSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BiomeDangerEvaluator {
    private BiomeDangerEvaluator() {}

    private static final Map<String, Integer> VANILLA = new HashMap<>();
    private static void put(int level, String... ids) {
        for (String id : ids) VANILLA.put(id, level);
    }
    static {
        put(0, "plains", "sunflower_plains", "forest", "flower_forest", "birch_forest",
                "old_growth_birch_forest", "dark_forest", "taiga", "old_growth_pine_taiga",
                "old_growth_spruce_taiga", "savanna", "savanna_plateau", "windswept_savanna",
                "jungle", "sparse_jungle", "bamboo_jungle", "swamp", "mangrove_swamp", "meadow",
                "cherry_grove", "river", "beach", "stony_shore", "mushroom_fields", "the_void");
        put(2, "snowy_plains", "snowy_taiga", "snowy_beach", "frozen_river", "snowy_slopes", "grove",
                "windswept_hills", "windswept_forest", "windswept_gravelly_hills");
        put(3, "desert", "jagged_peaks", "frozen_peaks", "stony_peaks", "ocean", "cold_ocean",
                "lukewarm_ocean", "warm_ocean", "lush_caves", "dripstone_caves");
        put(4, "badlands", "eroded_badlands", "wooded_badlands", "ice_spikes", "frozen_ocean",
                "deep_ocean", "deep_cold_ocean", "deep_lukewarm_ocean", "deep_frozen_ocean");
        put(5, "deep_dark", "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest",
                "basalt_deltas", "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens");
    }

    private static final Map<String, Integer> WORDS = new HashMap<>();
    static {
        for (String w : new String[]{"nether", "end", "crimson", "warped", "basalt", "wasteland"}) WORDS.put(w, 5);
        for (String w : new String[]{"badlands", "mesa"}) WORDS.put(w, 4);
        for (String w : new String[]{"desert", "ocean", "peak", "cave", "mountain"}) WORDS.put(w, 3);
        for (String w : new String[]{"snowy", "frozen"}) WORDS.put(w, 2);
    }

    static int classifyId(String namespace, String path) {
        if (path == null || path.isEmpty()) return -1;
        String lower = path.toLowerCase(Locale.ROOT);
        if ("minecraft".equals(namespace)) {
            Integer exact = VANILLA.get(lower);
            if (exact != null) return exact;
        }
        List<String> words = StructureClassifier.words(lower);
        int best = 0;
        boolean deep = false;
        boolean frozen = false;
        boolean ocean = false;
        for (String w : words) {
            String key = (w.length() > 3 && w.endsWith("s") && !WORDS.containsKey(w)) ? w.substring(0, w.length() - 1) : w;
            Integer level = WORDS.get(key);
            if (level != null && level > best) best = level;
            if (w.equals("deep")) deep = true;
            if (w.equals("frozen")) frozen = true;
            if (key.equals("ocean")) ocean = true;
        }
        if (ocean && (deep || frozen)) best = Math.max(best, 4);
        if (words.contains("deep") && words.contains("dark")) best = 5;
        if (words.contains("ice") && words.contains("spikes")) best = Math.max(best, 4);
        return best > 0 ? best : -1;
    }

    public static int getDangerLevel(Holder<Biome> biome) {
        if (biome == null) return 0;
        ResourceLocation id = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null) return 0;

        DifficultyConfig config = DifficultyConfig.getInstance();
        SmartAutoSettings settings = config == null ? SmartAutoSettings.DEFAULT : config.getSmartAutoSettings();
        Integer override = DangerAnalyzer.firstMatch(settings.biomeLevels(), id,
                tag -> biome.is(TagKey.create(Registries.BIOME, tag)));
        if (override != null) return override;

        int level = classifyId(id.getNamespace(), id.getPath());
        if (level >= 0) return level;

        if (biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_nether")))
                || biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_end")))) return 5;
        if (biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_deep_ocean")))
                || biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_badlands")))) return 4;
        if (biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_ocean")))) return 3;
        return 0;
    }

    public static boolean isDangerous(Holder<Biome> biome) {
        return getDangerLevel(biome) > 0;
    }
}
