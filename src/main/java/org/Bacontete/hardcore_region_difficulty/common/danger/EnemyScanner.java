package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts the hostile mobs around a position (the "enemy" part of the automatic level).
 *
 * Only real hostile mobs count (the vanilla Enemy interface: zombies, skeletons, creepers, slimes, ghasts...). Golems,
 * villagers, pets and animals do not, so walking next to them cannot change the level.
 *
 * The count moves all the time (mobs spawn, die, walk in), which made the level flicker. So a higher count is used at
 * once, but a lower one only after {@link #DECAY_TICKS} without reaching the old count again.
 */
public final class EnemyScanner {
    private EnemyScanner() {}

    /** How long a higher count is kept after the mobs are gone (30 seconds). */
    private static final long DECAY_TICKS = 600L;
    private static final int MAX_CELLS = 2048;

    private record Cell(ResourceLocation dimension, int cx, int cz) {}

    private record Held(int count, long time) {}

    private static final Map<Cell, Held> HELD = new ConcurrentHashMap<>();

    public static int count(Level level, BlockPos pos, int radius) {
        if (!(level instanceof ServerLevel serverLevel) || radius <= 0) return 0;
        try {
            // the filter runs while the entities are collected: no list of every mob around is built and walked again
            int total = serverLevel.getEntitiesOfClass(Mob.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(radius), EnemyScanner::isHostile).size();
            return stabilize(serverLevel, pos, total);
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.debug("Enemy count failed at {}: {}", pos, e.toString());
            return 0;
        }
    }

    private static int stabilize(ServerLevel level, BlockPos pos, int raw) {
        long now = level.getGameTime();
        Cell key = new Cell(level.dimension().location(), pos.getX() >> 4, pos.getZ() >> 4);
        Held held = HELD.get(key);
        if (held != null && now >= held.time() && now - held.time() < DECAY_TICKS) {
            if (raw >= held.count()) {
                HELD.put(key, new Held(raw, now));
                return raw;
            }
            return held.count();
        }
        if (HELD.size() > MAX_CELLS) {
            HELD.entrySet().removeIf(e -> now < e.getValue().time() || now - e.getValue().time() >= DECAY_TICKS);
        }
        HELD.put(key, new Held(raw, now));
        return raw;
    }

    public static void clear() {
        HELD.clear();
    }

    private static boolean isHostile(Mob mob) {
        return mob instanceof Enemy && mob.isAlive();
    }
}
