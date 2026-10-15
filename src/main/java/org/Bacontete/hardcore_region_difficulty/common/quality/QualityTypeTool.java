package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.ChatFormatting;

public enum QualityTypeTool implements QualityType {

    CRACKED(-2, ChatFormatting.DARK_GRAY, -0.17, -0.14, -0.18, -0.18, -0.5),
    CRUMBLING(-1, ChatFormatting.DARK_RED, -0.12, 0.03, -0.15, -0.06, -0.3),
    WORN(-1, ChatFormatting.GRAY, -0.07, 0.03, -0.1, -0.06, 0),
    RUSTED(-2, ChatFormatting.DARK_RED, -0.14, -0.1, -0.16, -0.15, -0.4),
    WEATHERED(-1, ChatFormatting.GRAY, -0.08, -0.06, 0.04, -0.1, 0.2),
    STANDARD(0, ChatFormatting.WHITE, 0, 0, 0, 0, 0),
    POLISHED(1, ChatFormatting.AQUA, 0.04, 0.06, -0.04, 0.04, 0.2),
    REFINED(1, ChatFormatting.BLUE, 0.06, -0.02, 0.06, 0.05, 0.4),
    SUPERIOR(2, ChatFormatting.GREEN, 0.09, 0.07, -0.06, 0.08, 0.6),
    MASTERWORK(2, ChatFormatting.DARK_GREEN, 0.11, 0.09, 0.11, 0.1, 0.8),
    LEGENDARY(3, ChatFormatting.GOLD, 0.14, 0.11, 0.13, 0.12, 1.0),
    ANCIENT(4, ChatFormatting.DARK_AQUA, 0.17, 0.17, 0.18, 0.17, 1.3),
    PRISTINE(3, ChatFormatting.GREEN, 0.15, 0.14, 0.16, 0.14, 1.1),
    FLAWLESS(4, ChatFormatting.AQUA, 0.2, 0.18, 0.2, 0.2, 1.5);

    private final int tier;
    private final ChatFormatting color;
    // Sát thương (%)
    private final double damagePercent;
    // TĐ Tấn công (%)
    private final double speedPercent;
    // Độ bền
    private final double durabilityPercent;
    // Đào (%)
    private final double miningPercent;
    // Reach
    private final double reach;

    QualityTypeTool(int tier, ChatFormatting color, double damagePercent, double speedPercent, double durabilityPercent,
                    double miningPercent, double reach) {
        this.tier = tier;
        this.color = color;
        this.damagePercent = damagePercent;
        this.speedPercent = speedPercent;
        this.durabilityPercent = durabilityPercent;
        this.miningPercent = miningPercent;
        this.reach = reach;
    }

    public int getTier() { return tier; }

    public ChatFormatting getColor() { return color; }

    public double getDamagePercent() { return damagePercent; }

    public double getSpeedPercent() { return speedPercent; }

    public double getDurabilityPercent() { return durabilityPercent; }

    public double getMiningPercent() { return miningPercent; }

    public double getReach() { return reach; }

    public double getArmorBonus() { return 0; }

    public double getMoveSpeedPercent() { return 0; }

    public double getHoldMiningPercent() { return 0; }

    public boolean isShieldQuality() { return false; }

    public boolean isNegative() { return tier < 0; }

    public boolean isPositive() { return tier > 0; }
}
