package org.Bacontete.hardcore_region_difficulty.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.MobEntry;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PatternMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * What only depends on the mob TYPE: the exclude / whitelist patterns, the boss tags and the global whitelist details.
 * Matching these patterns (string building, tag lookups) used to run again for every mob that joined a level and for
 * every mob around a player once per second (boss warning). The result is the same for all mobs of a type, so it is
 * computed once per type.
 *
 * The cache is dropped when the config is loaded again (a new {@link ScalingSettings} object) and when the tags are
 * reloaded ({@link #clear()}, called from DangerCache.clear()).
 */
public final class MobTypeCache {
    private MobTypeCache() {}

    public record Info(boolean excluded, boolean whitelistOk, boolean miniBossTag, boolean bossTag,
                       boolean forgeBossesTag, MobEntry globalEntry) {}

    private static final Map<EntityType<?>, Info> CACHE = new ConcurrentHashMap<>();
    private static volatile ScalingSettings cacheFor;

    public static Info get(Mob mob) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        ScalingSettings scaling = config == null ? null : config.getScalingSettings();
        if (cacheFor != scaling) {
            CACHE.clear();
            cacheFor = scaling;
        }
        EntityType<?> type = mob.getType();
        Info info = CACHE.get(type);
        if (info == null) {
            info = compute(type, scaling);
            CACHE.put(type, info);
        }
        return info;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Info compute(EntityType<?> type, ScalingSettings scaling) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Predicate<ResourceLocation> tagTest = tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag));

        boolean excluded = false;
        boolean whitelistOk = true;
        MobEntry globalEntry = null;
        if (scaling != null) {
            List<String> exclude = scaling.getExcludePatterns();
            excluded = !exclude.isEmpty() && PatternMatcher.matches(exclude, id, tagTest);
            List<String> whitelist = scaling.getWhitelistPatterns();
            whitelistOk = whitelist.isEmpty() || PatternMatcher.matches(whitelist, id, tagTest);
            for (MobEntry e : scaling.getWhitelistEntries()) {
                if (e.hasDetails() && PatternMatcher.matchesSingle(e.getPattern(), id, tagTest)) {
                    globalEntry = e;
                    break;
                }
            }
        }
        return new Info(excluded, whitelistOk, type.is(BossDetector.MINI_BOSS_TAG), type.is(BossDetector.BOSS_TAG),
                type.is(BossDetector.FORGE_BOSSES_TAG), globalEntry);
    }
}
