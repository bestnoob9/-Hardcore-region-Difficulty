package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.ChatFormatting;

public enum QualityTypeRanged implements QualityType {

    CRUMBLING(-1, ChatFormatting.DARK_RED, -0.12, 0.03, -0.15),
    WORN(-1, ChatFormatting.GRAY, -0.07, 0.03, -0.1),
    DAMAGED(-2, ChatFormatting.DARK_GRAY, -0.2, -0.18, -0.2),
    WEATHERED(-1, ChatFormatting.GRAY, -0.08, -0.06, 0.04),
    STANDARD(0, ChatFormatting.WHITE, 0, 0, 0),
    POLISHED(1, ChatFormatting.AQUA, 0.04, 0.06, -0.04),
    REFINED(1, ChatFormatting.BLUE, 0.06, -0.02, 0.06),
    SUPERIOR(2, ChatFormatting.GREEN, 0.09, 0.08, -0.06),
    MASTERWORK(2, ChatFormatting.DARK_GREEN, 0.11, 0.09, 0.11),
    LEGENDARY(3, ChatFormatting.GOLD, 0.14, 0.11, 0.13),
    EPIC(3, ChatFormatting.LIGHT_PURPLE, 0.15, 0.13, 0.14),
    ANCIENT(4, ChatFormatting.DARK_AQUA, 0.17, 0.17, 0.16),
    FLAWLESS(4, ChatFormatting.AQUA, 0.2, 0.18, 0.2);

    private final int tier;
    private final ChatFormatting color;
    // Tên đam to (%)
    private final double damagePercent;
    // Tốc độ băns (%)
    private final double speedPercent;
    // Độ bền (%)
    private final double durabilityPercent;

    QualityTypeRanged(int tier, ChatFormatting color, double damagePercent, double speedPercent, double durabilityPercent) {
        this.tier = tier;
        this.color = color;
        this.damagePercent = damagePercent;
        this.speedPercent = speedPercent;
        this.durabilityPercent = durabilityPercent;
    }

    public int getTier() { return tier; }

    public ChatFormatting getColor() { return color; }

    public double getDamagePercent() { return damagePercent; }

    public double getSpeedPercent() { return speedPercent; }

    public double getDurabilityPercent() { return durabilityPercent; }

    public double getMiningPercent() { return 0; }

    public double getReach() { return 0; }

    public double getArmorBonus() { return 0; }

    public double getMoveSpeedPercent() { return 0; }

    public double getHoldMiningPercent() { return 0; }

    public boolean isShieldQuality() { return false; }

    public boolean isNegative() { return tier < 0; }

    public boolean isPositive() { return tier > 0; }
}
