package org.Bacontete.hardcore_region_difficulty.common.entity;

import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.network.BossTitlePacket;
import org.Bacontete.hardcore_region_difficulty.forge.NetworkHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BossProximityWarning {

    private static final int BOSS_DETECTION_RADIUS = 64;

    private static final int MINI_BOSS_DETECTION_RADIUS = 48;

    private static final int LEAVE_MARGIN = 8;

    private static final Map<UUID, Set<UUID>> IN_RANGE = new HashMap<>();

    public static void checkForNearbyBosses(ServerPlayer player) {
        if (!ForgeConfig.isEnabled() || player == null || player.level().isClientSide) {
            return;
        }

        Set<UUID> before = IN_RANGE.getOrDefault(player.getUUID(), Collections.emptySet());
        Set<UUID> now = new HashSet<>();
        Mob announce = null;
        int announceLevel = 0;
        double announceDistance = Double.MAX_VALUE;

        AABB searchBox = player.getBoundingBox().inflate(BOSS_DETECTION_RADIUS + LEAVE_MARGIN);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, searchBox)) {
            if (!mob.isAlive()) {
                continue;
            }

            EntityType<?> type = mob.getType();
            if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.WARDEN) {
                continue;
            }
            int bossLevel = BossDetector.getBossDifficultyLevel(mob);
            if (bossLevel <= 0) {
                continue;
            }

            boolean wasInRange = before.contains(mob.getUUID());
            int radius = (bossLevel == 7 ? BOSS_DETECTION_RADIUS : MINI_BOSS_DETECTION_RADIUS)
                    + (wasInRange ? LEAVE_MARGIN : 0);
            double distance = player.distanceTo(mob);
            if (distance > radius) {
                continue;
            }

            now.add(mob.getUUID());
            if (!wasInRange && (bossLevel > announceLevel
                    || (bossLevel == announceLevel && distance < announceDistance))) {
                announce = mob;
                announceLevel = bossLevel;
                announceDistance = distance;
            }
        }

        if (now.isEmpty()) {
            IN_RANGE.remove(player.getUUID());
        } else {
            IN_RANGE.put(player.getUUID(), now);
        }

        if (announce != null) {
            sendBossTitle(player, announce, announceLevel);
        }
    }

    private static void sendBossTitle(ServerPlayer player, Mob mob, int bossLevel) {
        String bossTypeKey = bossLevel == 7 ? "hardcore_region_difficulty.boss.boss" : "hardcore_region_difficulty.boss.mini_boss";
        String entityName = mob.getType().getDescription().getString();

        NetworkHandler.sendToPlayer(player, new BossTitlePacket(entityName, bossTypeKey, bossLevel));
        HardcoreRegionDifficulty.LOGGER.debug("Boss title sent to {} for {} (level {})",
                player.getName().getString(), entityName, bossLevel);
    }

    public static void clearPlayer(UUID playerUuid) {
        IN_RANGE.remove(playerUuid);
    }

    public static void clearAll() {
        IN_RANGE.clear();
    }
}
