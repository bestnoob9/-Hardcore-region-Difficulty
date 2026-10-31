package org.Bacontete.hardcore_region_difficulty.common.difficulty;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;

import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegionTracker {
    private static final Map<UUID, RegionInfo> playerRegions = new ConcurrentHashMap<>();
    /** For the last change: would HRD's old title rules have announced it? (false = only Traveler's Titles needs it) */
    private static final Map<UUID, Boolean> legacyAnnounce = new ConcurrentHashMap<>();
    /** dimension + biome the player was in at the last check (for the cheap biome-change test). */
    private static final Map<UUID, String> lastBiome = new ConcurrentHashMap<>();

    /**
     * Biomes whose difficulty is only shown together with the Traveler's Titles title (plains is rated "safe", so
     * HRD's own title never announces it). The server still sends them (also when no region matches, then it is the
     * base level 1), but flagged as "not announced by the old rules", so players without Traveler's Titles ignore them.
     */
    private static final Set<String> TT_ONLY_BIOMES = Set.of("minecraft:plains", "minecraft:sunflower_plains");

    private static String biomeKey(Player player) {
        Level level = player.level();
        return level.dimension().location() + "|" + level.getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().toString()).orElse("");
    }

    /** Cheap test (one biome lookup): did the player enter another biome / dimension since the last full check? */
    public static boolean biomeChanged(Player player) {
        return !biomeKey(player).equals(lastBiome.get(player.getUUID()));
    }

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
        lastBiome.put(player.getUUID(), biomeKey(player));
        LocationMatcher.Match match = LocationMatcher.match(level, player.blockPosition());
        String dimension = level.dimension().location().toString();
        RegionInfo oldInfo = playerRegions.get(player.getUUID());

        RegionInfo newInfo;
        if (match == null) {
            String plainsId = level.getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> key.location().toString()).orElse("");
            DifficultyConfig config = DifficultyConfig.getInstance();
            if (TT_ONLY_BIOMES.contains(plainsId) && config != null && ForgeConfig.isEnabled()) {
                // no region here: plains gets the base difficulty (level 1) for Traveler's Titles
                newInfo = new RegionInfo(1, DangerAnalyzer.prettify(plainsId.substring(plainsId.indexOf(':') + 1)),
                        "Difficulty " + DangerAnalyzer.roman(1), "", dimension, true, plainsId,
                        config.levelIcon(1), config.levelColor(1));
            } else {
                newInfo = new RegionInfo(1, "", "", "", dimension, false, "");
            }
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
            // the real biome is tracked too, so a move into another biome of the same level is still sent
            // (Traveler's Titles shows a title for it and needs the difficulty line)
            String realBiome = level.getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> key.location().toString()).orElse("");
            newInfo = new RegionInfo(match.region().getDifficultyLevel(), match.region().getRegionName(),
                    match.region().getDifficultyTitle(), match.structureName(), dimension, false, realBiome,
                    match.region().getIcon(), match.region().getTitleColor());
        }

        playerRegions.put(player.getUUID(), newInfo);
        legacyAnnounce.put(player.getUUID(), shouldAnnounceLegacy(oldInfo, newInfo));
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
     * - Staying inside the same structure: never shown again (whatever changes: level, biome...).
     * - Any other change of level: shown.
     */
    static boolean shouldAnnounceLegacy(RegionInfo oldInfo, RegionInfo newInfo) {
        if (newInfo.structureName.isEmpty() && TT_ONLY_BIOMES.contains(newInfo.biomeId)) {
            return false;
        }
        if (oldInfo == null) {
            return true;
        }
        boolean newInStructure = !newInfo.structureName.isEmpty();
        boolean oldInStructure = !oldInfo.structureName.isEmpty();
        if (newInStructure || oldInStructure) {
            boolean sameStructure = newInStructure && oldInStructure
                    && oldInfo.structureName.equals(newInfo.structureName)
                    && oldInfo.regionName.equals(newInfo.regionName);
            return !sameStructure;
        }
        // biome -> biome: the title text differs by kind ("Adventure III" / "Dimension III"), so only the level counts
        return oldInfo.difficultyLevel != newInfo.difficultyLevel;
    }

    /**
     * What is SENT to the client: everything the old rules announce, plus every biome change (even the same level),
     * because Traveler's Titles shows a title for every new biome and needs its difficulty line.
     * Players without Traveler's Titles ignore the extra ones (see {@link #wasLegacyAnnounce}).
     */
    static boolean shouldAnnounce(RegionInfo oldInfo, RegionInfo newInfo) {
        if (shouldAnnounceLegacy(oldInfo, newInfo)) {
            return true;
        }
        if (oldInfo == null) {
            // first region after joining: always sent (the old rules may say "not shown", e.g. plains without Traveler's Titles)
            return true;
        }
        // inside the same structure nothing is announced, not even for Traveler's Titles biome changes
        if (!newInfo.structureName.isEmpty() && !oldInfo.structureName.isEmpty()) {
            return false;
        }
        return !oldInfo.regionName.equals(newInfo.regionName) || !oldInfo.biomeId.equals(newInfo.biomeId);
    }

    /** True when the last change is also announced by HRD's old title rules. */
    public static boolean wasLegacyAnnounce(Player player) {
        return legacyAnnounce.getOrDefault(player.getUUID(), true);
    }

    public static RegionInfo getPlayerRegion(Player player) {
        return playerRegions.get(player.getUUID());
    }

    public static void removePlayer(UUID uuid) {
        playerRegions.remove(uuid);
        legacyAnnounce.remove(uuid);
        lastBiome.remove(uuid);
    }

    public static void clearAll() {
        playerRegions.clear();
        legacyAnnounce.clear();
        lastBiome.clear();
    }
}
