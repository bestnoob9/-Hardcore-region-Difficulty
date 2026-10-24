package org.Bacontete.hardcore_region_difficulty.common.entity;

import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyIcons;
import org.Bacontete.hardcore_region_difficulty.common.network.BossTitlePacket;
import org.Bacontete.hardcore_region_difficulty.forge.NetworkHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BossProximityWarning {

    private static final int LEAVE_MARGIN = 8;

    private static final Map<UUID, Set<UUID>> IN_RANGE = new HashMap<>();

    public static void checkForNearbyBosses(ServerPlayer player) {
        if (!ForgeConfig.isEnabled() || player == null || player.level().isClientSide) {
            return;
        }

        Set<UUID> before = IN_RANGE.getOrDefault(player.getUUID(), Collections.emptySet());
        Set<UUID> now = new HashSet<>();
        Mob announce = null;
        BossLevel announceLevel = null;
        double announceDistance = Double.MAX_VALUE;

        AABB searchBox = player.getBoundingBox().inflate(BossLevel.maxDetectionRadius() + LEAVE_MARGIN);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, searchBox)) {
            if (!mob.isAlive()) {
                continue;
            }

            BossLevel bossLevel = BossDetector.getBossLevel(mob);
            if (bossLevel == null) {
                continue;
            }

            boolean wasInRange = before.contains(mob.getUUID());
            int radius = bossLevel.detectionRadius()
                    + (wasInRange ? LEAVE_MARGIN : 0);
            double distance = player.distanceTo(mob);
            if (distance > radius) {
                continue;
            }

            now.add(mob.getUUID());
            if (!wasInRange && (announceLevel == null || bossLevel.compareTo(announceLevel) > 0
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

    private static void sendBossTitle(ServerPlayer player, Mob mob, BossLevel bossLevel) {
        String entityName = mob.getType().getDescription().getString();

        // icon / color of the boss class from its "difficulty_levels" entry (see BossLevel#configLevel)
        DifficultyConfig cfg = DifficultyConfig.getInstance();
        String icon = cfg == null ? "" : cfg.levelIcon(bossLevel.configLevel());
        int color = cfg == null ? -1 : DifficultyIcons.parseColorRgb(cfg.levelColor(bossLevel.configLevel()));
        NetworkHandler.sendToPlayer(player, new BossTitlePacket(entityName, bossLevel, icon, color));
        HardcoreRegionDifficulty.LOGGER.debug("Boss title sent to {} for {} ({})",
                player.getName().getString(), entityName, bossLevel);
    }

    public static void clearPlayer(UUID playerUuid) {
        IN_RANGE.remove(playerUuid);
    }

    public static void clearAll() {
        IN_RANGE.clear();
    }
}
