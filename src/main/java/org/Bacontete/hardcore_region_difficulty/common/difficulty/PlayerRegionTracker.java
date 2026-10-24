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

        /** Icon of the region ("" = default skull) and its title color ("" = default). */
        public final String icon;
        public final String titleColor;

        public RegionInfo(int difficultyLevel, String regionName, String difficultyTitle, String structureName) {
            this(difficultyLevel, regionName, difficultyTitle, structureName, "", false, "");
        }

        public RegionInfo(int difficultyLevel, String regionName, String difficultyTitle, String structureName,
                          String dimension, boolean dimensionWide, String biomeId) {
            this(difficultyLevel, regionName, difficultyTitle, structureName, dimension, dimensionWide, biomeId, "", "");
        }

        public RegionInfo(int difficultyLevel, String regionName, String difficultyTitle, String structureName,
                          String dimension, boolean dimensionWide, String biomeId, String icon, String titleColor) {
            this.difficultyLevel = difficultyLevel;
            this.regionName = regionName == null ? "" : regionName;
            this.difficultyTitle = difficultyTitle == null ? "" : difficultyTitle;
            this.structureName = structureName == null ? "" : structureName;
            this.dimension = dimension == null ? "" : dimension;
            this.dimensionWide = dimensionWide;
            this.biomeId = biomeId == null ? "" : biomeId;
            this.icon = icon == null ? "" : icon;
            this.titleColor = titleColor == null ? "" : titleColor;
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
                    match.region().getDifficultyTitle(), "", dimension, true, biomeId,
                    match.region().getIcon(), match.region().getTitleColor());
        } else {
            newInfo = new RegionInfo(match.region().getDifficultyLevel(), match.region().getRegionName(),
                    match.region().getDifficultyTitle(), match.structureName(), dimension, false, "",
                    match.region().getIcon(), match.region().getTitleColor());
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
     * Title rules (biome = no structure at the player's feet, structure = inside a structure):
     * - First region (no old one): shown.
     * - biome -> biome: only when the level differs (the same level is not shown, whatever the title says).
     * - biome -> structure, structure -> biome, structure -> another structure: always shown, any level.
     * - Staying inside the same structure: shown again only if its level changed.
     * - Any other change of level: shown.
     */
    static boolean shouldAnnounce(RegionInfo oldInfo, RegionInfo newInfo) {
        if (oldInfo == null) {
            return true;
        }
        boolean newInStructure = !newInfo.structureName.isEmpty();
        boolean oldInStructure = !oldInfo.structureName.isEmpty();
        if (newInStructure || oldInStructure) {
            boolean sameStructure = newInStructure && oldInStructure
                    && oldInfo.structureName.equals(newInfo.structureName)
                    && oldInfo.regionName.equals(newInfo.regionName);
            return !sameStructure || oldInfo.difficultyLevel != newInfo.difficultyLevel;
        }
        // biome -> biome: the title text differs by kind ("Adventure III" / "Dimension III"), so only the level counts
        return oldInfo.difficultyLevel != newInfo.difficultyLevel;
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
