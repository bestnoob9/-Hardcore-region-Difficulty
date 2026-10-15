package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpawnerScanner {
    private SpawnerScanner() {}

    private static final long CACHE_TTL_TICKS = 1200L;

    private static final long[] NONE = new long[0];

    public static int count(Level level, BlockPos pos, int radius) {
        if (!(level instanceof ServerLevel serverLevel) || radius <= 0) return 0;
        try {
            ResourceLocation dim = serverLevel.dimension().location();
            long now = serverLevel.getGameTime();
            long radiusSq = (long) radius * radius;
            int minCx = (pos.getX() - radius) >> 4, maxCx = (pos.getX() + radius) >> 4;
            int minCz = (pos.getZ() - radius) >> 4, maxCz = (pos.getZ() + radius) >> 4;

            int total = 0;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    long[] spawners = spawnersInChunk(serverLevel, dim, cx, cz, now);
                    for (long packed : spawners) {
                        long dx = BlockPos.getX(packed) - pos.getX();
                        long dy = BlockPos.getY(packed) - pos.getY();
                        long dz = BlockPos.getZ(packed) - pos.getZ();
                        if (dx * dx + dy * dy + dz * dz <= radiusSq) total++;
                    }
                }
            }
            return total;
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.debug("Spawner count failed at {}: {}", pos, e.toString());
            return 0;
        }
    }

    public static BlockPos nearest(Level level, BlockPos pos, int radius) {
        if (!(level instanceof ServerLevel serverLevel) || radius <= 0) return null;
        try {
            ResourceLocation dim = serverLevel.dimension().location();
            long now = serverLevel.getGameTime();
            long radiusSq = (long) radius * radius;
            int minCx = (pos.getX() - radius) >> 4, maxCx = (pos.getX() + radius) >> 4;
            int minCz = (pos.getZ() - radius) >> 4, maxCz = (pos.getZ() + radius) >> 4;

            long bestPacked = 0;
            long bestDistSq = Long.MAX_VALUE;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    for (long packed : spawnersInChunk(serverLevel, dim, cx, cz, now)) {
                        long dx = BlockPos.getX(packed) - pos.getX();
                        long dy = BlockPos.getY(packed) - pos.getY();
                        long dz = BlockPos.getZ(packed) - pos.getZ();
                        long distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq <= radiusSq && distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPacked = packed;
                        }
                    }
                }
            }
            return bestDistSq == Long.MAX_VALUE ? null : BlockPos.of(bestPacked);
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.debug("Nearest spawner lookup failed at {}: {}", pos, e.toString());
            return null;
        }
    }

    private static long[] spawnersInChunk(ServerLevel level, ResourceLocation dim, int cx, int cz, long now) {
        DangerCache.ChunkKey key = new DangerCache.ChunkKey(dim, ChunkPos.asLong(cx, cz));
        DangerCache.SpawnerChunk cached = DangerCache.getSpawnerChunk(key);
        if (cached != null && now - cached.scannedAt() < CACHE_TTL_TICKS && now >= cached.scannedAt()) {
            return cached.positions();
        }

        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz); 
        if (chunk == null) {
            DangerCache.invalidateSpawnerChunk(key);
            return NONE;
        }

        List<BlockPos> found = null;
        for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
            BlockEntity be = e.getValue();
            if (be != null && be.getBlockState().is(Blocks.SPAWNER)) {
                if (found == null) found = new ArrayList<>(4);
                found.add(e.getKey());
            }
        }
        long[] positions = NONE;
        if (found != null) {
            positions = new long[found.size()];
            for (int i = 0; i < positions.length; i++) positions[i] = found.get(i).asLong();
        }
        DangerCache.putSpawnerChunk(key, new DangerCache.SpawnerChunk(positions, now));
        return positions;
    }

    public static void invalidate(ResourceLocation dimension, ChunkPos chunk) {
        DangerCache.invalidateSpawnerChunk(new DangerCache.ChunkKey(dimension, chunk.toLong()));
    }
}
