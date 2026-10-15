package org.Bacontete.hardcore_region_difficulty.common.difficulty;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegionTracker {
    private static final Map<UUID, RegionInfo> playerRegions = new ConcurrentHashMap<>();

    public static class RegionInfo {
        public final int difficultyLevel;
        public final String regionName;
        public final String difficultyTitle;
        public final String structureName;

        public final String dimension;

        public final boolean dimensionWide;

        public final String biomeId;

        public RegionInfo(int difficultyLevel, String regionName, String difficultyTitle, String structureName) {
            this(difficultyLevel, regionName, difficultyTitle, structureName, "", false, "");
        }

        public RegionInfo(int difficultyLevel, String regionName, String difficultyTitle, String structureName,
                          String dimension, boolean dimensionWide, String biomeId) {
            this.difficultyLevel = difficultyLevel;
            this.regionName = regionName == null ? "" : regionName;
            this.difficultyTitle = difficultyTitle == null ? "" : difficultyTitle;
            this.structureName = structureName == null ? "" : structureName;
            this.dimension = dimension == null ? "" : dimension;
            this.dimensionWide = dimensionWide;
            this.biomeId = biomeId == null ? "" : biomeId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RegionInfo other)) return false;

            if (difficultyLevel == other.difficultyLevel && difficultyTitle.equals(other.difficultyTitle)) {
                return true;
            }
            if (dimensionWide != other.dimensionWide || difficultyLevel != other.difficultyLevel
                    || !difficultyTitle.equals(other.difficultyTitle)) {
                return false;
            }
            if (dimensionWide) {
                return dimension.equals(other.dimension) && biomeId.equals(other.biomeId);
            }
            return regionName.equals(other.regionName) && structureName.equals(other.structureName);
        }

        @Override
        public int hashCode() {

            return Objects.hash(difficultyLevel, difficultyTitle);
        }
    }

    public static boolean updateAndCheckChange(Player player) {
        Level level = player.level();
        LocationMatcher.Match match = LocationMatcher.match(level, player.blockPosition());
        String dimension = level.dimension().location().toString();
        RegionInfo oldInfo = playerRegions.get(player.getUUID());

        RegionInfo newInfo;
        if (match == null) {
            newInfo = new RegionInfo(1, "", "", "", dimension, false, "");
        } else if (match.dimensionWide()) {
            String biomeId = level.getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> key.location().toString()).orElse("");
            boolean arrived = oldInfo == null || !oldInfo.dimension.equals(dimension);
            String name = arrived || biomeId.isEmpty()
                    ? match.region().getRegionName()
                    : DangerAnalyzer.prettify(biomeId.substring(biomeId.indexOf(':') + 1));
            newInfo = new RegionInfo(match.region().getDifficultyLevel(), name,
                    match.region().getDifficultyTitle(), "", dimension, true, biomeId);
        } else {
            newInfo = new RegionInfo(match.region().getDifficultyLevel(), match.region().getRegionName(),
                    match.region().getDifficultyTitle(), match.structureName(), dimension, false, "");
        }

        playerRegions.put(player.getUUID(), newInfo);
        if (shouldAnnounce(oldInfo, newInfo)) {
            HardcoreRegionDifficulty.LOGGER.debug("Player {} changed region to: {} ({})",
                    player.getName().getString(), newInfo.regionName, newInfo.difficultyTitle);
            return true;
        }
        return false;
    }

    /**
     * Quy tac hien title:
     * - Lan dau (chua co vung cu): hien.
     * - Vao mot cong trinh (tu biome/vung khac hoac tu cong trinh khac): luon hien, bat ke cap.
     * - Dang o trong cung mot cong trinh: khong hien lai.
     * - Biome -> biome (khong co cong trinh): chi hien khi cap/do kho khac nhau.
     * - Ra khoi cong trinh ve biome: luon hien, bat ke cap (thap hay cao, cung cap hay khac cap).
     */
    static boolean shouldAnnounce(RegionInfo oldInfo, RegionInfo newInfo) {
        if (oldInfo == null) {
            return true;
        }
        boolean newInStructure = !newInfo.structureName.isEmpty();
        boolean oldInStructure = !oldInfo.structureName.isEmpty();
        if (newInStructure) {
            if (!oldInStructure) {
                return true;
            }
            return !(oldInfo.structureName.equals(newInfo.structureName)
                    && oldInfo.regionName.equals(newInfo.regionName));
        }
        if (oldInStructure) {
            return true;
        }
        return oldInfo.difficultyLevel != newInfo.difficultyLevel
                || !oldInfo.difficultyTitle.equals(newInfo.difficultyTitle);
    }

    public static RegionInfo getPlayerRegion(Player player) {
        return playerRegions.get(player.getUUID());
    }

    public static void removePlayer(UUID uuid) {
        playerRegions.remove(uuid);
    }

    public static void clearAll() {
        playerRegions.clear();
    }
}
