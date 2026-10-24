package org.Bacontete.hardcore_region_difficulty.common.entity;

import net.minecraft.ChatFormatting;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

/**
 * Class of a boss. Bosses are not part of the numeric difficulty levels (regions use 1-5 and own levels 6-98);
 * everything that depends on the class of a boss lives here instead of in magic numbers.
 * <p>
 * Declared from weakest to strongest, so {@link #compareTo} orders them by strength.
 */
public enum BossLevel {
    MINI_BOSS(99, 48, "mini_boss", "skull_miniboss", ChatFormatting.DARK_PURPLE),
    BOSS(100, 64, "boss", "skull_boss", ChatFormatting.DARK_RED);

    private final int configLevel;
    private final int detectionRadius;
    private final String titleKey;
    private final String skullIcon;
    private final ChatFormatting skullColor;

    BossLevel(int configLevel, int detectionRadius, String keyName, String skullIcon, ChatFormatting skullColor) {
        this.configLevel = configLevel;
        this.detectionRadius = detectionRadius;
        this.titleKey = HardcoreRegionDifficulty.MODID + ".boss." + keyName;
        this.skullIcon = skullIcon;
        this.skullColor = skullColor;
    }

    /**
     * The "level" entry of "difficulty_levels" in the config file that holds the customizable title, icon and
     * color of this boss class. Only the config file format uses this number, it is never a difficulty level of
     * an entity or a region.
     */
    public int configLevel() {
        return configLevel;
    }

    /** Distance in blocks at which a player gets the boss warning title. */
    public int detectionRadius() {
        return detectionRadius;
    }

    /** Translation key of the label shown under the boss name ("MINI BOSS" / "BOSS"). */
    public String titleKey() {
        return titleKey;
    }

    /** Built-in icon (textures/gui/NAME.png) used when the config does not set one. */
    public String skullIcon() {
        return skullIcon;
    }

    /** Color of the skull in the vanilla style title when the config does not set one. */
    public ChatFormatting skullColor() {
        return skullColor;
    }

    /** Boss class whose title is configured by the given "difficulty_levels" level, or null for a normal level. */
    public static BossLevel fromConfigLevel(int level) {
        for (BossLevel boss : values()) {
            if (boss.configLevel == level) {
                return boss;
            }
        }
        return null;
    }

    /** Region levels 1-5 are the built-in ones; own region levels start above it (6 up to {@link #MAX_CUSTOM_LEVEL}). */
    public static final int LAST_BUILT_IN_LEVEL = 5;

    /** Highest own region level a player can create (99 and 100 belong to the boss classes). */
    public static final int MAX_CUSTOM_LEVEL = 98;

    /** Highest built-in level; own levels start above it. */
    public static int lastConfigLevel() {
        return LAST_BUILT_IN_LEVEL;
    }

    /** Largest {@link #detectionRadius()} of all boss classes. */
    public static int maxDetectionRadius() {
        int max = 0;
        for (BossLevel boss : values()) {
            max = Math.max(max, boss.detectionRadius);
        }
        return max;
    }
}
