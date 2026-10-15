package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.ChatFormatting;

/**
 * Balance limits for every quality type (all categories):
 * <ul>
 *   <li>Percent values (damage, speed, durability, mining, move speed, hold mining): -0.20 .. +0.20 (-20% .. +20%)</li>
 *   <li>Flat values (reach, armor bonus): -0.5 .. +1.5</li>
 * </ul>
 */
public interface QualityType {
    int getTier();
    ChatFormatting getColor();
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
    String name();
}
