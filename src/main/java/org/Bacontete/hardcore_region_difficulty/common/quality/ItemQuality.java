package org.Bacontete.hardcore_region_difficulty.common.quality;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public final class ItemQuality {

    public static final String TAG = "HrdQuality";

    private ItemQuality() {}

    public static boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG, 8);
    }

    public static QualityType get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, 8)) {
            return getStandardForCategory(stack);
        }
        String qualityName = tag.getString(TAG);
        QualityCategory category = QualityCategory.of(stack);
        try {
            QualityType q = parseQuality(qualityName, category);
            if (q == null) {
                return getStandardForCategory(stack);
            }
            return q;
        } catch (IllegalArgumentException e) {
            return getStandardForCategory(stack);
        }
    }

    private static QualityType parseQuality(String name, QualityCategory category) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (category == null) {
            return null;
        }
        return switch (category) {
            case WEAPON -> {
                try { yield QualityTypeWeapon.valueOf(name); }
                catch (IllegalArgumentException e) { yield null; }
            }
            case TOOL -> {
                try { yield QualityTypeTool.valueOf(name); }
                catch (IllegalArgumentException e) { yield null; }
            }
            case ARMOR -> {
                try { yield QualityTypeArmor.valueOf(name); }
                catch (IllegalArgumentException e) { yield null; }
            }
            case RANGED -> {
                try { yield QualityTypeRanged.valueOf(name); }
                catch (IllegalArgumentException e) { yield null; }
            }
            case SHIELD -> {
                try { yield QualityTypeShield.valueOf(name); }
                catch (IllegalArgumentException e) { yield null; }
            }
        };
    }

    private static QualityType getStandardForCategory(ItemStack stack) {
        QualityCategory category = QualityCategory.of(stack);
        if (category == null) {
            return QualityTypeTool.STANDARD;
        }
        return switch (category) {
            case WEAPON -> QualityTypeWeapon.STANDARD;
            case TOOL -> QualityTypeTool.STANDARD;
            case ARMOR -> QualityTypeArmor.STANDARD;
            case RANGED -> QualityTypeRanged.STANDARD;
            case SHIELD -> QualityTypeShield.THICK;
        };
    }

    public static void set(ItemStack stack, QualityType quality) {
        stack.getOrCreateTag().putString(TAG, quality.name());
    }

    public static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem;
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.getItem() instanceof DiggerItem && !(stack.getItem() instanceof AxeItem)) {
            return false;
        }
        Multimap<Attribute, AttributeModifier> modifiers = stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);
        return modifiers.containsKey(Attributes.ATTACK_DAMAGE);
    }

    public static boolean isRanged(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    public static boolean isShield(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem;
    }

    public static boolean isEligible(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1 && 
               (isWeapon(stack) || isTool(stack) || isRanged(stack) || isArmor(stack) || isShield(stack));
    }

    public static QualityType roll(RandomSource random) {
        return roll(random, 1, null);
    }

    public static QualityType roll(RandomSource random, int difficultyLevel) {
        return roll(random, difficultyLevel, null);
    }

    public static QualityType roll(RandomSource random, int difficultyLevel, ItemStack item) {
        QualityCategory category = item != null ? QualityCategory.of(item) : null;
        if (category == null) {
            category = QualityCategory.WEAPON;
        }
        double bonus = QualityConfig.DIFFICULTY_QUALITY_BONUS.get();
        double difficultyMultiplier = 1.0 + (difficultyLevel - 1) * bonus;
        double highMultiplier = 1.0 + (difficultyLevel - 1) * bonus * QualityConfig.HIGH_TIER_BONUS_MULTIPLIER.get();

        QualityType[] types = category.getAllowed().toArray(new QualityType[0]);
        int[] weights = new int[types.length];
        int total = 0;
        for (int i = 0; i < types.length; i++) {
            weights[i] = weightOf(category, types[i], difficultyLevel, difficultyMultiplier, highMultiplier);
            total += weights[i];
        }
        if (total <= 0) {
            return getStandardForCategory(item);
        }
        int pick = random.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0) {
                return types[i];
            }
        }
        return getStandardForCategory(item);
    }

    private static int weightOf(QualityCategory category, QualityType type, int level, double mult, double highMult) {
        String name = type.name();
        return switch (name) {
            case "CRACKED" -> adjustWeight(QualityConfig.WEIGHT_CRACKED.get(), mult, -1);
            case "CRUMBLING" -> adjustWeight(QualityConfig.WEIGHT_CRUMBLING.get(), mult, -1);
            case "CHIPPED" -> adjustWeight(QualityConfig.WEIGHT_CHIPPED.get(), mult, -1);
            case "WORN" -> adjustWeight(QualityConfig.WEIGHT_WORN.get(), mult, -1);
            case "RUSTED" -> adjustWeight(QualityConfig.WEIGHT_RUSTED.get(), mult, -1);
            case "DAMAGED" -> adjustWeight(QualityConfig.WEIGHT_DAMAGED.get(), mult, -1);
            case "WEATHERED" -> adjustWeight(QualityConfig.WEIGHT_WEATHERED.get(), mult, -1);
            case "STANDARD" -> QualityConfig.WEIGHT_STANDARD.get();
            case "POLISHED" -> adjustWeight(QualityConfig.WEIGHT_POLISHED.get(), mult, 1);
            case "REFINED" -> adjustWeight(QualityConfig.WEIGHT_REFINED.get(), mult, 1);
            case "SUPERIOR" -> highWeight(type, QualityConfig.WEIGHT_SUPERIOR.get(), highMult, level);
            case "MASTERWORK" -> highWeight(type, QualityConfig.WEIGHT_MASTERWORK.get(), highMult, level);
            case "LEGENDARY" -> highWeight(type, QualityConfig.WEIGHT_LEGENDARY.get(), highMult, level);
            case "EPIC" -> highWeight(type, QualityConfig.WEIGHT_EPIC.get(), highMult, level);
            case "MYTHIC" -> highWeight(type, QualityConfig.WEIGHT_MYTHIC.get(), highMult, level);
            case "DIVINE" -> highWeight(type, QualityConfig.WEIGHT_DIVINE.get(), highMult, level);
            case "ANCIENT" -> highWeight(type, QualityConfig.WEIGHT_ANCIENT.get(), highMult, level);
            case "PRISTINE" -> highWeight(type, QualityConfig.WEIGHT_PRISTINE.get(), highMult, level);
            case "FLAWLESS" -> highWeight(type, QualityConfig.WEIGHT_FLAWLESS.get(), highMult, level);
            case "BRITTLE" -> adjustWeight(QualityConfig.WEIGHT_BRITTLE.get(), mult, -1);
            case "SLUGGISH" -> adjustWeight(QualityConfig.WEIGHT_SLUGGISH.get(), mult, -1);
            case "CUMBERSOME" -> adjustWeight(QualityConfig.WEIGHT_CUMBERSOME.get(), mult, -1);
            case "HEAVY" -> adjustWeight(QualityConfig.WEIGHT_HEAVY.get(), mult, -1);
            case "THICK" -> QualityConfig.WEIGHT_THICK.get();
            case "SOLID" -> adjustWeight(QualityConfig.WEIGHT_SOLID.get(), mult, 1);
            case "FORTIFIED" -> adjustWeight(QualityConfig.WEIGHT_FORTIFIED.get(), highMult, 1);
            case "REINFORCED" -> adjustWeight(QualityConfig.WEIGHT_REINFORCED.get(), highMult, 1);
            default -> 0;
        };
    }

    public static int steps(QualityType quality) {
        return quality.getTier();
    }

    private static int unlockLevel(QualityType type) {
        String name = type.name();
        return switch (name) {
            case "MYTHIC" -> 4;
            case "DIVINE", "ANCIENT", "PRISTINE", "FLAWLESS" -> 5;
            default -> Integer.MAX_VALUE;
        };
    }

    private static int highWeight(QualityType type, int baseWeight, double multiplier, int difficultyLevel) {
        if (baseWeight <= 0) {
            int unlock = unlockLevel(type);
            if (QualityConfig.UNLOCK_HIGH_QUALITIES.get() && difficultyLevel >= unlock) {
                return (difficultyLevel - unlock) / 2 + 1;
            }
            return 0;
        }
        return (int) Math.round(baseWeight * multiplier);
    }

    private static int adjustWeight(int baseWeight, double multiplier, int direction) {
        if (direction == -1) {
            return Math.max(0, (int) Math.round(baseWeight / multiplier));
        } else if (direction == 1) {
            return (int) Math.round(baseWeight * multiplier);
        }
        return baseWeight;
    }
}
