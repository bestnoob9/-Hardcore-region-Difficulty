package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.ChatFormatting;

public enum QualityTypeWeapon implements QualityType {

    CRACKED(-2, ChatFormatting.DARK_GRAY, -0.17, -0.14, -0.18, -0.4),
    CHIPPED(-1, ChatFormatting.RED, 0.05, -0.12, -0.15, 0),
    WORN(-1, ChatFormatting.GRAY, -0.08, 0.03, -0.1, 0),
    RUSTED(-2, ChatFormatting.DARK_RED, -0.14, -0.1, -0.16, -0.3),
    DAMAGED(-2, ChatFormatting.DARK_GRAY, -0.2, -0.2, -0.2, -0.5),
    STANDARD(0, ChatFormatting.WHITE, 0, 0, 0, 0),
    POLISHED(1, ChatFormatting.AQUA, 0.04, 0.06, -0.04, 0.2),
    REFINED(1, ChatFormatting.BLUE, 0.06, -0.02, 0.06, 0.4),
    SUPERIOR(2, ChatFormatting.GREEN, 0.09, 0.07, -0.06, 0.6),
    MASTERWORK(2, ChatFormatting.DARK_GREEN, 0.11, 0.09, 0.11, 0.8),
    LEGENDARY(3, ChatFormatting.GOLD, 0.14, 0.11, 0.13, 1.0),
    EPIC(3, ChatFormatting.LIGHT_PURPLE, 0.16, 0.13, 0.15, 1.1),
    MYTHIC(4, ChatFormatting.DARK_PURPLE, 0.17, 0.15, 0.16, 1.2),
    DIVINE(4, ChatFormatting.YELLOW, 0.19, 0.16, 0.18, 1.35),
    FLAWLESS(4, ChatFormatting.AQUA, 0.2, 0.18, 0.2, 1.5);

    private final int tier;
    private final ChatFormatting color;
    // sát thương (%)
    private final double damagePercent;
    // tđ tấn công (%)
    private final double speedPercent;
    // độ bền (%)
    private final double durabilityPercent;
    // reach
    private final double reach;

    QualityTypeWeapon(int tier, ChatFormatting color, double damagePercent, double speedPercent, double durabilityPercent,
                      double reach) {
        this.tier = tier;
        this.color = color;
        this.damagePercent = damagePercent;
        this.speedPercent = speedPercent;
        this.durabilityPercent = durabilityPercent;
        this.reach = reach;
    }

    public int getTier() { return tier; }

    public ChatFormatting getColor() { return color; }

    public double getDamagePercent() { return damagePercent; }

    public double getSpeedPercent() { return speedPercent; }

    public double getDurabilityPercent() { return durabilityPercent; }

    public double getReach() { return reach; }

    public double getMiningPercent() { return 0; }

    public double getArmorBonus() { return 0; }

    public double getMoveSpeedPercent() { return 0; }

    public double getHoldMiningPercent() { return 0; }

    public boolean isShieldQuality() { return false; }

    public boolean isNegative() { return tier < 0; }

    public boolean isPositive() { return tier > 0; }
}
