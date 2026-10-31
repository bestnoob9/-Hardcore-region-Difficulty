package org.Bacontete.hardcore_region_difficulty.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.TitleDisplaySettings;
import org.Bacontete.hardcore_region_difficulty.common.entity.BossLevel;
import org.Bacontete.hardcore_region_difficulty.forge.ClientConfig;
import org.Bacontete.hardcore_region_difficulty.forge.compat.TravelersTitlesCompat;

import java.util.HashMap;
import java.util.Map;

public class TitleDisplayHandler {

    /**
     * @param icon  icon of the region ("" = default skull of the tier)
     * @param color subtitle color 0xRRGGBB, -1 = default
     */
    public record Active(String name, String subtitle, String skullIcon, long startMillis,
                         int fadeInTicks, int stayTicks, int fadeOutTicks,
                         double scale, int yOffset, String icon, int color) {}

    /** Regions have skull_1 ... skull_5; bosses have their own skulls (see {@link BossLevel#skullIcon()}). */
    public static final int MAX_SKULL_LEVEL = 5;

    public static int skullTier(int difficultyLevel) {
        return Math.max(1, Math.min(MAX_SKULL_LEVEL, difficultyLevel));
    }

    private static String skullIcon(int tier) {
        return "skull_" + tier;
    }

    private static ChatFormatting skullColor(int tier) {
        return switch (tier) {
            case 1 -> ChatFormatting.GRAY;
            case 2 -> ChatFormatting.YELLOW;
            case 3 -> ChatFormatting.GOLD;
            case 4 -> ChatFormatting.RED;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static volatile Active current;
    private static final Map<String, Long> LAST_SHOWN = new HashMap<>();

    /** Structure of the previous title packet (including packets blocked by the cooldown), used to tell when the player has just left a structure. */
    private static String lastPacketStructure = "";

    /** Level of the previous title packet; a level change is always shown and is never blocked by the cooldown. */
    private static int lastPacketLevel = Integer.MIN_VALUE;

    public static Active current() {
        return current;
    }

    public static void clear() {
        current = null;
    }

    public static void displayBossTitle(String entityName, BossLevel bossLevel, String icon, int titleColor) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null && config.hasFileChanged()) {
            config.loadConfig();
        }
        TitleDisplaySettings settings = config != null ? config.getTitleDisplaySettings() : new TitleDisplaySettings();
        if (settings == null || !settings.isEnabled() || !ClientConfig.SHOW_BOSS_TITLE.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entityName == null || entityName.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (settings.isVanillaStyle()) {
            minecraft.gui.setTimes(settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks());
            Component skull = Component.literal("\u2620 ");
            skull = titleColor >= 0 ? skull.copy().withStyle(Style.EMPTY.withColor(titleColor)) : skull.copy().withStyle(bossLevel.skullColor());
            minecraft.gui.setSubtitle(skull.copy()
                    .append(Component.translatable(bossLevel.titleKey()).withStyle(ChatFormatting.GRAY)));
            minecraft.gui.setTitle(Component.literal(entityName));
            return;
        }

        current = new Active(entityName, Component.translatable(bossLevel.titleKey()).getString(), bossLevel.skullIcon(), now,
                settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks(),
                settings.getScale(), settings.getYOffset(), icon == null ? "" : icon, titleColor);
    }

    public static void displayTitle(String regionName, String difficultyTitle, String structureName, int difficultyLevel) {
        displayTitle(regionName, difficultyTitle, structureName, difficultyLevel, "", -1, true);
    }

    public static void displayTitle(String regionName, String difficultyTitle, String structureName, int difficultyLevel,
                                    String icon, int titleColor) {
        displayTitle(regionName, difficultyTitle, structureName, difficultyLevel, icon, titleColor, true);
    }

    /**
     * @param legacyAnnounce whether HRD's own old title rules would announce this. Without Traveler's Titles a packet
     *                       where this is false is ignored completely, so HRD behaves exactly as before.
     */
    public static void displayTitle(String regionName, String difficultyTitle, String structureName, int difficultyLevel,
                                    String icon, int titleColor, boolean legacyAnnounce) {
        if (!legacyAnnounce && !TravelersTitlesCompat.isIntegrated()) {
            return;
        }
        String incomingStructure = structureName == null ? "" : structureName;
        boolean leftStructure = !lastPacketStructure.isEmpty() && incomingStructure.isEmpty();
        lastPacketStructure = incomingStructure;
        boolean levelChanged = difficultyLevel != lastPacketLevel;
        lastPacketLevel = difficultyLevel;

        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null && config.hasFileChanged()) {
            config.loadConfig(); 
        }
        TitleDisplaySettings settings = config != null ? config.getTitleDisplaySettings() : new TitleDisplaySettings();
        // With Traveler's Titles the region title cannot be switched off here: only its title is shown and HRD adds the difficulty line
        boolean travelersTitles = TravelersTitlesCompat.isIntegrated();
        if (settings == null) {
            settings = new TitleDisplaySettings();
        }
        if (!travelersTitles && (!settings.isEnabled() || !ClientConfig.SHOW_REGION_TITLE.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || regionName == null || regionName.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        // a structure title always shows (the server already sends it only when the player enters one);
        // biome titles keep the cooldown, but a different level (vs the previous title) always shows
        String key = regionName + "|" + structureName + "|" + difficultyLevel;
        int cooldown = settings.getRepeatCooldownSeconds();
        Long last = LAST_SHOWN.get(key);
        // with Traveler's Titles the cooldown is Traveler's Titles' own: if it stays silent the difficulty line is dropped
        if (!travelersTitles && !leftStructure && !levelChanged && incomingStructure.isEmpty() && cooldown > 0 && last != null
                && now - last < cooldown * 1000L) {
            return;
        }
        if (LAST_SHOWN.size() > 64) {
            LAST_SHOWN.clear();
        }
        LAST_SHOWN.put(key, now);

        int tier = skullTier(difficultyLevel);
        String subtitleText = difficultyTitle == null ? "" : difficultyTitle;

        if (travelersTitles) {
            // biome: the Traveler's Titles title + HRD difficulty line under it.
            // structure: HRD's structure name is shown through the Traveler's Titles renderer + difficulty line under it
            TravelersTitlesCompat.show(incomingStructure.isEmpty() ? null : regionName, subtitleText, skullIcon(tier), icon, titleColor);
            return;
        }

        if (settings.isVanillaStyle()) {
            minecraft.gui.setTimes(settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks());

            Component skull = Component.literal("\u2620 ");
            skull = titleColor >= 0 ? skull.copy().withStyle(Style.EMPTY.withColor(titleColor)) : skull.copy().withStyle(skullColor(tier));
            minecraft.gui.setSubtitle(skull.copy()
                    .append(Component.literal(subtitleText).withStyle(ChatFormatting.GRAY)));
            minecraft.gui.setTitle(Component.literal(regionName));
            return;
        }

        current = new Active(regionName, subtitleText, skullIcon(tier), now,
                settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks(),
                settings.getScale(), settings.getYOffset(), icon == null ? "" : icon, titleColor);
    }
}
