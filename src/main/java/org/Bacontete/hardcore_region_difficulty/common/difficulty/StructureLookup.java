package org.Bacontete.hardcore_region_difficulty.common.difficulty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StructureLookup {
    private StructureLookup() {}

    public record Hit(ResourceLocation id, Holder<Structure> holder) {}

    public static List<Hit> at(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.hasChunkAt(pos)) {
            return Collections.emptyList();
        }
        try {
            Registry<Structure> registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
            List<StructureStart> starts = serverLevel.structureManager().startsForStructure(new ChunkPos(pos), s -> true);
            if (starts.isEmpty()) {
                return Collections.emptyList();
            }
            List<Hit> hits = new ArrayList<>(starts.size());
            for (StructureStart start : starts) {
                if (!start.isValid() || !start.getBoundingBox().isInside(pos)) {
                    continue;
                }
                ResourceLocation id = registry.getKey(start.getStructure());
                if (id != null) {
                    hits.add(new Hit(id, registry.wrapAsHolder(start.getStructure())));
                }
            }
            return hits;
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.debug("Structure lookup failed at {}: {}", pos, e.toString());
            return Collections.emptyList();
        }
    }
}
