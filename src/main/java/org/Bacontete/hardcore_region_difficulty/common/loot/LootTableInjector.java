package org.Bacontete.hardcore_region_difficulty.common.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.EnchantmentSettings;
import org.Bacontete.hardcore_region_difficulty.common.danger.DangerAnalyzer;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.StructureLookup;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.AdvancedEnchantApplier;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.AdvancedEnchantments;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.ArmorPiercingEnchantment;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.TrueDamageEnchantment;

/**
 * Advanced loot in chests. Every source here is:
 * - only dropped in regions of level >= {@code advanced_min_level} (default 3),
 * - capped chance: advanced enchant 5%, normal enchant 6% (reached in the highest level region); True Damage 1%, only from level 5.
 */
public class LootTableInjector {

    /** Loot chance cap (in the highest level region); lower regions get a smaller chance following base_chance_per_level. */
    public static final double TRUE_DAMAGE_CHANCE = 0.00025D;
    /** True Damage only drops from regions / structures of this level and above. */
    public static final int TRUE_DAMAGE_MIN_LEVEL = 5;
    public static final double ADVANCED_MAX_CHANCE = 0.05D;
    public static final double NORMAL_MAX_CHANCE = 0.06D;

    private static final double ARMOR_PIERCING_LEVEL_DECAY = 0.5D;

    private static EnchantmentSettings settings() {
        DifficultyConfig config = DifficultyConfig.getInstance();
        return config == null ? null : config.getEnchantmentSettings();
    }

    /** Does this region give Advanced loot (is the level high enough)? */
    public static boolean advancedAllowed(int difficultyLevel) {
        EnchantmentSettings settings = settings();
        return settings != null && settings.allowsAdvancedLoot(difficultyLevel);
    }

    public static ItemStack rollArmorPiercingBook(int difficultyLevel, RandomSource random) {
        EnchantmentSettings settings = settings();
        if (settings == null || !settings.allowsAdvancedLoot(difficultyLevel)
                || !AdvancedEnchantments.isEnabled(AdvancedEnchantments.ARMOR_PIERCING)) {
            return ItemStack.EMPTY;
        }
        if (random.nextDouble() >= scaledChance(settings, NORMAL_MAX_CHANCE, difficultyLevel)) {
            return ItemStack.EMPTY;
        }
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        int level = AdvancedEnchantApplier.rollLevel(ArmorPiercingEnchantment.MAX_LEVEL, ARMOR_PIERCING_LEVEL_DECAY, random);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(AdvancedEnchantments.ARMOR_PIERCING.get(), level));
        return book;
    }

    /** Chance = cap * (base chance of the region level / highest base chance): the higher the level, the closer to the cap. */
    private static double scaledChance(EnchantmentSettings settings, double max, int difficultyLevel) {
        double top = settings.getMaxBaseChance();
        if (top <= 0.0D) {
            return 0.0D;
        }
        return max * Math.min(1.0D, settings.getBaseChance(difficultyLevel) / top);
    }

    public static ItemStack rollTrueDamageBook(int difficultyLevel, RandomSource random) {
        EnchantmentSettings settings = settings();
        if (settings == null || !settings.allowsAdvancedLoot(difficultyLevel)
                || difficultyLevel < TRUE_DAMAGE_MIN_LEVEL
                || !AdvancedEnchantments.isEnabled(AdvancedEnchantments.TRUE_DAMAGE)) {
            return ItemStack.EMPTY;
        }
        if (random.nextDouble() >= TRUE_DAMAGE_CHANCE) {
            return ItemStack.EMPTY;
        }
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book,
                new EnchantmentInstance(AdvancedEnchantments.TRUE_DAMAGE.get(), TrueDamageEnchantment.MAX_LEVEL));
        return book;
    }

    public static ItemStack tryEnchantItem(ItemStack item, BlockPos chestPos, Level level, RandomSource random) {
        if (item.isEmpty()) {
            return item;
        }
        return tryEnchantItem(item, LocationMatcher.getDifficultyLevel(level, chestPos), random);
    }

    public static ItemStack tryEnchantItem(ItemStack item, int difficultyLevel, RandomSource random) {
        if (item.isEmpty()) {
            return item;
        }
        EnchantmentSettings settings = settings();
        if (settings == null || !settings.allowsAdvancedLoot(difficultyLevel)) {
            return item;
        }
        if (random.nextDouble() < scaledChance(settings, ADVANCED_MAX_CHANCE, difficultyLevel)) {
            AdvancedEnchantApplier.apply(item, difficultyLevel, random, Integer.MAX_VALUE,
                    settings.getAdvancedLootLevelDecay(), AdvancedEnchantments.advancedPool());
        }
        if (random.nextDouble() < scaledChance(settings, NORMAL_MAX_CHANCE, difficultyLevel)) {
            AdvancedEnchantApplier.apply(item, difficultyLevel, random, Integer.MAX_VALUE,
                    settings.getAdvancedLootLevelDecay(), AdvancedEnchantments.normalPool());
        }
        return item;
    }

    public static ItemStack rollDungeonBook(String tableId, BlockPos chestPos, Level level, RandomSource random) {
        EnchantmentSettings settings = settings();
        if (settings == null) {
            return ItemStack.EMPTY;
        }
        double multiplier = settings.getAdvancedChanceMultiplier();

        if (settings.isDungeonLootTable(tableId)) {
            int difficultyLevel = LocationMatcher.getDifficultyLevel(level, chestPos);
            if (!settings.allowsAdvancedLoot(difficultyLevel)) {
                return ItemStack.EMPTY;
            }
            if (random.nextDouble() >= Math.min(ADVANCED_MAX_CHANCE, settings.getDungeonChestChance() * multiplier)) {
                return ItemStack.EMPTY;
            }
            return AdvancedEnchantApplier.createBook(difficultyLevel, random, Integer.MAX_VALUE,
                    settings.getAdvancedLootLevelDecay(), AdvancedEnchantments.advancedPool());
        }

        if (!settings.isDungeonBookAuto()) {
            return ItemStack.EMPTY;
        }
        LocationMatcher.Match match = LocationMatcher.match(level, chestPos);
        if (match == null || match.structureName().isEmpty()) {
            return ItemStack.EMPTY;
        }

        boolean chestInside = false;
        for (StructureLookup.Hit hit : StructureLookup.at(level, chestPos)) {
            if (DangerAnalyzer.prettify(hit.id().getPath()).equals(match.structureName())) {
                chestInside = true;
                break;
            }
        }
        if (!chestInside) {
            return ItemStack.EMPTY;
        }
        int difficultyLevel = match.region().getDifficultyLevel();
        if (!settings.allowsAdvancedLoot(difficultyLevel)) {
            return ItemStack.EMPTY;
        }
        EnchantmentSettings.DungeonBookTier tier = settings.getDungeonBookTier(difficultyLevel);
        if (tier == null || random.nextDouble() >= Math.min(ADVANCED_MAX_CHANCE, tier.chance() * multiplier)) {
            return ItemStack.EMPTY;
        }
        return AdvancedEnchantApplier.createBook(difficultyLevel, random, tier.maxLevel(),
                settings.getDungeonBookLevelDecay(), AdvancedEnchantments.advancedPool());
    }
}