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

import java.util.List;

public final class ItemQuality {

    public static final String TAG = "HrdQuality";

    private ItemQuality() {}

    /**
     * True if the stack carries a quality that is currently defined for its category.
     * A tag whose quality was removed from the JSON file counts as "no quality", so the item gets a new one
     * (inventory roll) or can be reforged.
     */
    public static boolean has(ItemStack stack) {
        return find(stack) != null;
    }

    /** The quality of the stack, or null if it has none (or its quality no longer exists). */
    public static QualityType find(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, 8)) {
            return null;
        }
        QualityCategory category = QualityCategory.of(stack);
        if (category == null) {
            return null;
        }
        return QualityRegistry.findExact(category, tag.getString(TAG));
    }

    /** The quality of the stack, or the neutral "standard" quality of its category if it has none. */
    public static QualityType get(ItemStack stack) {
        QualityType q = find(stack);
        return q != null ? q : getStandardForCategory(stack);
    }

    /** The tier 0 quality of the category (or a no-effect placeholder if the file defines none). */
    private static QualityType getStandardForCategory(ItemStack stack) {
        QualityCategory category = stack == null ? null : QualityCategory.of(stack);
        if (category == null) {
            category = QualityCategory.TOOL;
        }
        for (QualityDefinition d : QualityRegistry.all(category)) {
            if (d.getTier() == 0) {
                return d;
            }
        }
        return QualityDefinition.neutral(category);
    }

    /** The tier 0 quality defined for the category, or null if the file defines none. */
    public static QualityType standardFor(QualityCategory category) {
        for (QualityDefinition d : QualityRegistry.all(category)) {
            if (d.getTier() == 0) {
                return d;
            }
        }
        return null;
    }

    /** Stores the quality on the item. A null quality does nothing. */
    public static void set(ItemStack stack, QualityType quality) {
        if (quality == null) {
            return;
        }
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
        if (stack.isEmpty() || stack.getMaxStackSize() != 1) {
            return false;
        }
        QualityCategory category = QualityCategory.of(stack);
        return category != null && category.isActive();
    }

    /** Luck of a player for quality rolls (0 if there is no player). Potion of Luck = +1, Bad Luck = -1. */
    public static float luckOf(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player player ? player.getLuck() : 0.0F;
    }

    public static QualityType roll(RandomSource random) {
        return roll(random, 1, null, 0.0F);
    }

    public static QualityType roll(RandomSource random, int difficultyLevel) {
        return roll(random, difficultyLevel, null, 0.0F);
    }

    /** Random quality for the item. Returns null if its category has no quality with a chance above 0. */
    public static QualityType roll(RandomSource random, int difficultyLevel, ItemStack item) {
        return roll(random, difficultyLevel, item, 0.0F);
    }

    /**
     * Random quality for loot, mob drops, villagers and the inventory: the base chances (weight), scaled by the
     * difficulty / villager level, then by the Luck of the player. (The Quality Forge does not use weights.)
     */
    public static QualityType roll(RandomSource random, int difficultyLevel, ItemStack item, float luck) {
        return roll(random, difficultyLevel, item, luck, true);
    }

    /** Share of one whole tier (split between the qualities that have it), from the tier_chances config. */
    private static double tierMass(int tier) {
        if (tier == 0) return QualityConfig.TIER_STANDARD_CHANCE.get();
        if (tier < 0) return QualityConfig.TIER_BAD_CHANCE.get() * Math.pow(QualityConfig.TIER_BAD_FALLOFF.get(), -tier - 1);
        return QualityConfig.TIER_GOOD_CHANCE.get() * Math.pow(QualityConfig.TIER_GOOD_FALLOFF.get(), tier - 1);
    }

    /**
     * Random quality for the Quality Forge and for crafted / inventory items: NO weights from the file, no difficulty
     * level, no unlock level. The chance depends only on the tier: standard (tier 0) is the likeliest, bad qualities
     * are likelier than good ones, and the higher the tier the rarer. Every quality of the category can be rolled
     * (Mythic, Divine, Flawless...). Luck makes good qualities likelier and bad ones rarer (luck_quality_bonus).
     */
    public static QualityType rollForge(RandomSource random, ItemStack item, float luck) {
        QualityCategory category = item != null ? QualityCategory.of(item) : null;
        if (category == null) {
            category = QualityCategory.WEAPON;
        }
        List<QualityDefinition> types = QualityRegistry.all(category);
        if (types.isEmpty()) {
            return null;
        }
        java.util.Map<Integer, Integer> perTier = new java.util.HashMap<>();
        for (QualityDefinition d : types) {
            perTier.merge(d.getTier(), 1, Integer::sum);
        }
        double highFactor = QualityConfig.HIGH_TIER_BONUS_MULTIPLIER.get();
        double luckBonus = luck * QualityConfig.LUCK_QUALITY_BONUS.get();
        double mult = Math.max(0.1D, 1.0D + luckBonus);
        double highMult = Math.max(0.1D, 1.0D + luckBonus * highFactor);
        double[] weights = new double[types.size()];
        double total = 0.0D;
        for (int i = 0; i < weights.length; i++) {
            int tier = types.get(i).getTier();
            double w = tierMass(tier) / perTier.get(tier);
            if (tier < 0) w /= mult;
            else if (tier == 1) w *= mult;
            else if (tier > 1) w *= highMult;
            weights[i] = w;
            total += w;
        }
        if (total <= 0.0D) {
            return null;
        }
        double pick = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0.0D) {
                return types.get(i);
            }
        }
        for (int i = weights.length - 1; i >= 0; i--) {
            if (weights[i] > 0.0D) return types.get(i);
        }
        return null;
    }

    /** Chance in percent (Luck 0) that the Quality Forge / a crafted item gets this quality, for display. */
    public static double forgeChancePercent(QualityDefinition quality) {
        List<QualityDefinition> types = QualityRegistry.all(quality.getCategory());
        java.util.Map<Integer, Integer> perTier = new java.util.HashMap<>();
        double total = 0.0D;
        for (QualityDefinition d : types) {
            perTier.merge(d.getTier(), 1, Integer::sum);
        }
        for (QualityDefinition d : types) {
            total += tierMass(d.getTier()) / perTier.get(d.getTier());
        }
        if (total <= 0.0D) {
            return 0.0D;
        }
        int same = perTier.getOrDefault(quality.getTier(), 1);
        return tierMass(quality.getTier()) / same * 100.0D / total;
    }

    public static QualityType rollForge(RandomSource random, ItemStack item) {
        return rollForge(random, item, 0.0F);
    }

    /** One shared roll for every source. useLevel = false ignores the difficulty level completely (Quality Forge). */
    private static QualityType roll(RandomSource random, int difficultyLevel, ItemStack item, float luck, boolean useLevel) {
        QualityCategory category = item != null ? QualityCategory.of(item) : null;
        if (category == null) {
            category = QualityCategory.WEAPON;
        }
        double bonus = QualityConfig.DIFFICULTY_QUALITY_BONUS.get();
        double highFactor = QualityConfig.HIGH_TIER_BONUS_MULTIPLIER.get();
        double difficultyMultiplier = 1.0D;
        double highMultiplier = 1.0D;
        // the highest level of the mod (5, or the highest level the player added): the quality scale follows it
        int maxLevel = 5;
        boolean ownLevel = false;
        if (useLevel) {
            difficultyMultiplier = 1.0 + (difficultyLevel - 1) * bonus;
            highMultiplier = 1.0 + (difficultyLevel - 1) * bonus * highFactor;
            // a level can set its own quality chances (quality_bonus_percent / high_quality_bonus_percent)
            org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig cfg =
                    org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig.getInstance();
            if (cfg != null) {
                maxLevel = Math.max(5, cfg.getScalingSettings().maxRegionLevel());
                Double own = cfg.getScalingSettings().qualityMultiplier(difficultyLevel);
                Double ownHigh = cfg.getScalingSettings().highQualityMultiplier(difficultyLevel);
                if (own != null) { difficultyMultiplier = own; ownLevel = true; }
                if (ownHigh != null) highMultiplier = ownHigh;
            }
        }
        // Luck (potion of luck...) stacks on top of the level, the same way for every roll source
        double luckBonus = luck * QualityConfig.LUCK_QUALITY_BONUS.get();
        difficultyMultiplier = Math.max(0.1D, difficultyMultiplier + luckBonus);
        highMultiplier = Math.max(0.1D, highMultiplier + luckBonus * highFactor);
        // the higher the level, the fewer low qualities (not when the level sets its own numbers, never in the forge)
        double lowFactor = !useLevel || ownLevel ? 1.0 : lowQualityFactor(difficultyLevel, maxLevel);

        List<QualityDefinition> types = QualityRegistry.all(category);
        // one "step" of an unlocked quality = 1 percent of the category, so it fits weights of any size
        double baseTotal = 0.0D;
        for (QualityDefinition d : types) {
            baseTotal += d.getWeight();
        }
        double unit = Math.max(1.0D, baseTotal / 100.0D);
        double[] weights = new double[types.size()];
        double total = 0.0D;
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weightOf(types.get(i), useLevel, difficultyLevel, maxLevel, difficultyMultiplier, highMultiplier, lowFactor, unit);
            total += weights[i];
        }
        if (total <= 0.0D) {
            return null;
        }
        double pick = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0.0D) {
                return types.get(i);
            }
        }
        // rounding at the very end: the last quality that can be rolled
        for (int i = weights.length - 1; i >= 0; i--) {
            if (weights[i] > 0.0D) return types.get(i);
        }
        return null;
    }

    /**
     * Share (0-1) of the weight of the low qualities that is left at a level: 1 at level 1, and
     * 1 - low_quality_reduction at the highest level of the mod (levels above it count as the highest).
     */
    public static double lowQualityFactor(int difficultyLevel, int maxLevel) {
        if (maxLevel <= 1 || difficultyLevel <= 1) {
            return 1.0D;
        }
        double progress = Math.min(1.0D, (difficultyLevel - 1.0D) / (maxLevel - 1.0D));
        return 1.0D - progress * QualityConfig.LOW_QUALITY_REDUCTION.get();
    }

    private static double weightOf(QualityDefinition type, boolean useLevel, int level, int maxLevel, double mult, double highMult, double lowFactor, double unit) {
        int base = type.getWeight();
        return switch (type.getWeightGroup()) {
            case NEGATIVE -> Math.max(0.0D, base / mult * lowFactor);
            case NEUTRAL -> base;
            case POSITIVE -> base * mult;
            case HIGH -> highWeight(type, base, highMult, level, maxLevel, unit);
        };
    }

    public static int steps(QualityType quality) {
        return quality.getTier();
    }

    private static double highWeight(QualityDefinition type, int baseWeight, double multiplier, int difficultyLevel, int maxLevel, double unit) {
        if (baseWeight <= 0) {
            int unlock = unlockLevelFor(type.getUnlockLevel(), maxLevel);
            if (unlock > 0 && difficultyLevel > 0 && QualityConfig.UNLOCK_HIGH_QUALITIES.get() && difficultyLevel >= unlock) {
                // +1 every 2 levels on the 5-level scale, stretched when the player added levels
                return (Math.floor((difficultyLevel - unlock) * (5.0D / Math.max(5, maxLevel)) / 2.0D) + 1.0D) * unit;
            }
            return 0.0D;
        }
        return baseWeight * multiplier;
    }

    /** The unlock level of a high quality, stretched from the 5-level scale to the highest level of the mod. */
    private static int unlockLevelFor(int unlockLevel, int maxLevel) {
        if (unlockLevel <= 0 || maxLevel <= 5) {
            return unlockLevel;
        }
        return Math.max(unlockLevel, (int) Math.round(unlockLevel * (maxLevel / 5.0D)));
    }
}
