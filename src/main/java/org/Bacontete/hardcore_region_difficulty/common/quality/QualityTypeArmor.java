package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.ChatFormatting;

public enum QualityTypeArmor implements QualityType {

    CRACKED(-2, ChatFormatting.DARK_GRAY, -0.17, -0.18, -0.4),
    CRUMBLING(-1, ChatFormatting.DARK_RED, -0.12, -0.15, -0.2),
    RUSTED(-2, ChatFormatting.DARK_RED, -0.14, -0.16, -0.3),
    DAMAGED(-2, ChatFormatting.DARK_GRAY, -0.2, -0.2, -0.5),
    WEATHERED(-1, ChatFormatting.GRAY, -0.08, 0.04, 0.1),
    STANDARD(0, ChatFormatting.WHITE, 0, 0, 0),
    POLISHED(1, ChatFormatting.AQUA, 0.04, -0.04, 0.2),
    REFINED(1, ChatFormatting.BLUE, 0.06, 0.06, 0.3),
    SUPERIOR(2, ChatFormatting.GREEN, 0.09, -0.06, 0.4),
    MASTERWORK(2, ChatFormatting.DARK_GREEN, 0.11, 0.11, 0.6),
    EPIC(3, ChatFormatting.LIGHT_PURPLE, 0.15, 0.14, 0.8),
    MYTHIC(4, ChatFormatting.DARK_PURPLE, 0.17, 0.16, 1.0),
    DIVINE(4, ChatFormatting.YELLOW, 0.19, 0.18, 1.3),
    ANCIENT(4, ChatFormatting.DARK_AQUA, 0.16, 0.16, 1.1),
    PRISTINE(3, ChatFormatting.GREEN, 0.14, 0.15, 0.9),
    FLAWLESS(4, ChatFormatting.AQUA, 0.2, 0.2, 1.5);

    private final int tier;
    private final ChatFormatting color;
    // Sát thương giáp (%)
    private final double damagePercent;
    // độ bền (%)
    private final double durabilityPercent;
    // giảm giáp
    private final double armorBonus;

    QualityTypeArmor(int tier, ChatFormatting color, double damagePercent, double durabilityPercent, double armorBonus) {
        this.tier = tier;
        this.color = color;
        this.damagePercent = damagePercent;
        this.durabilityPercent = durabilityPercent;
        this.armorBonus = armorBonus;
    }

    public int getTier() { return tier; }

    public ChatFormatting getColor() { return color; }

    public double getDamagePercent() { return damagePercent; }

    public double getDurabilityPercent() { return durabilityPercent; }

    public double getArmorBonus() { return armorBonus; }

    public double getSpeedPercent() { return 0; }

    public double getMiningPercent() { return 0; }

    public double getReach() { return 0; }

    public double getMoveSpeedPercent() { return 0; }

    public double getHoldMiningPercent() { return 0; }

    public boolean isShieldQuality() { return false; }

    public boolean isNegative() { return tier < 0; }

    public boolean isPositive() { return tier > 0; }
}
