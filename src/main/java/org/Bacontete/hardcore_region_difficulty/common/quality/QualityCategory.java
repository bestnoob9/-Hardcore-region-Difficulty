package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Item category. Each category only rolls / accepts the QualityTypes that belong to it.
 */
public enum QualityCategory {

    /** Swords, tridents, modded items with ATTACK_DAMAGE (not a DiggerItem). */
    WEAPON,

    /** Pickaxes, shovels, hoes, axes (every DiggerItem). */
    TOOL,

    /** Helmet, chestplate, leggings, boots. */
    ARMOR,

    /** Bows and crossbows. */
    RANGED,

    /** Shields. */
    SHIELD;

    public Set<QualityType> getAllowed() {
        return switch (this) {
            case WEAPON -> new java.util.HashSet<>(EnumSet.allOf(QualityTypeWeapon.class));
            case TOOL -> new java.util.HashSet<>(EnumSet.allOf(QualityTypeTool.class));
            case ARMOR -> new java.util.HashSet<>(EnumSet.allOf(QualityTypeArmor.class));
            case RANGED -> new java.util.HashSet<>(EnumSet.allOf(QualityTypeRanged.class));
            case SHIELD -> new java.util.HashSet<>(EnumSet.allOf(QualityTypeShield.class));
        };
    }

    public boolean allows(QualityType type) {
        return switch (this) {
            case WEAPON -> type instanceof QualityTypeWeapon;
            case TOOL -> type instanceof QualityTypeTool;
            case ARMOR -> type instanceof QualityTypeArmor;
            case RANGED -> type instanceof QualityTypeRanged;
            case SHIELD -> type instanceof QualityTypeShield;
        };
    }

    /** Category names used by the commands. */
    public static final List<String> COMMAND_NAMES = List.of("weapon", "tool", "armor", "ranged", "shield");

    /**
     * Parses a category name from a command (case-insensitive).
     * Accepted: weapon, tool, armor, ranged, shield. Returns null if not recognized.
     */
    public static QualityCategory parse(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "weapon", "weapons" -> WEAPON;
            case "tool", "tools" -> TOOL;
            case "armor", "armour" -> ARMOR;
            case "ranged", "bow", "crossbow" -> RANGED;
            case "shield", "shields" -> SHIELD;
            default -> null;
        };
    }

    /** Category of the item, or null if it cannot have a quality. */
    public static QualityCategory of(ItemStack stack) {
        if (ItemQuality.isShield(stack)) {
            return SHIELD;
        }
        if (ItemQuality.isArmor(stack)) {
            return ARMOR;
        }
        if (ItemQuality.isRanged(stack)) {
            return RANGED;
        }
        if (ItemQuality.isTool(stack)) {
            return TOOL;
        }
        if (ItemQuality.isWeapon(stack)) {
            return WEAPON;
        }
        return null;
    }
}
