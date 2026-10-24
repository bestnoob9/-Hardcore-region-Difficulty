package org.Bacontete.hardcore_region_difficulty.common.quality;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Item category. Each category only rolls / accepts the qualities defined for it in the quality JSON file
 * (config/hardcore_region_difficulty-qualities.json).
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

    /** Key of the category in the JSON file and in commands. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** All qualities currently defined for this category, in file order. */
    public List<QualityType> getAllowed() {
        return new ArrayList<>(QualityRegistry.all(this));
    }

    /** The quality with that id (case-insensitive) in this category, or null. */
    public QualityDefinition find(String id) {
        return QualityRegistry.find(this, id);
    }

    /** False when the quality list of this category is empty: such items get no quality at all. */
    public boolean isActive() {
        return !QualityRegistry.all(this).isEmpty();
    }

    public boolean allows(QualityType type) {
        return type instanceof QualityDefinition d && d.getCategory() == this;
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
