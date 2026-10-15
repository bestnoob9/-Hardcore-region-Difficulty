package org.Bacontete.hardcore_region_difficulty.common.danger;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.List;

public final class EnemyScanner {
    private EnemyScanner() {}

    public static int count(Level level, BlockPos pos, int radius) {
        if (!(level instanceof ServerLevel serverLevel) || radius <= 0) return 0;
        try {
            int total = 0;
            List<Mob> mobs = serverLevel.getEntitiesOfClass(Mob.class, 
                new net.minecraft.world.phys.AABB(pos).inflate(radius));

            for (Mob mob : mobs) {

                if (isHostile(mob)) {
                    total++;
                }
            }
            return total;
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.debug("Enemy count failed at {}: {}", pos, e.toString());
            return 0;
        }
    }

    private static boolean isHostile(Mob mob) {

        if (mob instanceof net.minecraft.world.entity.animal.Wolf wolf && wolf.isTame()) return false;
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat && cat.isTame()) return false;

        EntityType<?> type = mob.getType();
        if (type == EntityType.COW || type == EntityType.PIG || type == EntityType.SHEEP ||
            type == EntityType.CHICKEN || type == EntityType.RABBIT || type == EntityType.HORSE ||
            type == EntityType.DONKEY || type == EntityType.MULE || type == EntityType.LLAMA ||
            type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER ||
            type == EntityType.BAT || type == EntityType.SQUID || type == EntityType.SALMON ||
            type == EntityType.COD || type == EntityType.TROPICAL_FISH || type == EntityType.PUFFERFISH ||
            type == EntityType.FROG || type == EntityType.TADPOLE || type == EntityType.AXOLOTL ||
            type == EntityType.BEE || type == EntityType.CAT || type == EntityType.WOLF ||
            type == EntityType.PARROT || type == EntityType.OCELOT || type == EntityType.FOX ||
            type == EntityType.PANDA || type == EntityType.TURTLE || type == EntityType.DOLPHIN ||
            type == EntityType.GLOW_SQUID || type == EntityType.GOAT || type == EntityType.SNIFFER ||
            type == EntityType.CAMEL) {
            return false;
        }

        return true;
    }
}
