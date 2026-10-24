package org.Bacontete.hardcore_region_difficulty.common.villager;

import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.EnchantmentSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.AdvancedEnchantApplier;
import org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityCategory;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityType;

public class VillagerTradeInjector {

    /** Highest villager level (Master). */
    private static final int MAX_VILLAGER_LEVEL = 5;
    /** Chance that an enchanted-book trade of a Master villager is an Advanced book. */
    private static final double MASTER_ADVANCED_BOOK_CHANCE = 0.25D;

    public static MerchantOffer tryUpgradeTrade(Villager villager, MerchantOffer offer, RandomSource random) {
        if (offer == null) {
            return offer;
        }

        ItemStack result = offer.getResult();

        int villagerLevel = villager.getVillagerData().getLevel();
        QualityType quality = getQualityForVillagerLevel(villagerLevel, result, random,
                ItemQuality.luckOf(villager.getTradingPlayer()));

        if (QualityConfig.isEnabled() && ItemQuality.isEligible(result) && !ItemQuality.has(result)) {
            ItemQuality.set(result, quality);
        }

        if (!ForgeConfig.isEnabled() || !result.is(Items.ENCHANTED_BOOK)) {
            return offer;
        }

        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || config.getEnchantmentSettings() == null) {
            return offer;
        }
        EnchantmentSettings settings = config.getEnchantmentSettings();
        double advancedChance = villagerLevel >= MAX_VILLAGER_LEVEL
                ? MASTER_ADVANCED_BOOK_CHANCE
                : settings.getLibrarianTradeChance();
        if (random.nextDouble() >= advancedChance) {
            return offer;
        }

        int difficultyLevel = LocationMatcher.getDifficultyLevel(villager.level(), villager.blockPosition());
        int levelCap = settings.isLibrarianOnlyLevel1() ? 1 : Integer.MAX_VALUE;
        ItemStack advancedBook = AdvancedEnchantApplier.createBook(difficultyLevel, random, levelCap,
                settings.getAdvancedLootLevelDecay());
        if (advancedBook.isEmpty()) {
            return offer;
        }

        if (QualityConfig.isEnabled() && ItemQuality.isEligible(advancedBook)) {
            ItemQuality.set(advancedBook, quality);
        }

        int computedPrice = 20 + 10 * AdvancedEnchantApplier.totalLevel(advancedBook);
        int price = Math.min(64, Math.max(settings.getLibrarianMinEmeraldPrice(), computedPrice));
        ItemStack emeralds = new ItemStack(Items.EMERALD, price);

        return new MerchantOffer(
                emeralds,
                offer.getCostB(),
                advancedBook,
                0,                              
                settings.getLibrarianMaxUses(), 
                offer.getXp(),
                offer.getPriceMultiplier());
    }

    /**
     * Quality of a traded item: the same chances as every other roll (weight), scaled by the villager level (1-5)
     * like a region level, plus the Luck of the player who trades. Below villager_min_quality_level the item is
     * always the standard quality. Null if the item cannot have a quality.
     */
    private static QualityType getQualityForVillagerLevel(int level, ItemStack item, RandomSource random, float luck) {
        QualityCategory category = QualityCategory.of(item);
        if (category == null || !category.isActive()) {
            return null;
        }
        int l = Math.max(1, Math.min(5, level));
        if (l < QualityConfig.VILLAGER_MIN_QUALITY_LEVEL.get()) {
            return ItemQuality.standardFor(category);
        }
        return ItemQuality.roll(random, l, item, luck);
    }
}
