package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

/**
 * Immutable definition of one quality (one entry of the quality JSON file).
 * Which effects are meaningful depends on the category:
 * <ul>
 *   <li>WEAPON: damage, speed (attack speed), durability, reach</li>
 *   <li>TOOL: damage, speed, durability, mining, reach</li>
 *   <li>ARMOR: damage (= armor / toughness percent), durability, armorBonus</li>
 *   <li>RANGED: damage (arrow), speed (draw / load), durability</li>
 *   <li>SHIELD: durability, armorBonus, moveSpeed, holdMining</li>
 * </ul>
 * Everything else is always 0 (the loader never sets it).
 */
public final class QualityDefinition implements QualityType {

    /** How the base weight reacts to the difficulty level of the region. */
    public enum WeightGroup {
        NEGATIVE, NEUTRAL, POSITIVE, HIGH;

        public static WeightGroup fromTier(int tier) {
            if (tier < 0) return NEGATIVE;
            if (tier == 0) return NEUTRAL;
            if (tier == 1) return POSITIVE;
            return HIGH;
        }

        public static WeightGroup parse(String s, WeightGroup fallback) {
            if (s == null) return fallback;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    private final QualityCategory category;
    private final String id;
    private final String customName;
    private final TextColor color;
    private final int tier;
    private final int weight;
    /** Old forge_weight of the file: still read so old files load, but ignored (all rolls share weight). */
    private final int forgeWeight;
    private final WeightGroup group;
    private final int unlockLevel;
    private final double damage;
    private final double speed;
    private final double durability;
    private final double mining;
    private final double reach;
    private final double armorBonus;
    private final double moveSpeed;
    private final double holdMining;

    public QualityDefinition(QualityCategory category, String id, String customName, TextColor color, int tier,
                             int weight, WeightGroup group, int unlockLevel,
                             double damage, double speed, double durability, double mining, double reach,
                             double armorBonus, double moveSpeed, double holdMining) {
        this(category, id, customName, color, tier, weight, -1, group, unlockLevel,
                damage, speed, durability, mining, reach, armorBonus, moveSpeed, holdMining);
    }

    public QualityDefinition(QualityCategory category, String id, String customName, TextColor color, int tier,
                             int weight, int forgeWeight, WeightGroup group, int unlockLevel,
                             double damage, double speed, double durability, double mining, double reach,
                             double armorBonus, double moveSpeed, double holdMining) {
        this.forgeWeight = forgeWeight;
        this.category = category;
        this.id = id;
        this.customName = customName == null || customName.isBlank() ? null : customName;
        this.color = color == null ? TextColor.fromRgb(0xFFFFFF) : color;
        this.tier = tier;
        this.weight = weight;
        this.group = group;
        this.unlockLevel = unlockLevel;
        this.damage = damage;
        this.speed = speed;
        this.durability = durability;
        this.mining = mining;
        this.reach = reach;
        this.armorBonus = armorBonus;
        this.moveSpeed = moveSpeed;
        this.holdMining = holdMining;
    }

    /** No-effect placeholder, used when a category has no usable definition. Never stored on items. */
    public static QualityDefinition neutral(QualityCategory category) {
        return new QualityDefinition(category, "STANDARD", null, TextColor.fromRgb(0xFFFFFF), 0,
                0, WeightGroup.NEUTRAL, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public QualityCategory getCategory() { return category; }

    public int getWeight() { return weight; }

        /** Old "forge_weight" of the file (read for compatibility, no longer used by any roll), or -1. */
    @Deprecated
    public int getOwnForgeWeight() { return forgeWeight; }

    public WeightGroup getWeightGroup() { return group; }

    public int getUnlockLevel() { return unlockLevel; }

    /** The custom "name" of the JSON entry, or null. */
    public String getCustomName() { return customName; }

    @Override
    public String name() { return id; }

    @Override
    public int getTier() { return tier; }

    @Override
    public TextColor getTextColor() { return color; }

    @Override
    public Style getStyle() { return Style.EMPTY.withColor(color); }

    @Override
    public Component getDisplayName() {
        String key = "hardcore_region_difficulty.quality." + id;
        if (customName != null) {
            // The custom name may itself be a lang key; if it is not, it is shown as plain text.
            return Component.translatableWithFallback(customName, customName.replace("%", "%%")).withStyle(getStyle());
        }
        return Component.translatableWithFallback(key, prettify(id).replace("%", "%%")).withStyle(getStyle());
    }

    private static String prettify(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.length() == 0 ? id : sb.toString();
    }

    @Override
    public double getDamagePercent() { return damage; }

    @Override
    public double getSpeedPercent() { return speed; }

    @Override
    public double getDurabilityPercent() { return durability; }

    @Override
    public double getMiningPercent() { return mining; }

    @Override
    public double getReach() { return reach; }

    @Override
    public double getArmorBonus() { return armorBonus; }

    @Override
    public double getMoveSpeedPercent() { return moveSpeed; }

    @Override
    public double getHoldMiningPercent() { return holdMining; }

    @Override
    public boolean isShieldQuality() { return category == QualityCategory.SHIELD; }

    @Override
    public boolean isNegative() { return tier < 0; }

    @Override
    public boolean isPositive() { return tier > 0; }
}
