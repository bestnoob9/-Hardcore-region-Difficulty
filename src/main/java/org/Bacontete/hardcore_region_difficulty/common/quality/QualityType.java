package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * One quality of one item category. Implemented by {@link QualityDefinition}, which is loaded from
 * config/hardcore_region_difficulty-qualities.json (see {@link QualityRegistry}).
 */
public interface QualityType {
    int getTier();
    /** Color of the quality name (vanilla color or any #RRGGBB). */
    TextColor getTextColor();
    /** Style that only carries the color. */
    Style getStyle();
    /** Colored, translated display name. */
    Component getDisplayName();
    double getDamagePercent();
    double getSpeedPercent();
    double getDurabilityPercent();
    double getMiningPercent();
    double getReach();
    double getArmorBonus();
    double getMoveSpeedPercent();
    double getHoldMiningPercent();
    boolean isShieldQuality();
    boolean isNegative();
    boolean isPositive();
    /** Stable id, stored in the item tag. */
    String name();
}
