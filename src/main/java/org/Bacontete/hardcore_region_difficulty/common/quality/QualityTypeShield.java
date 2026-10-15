package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.ChatFormatting;

public enum QualityTypeShield implements QualityType {

    BRITTLE(-2, ChatFormatting.DARK_GRAY, -0.2, -0.5, -0.03, -0.05),
    SLUGGISH(-1, ChatFormatting.GRAY, -0.06, 0.5, -0.12, -0.06),
    CUMBERSOME(-1, ChatFormatting.RED, 0.05, 0.3, -0.03, -0.15),
    HEAVY(-1, ChatFormatting.DARK_GRAY, -0.15, 0.9, -0.1, -0.08),
    THICK(0, ChatFormatting.BLUE, 0.08, 0.4, -0.03, 0),
    SOLID(1, ChatFormatting.GREEN, 0.12, 0.7, 0, -0.03),
    FORTIFIED(1, ChatFormatting.DARK_GREEN, 0.16, 1.1, -0.02, 0),
    REINFORCED(2, ChatFormatting.DARK_BLUE, 0.2, 1.5, 0.03, 0.03);

    private final int tier;
    private final ChatFormatting color;
    // Độ bền
    private final double durabilityPercent;
    // Tăng giáp
    private final double armorBonus;
    // Chạy (%)
    private final double moveSpeedPercent;
    // Tốc độ đào (%)
    private final double holdMiningPercent;

    QualityTypeShield(int tier, ChatFormatting color, double durabilityPercent, double armorBonus, double moveSpeedPercent, double holdMiningPercent) {
        this.tier = tier;
        this.color = color;
        this.durabilityPercent = durabilityPercent;
        this.armorBonus = armorBonus;
        this.moveSpeedPercent = moveSpeedPercent;
        this.holdMiningPercent = holdMiningPercent;
    }

    public int getTier() { return tier; }

    public ChatFormatting getColor() { return color; }

    public double getDamagePercent() { return 0; }

    public double getSpeedPercent() { return 0; }

    public double getDurabilityPercent() { return durabilityPercent; }

    public double getMiningPercent() { return 0; }

    public double getReach() { return 0; }

    public double getArmorBonus() { return armorBonus; }

    public double getMoveSpeedPercent() { return moveSpeedPercent; }

    public double getHoldMiningPercent() { return holdMiningPercent; }

    public boolean isShieldQuality() { return true; }

    public boolean isNegative() { return tier < 0; }

    public boolean isPositive() { return tier > 0; }
}
