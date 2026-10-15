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
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityTypeShield;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityTypeTool;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityTypeWeapon;

public class VillagerTradeInjector {

    public static MerchantOffer tryUpgradeTrade(Villager villager, MerchantOffer offer, RandomSource random) {
        if (offer == null) {
            return offer;
        }

        ItemStack result = offer.getResult();

        int villagerLevel = villager.getVillagerData().getLevel();
        QualityType quality = getQualityForVillagerLevel(villagerLevel, result, random);

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
        if (random.nextDouble() >= settings.getLibrarianTradeChance()) {
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

    private static final Object[][] SHIELD_POOLS_L1 = {
        {QualityTypeShield.THICK, 40}, {QualityTypeShield.HEAVY, 20}, {QualityTypeShield.SLUGGISH, 15},
        {QualityTypeShield.CUMBERSOME, 15}, {QualityTypeShield.SOLID, 10}};
    private static final Object[][] SHIELD_POOLS_L2 = {
        {QualityTypeShield.SOLID, 40}, {QualityTypeShield.THICK, 30}, {QualityTypeShield.HEAVY, 10},
        {QualityTypeShield.SLUGGISH, 10}, {QualityTypeShield.FORTIFIED, 10}};
    private static final Object[][] SHIELD_POOLS_L3 = {
        {QualityTypeShield.FORTIFIED, 40}, {QualityTypeShield.SOLID, 35}, {QualityTypeShield.THICK, 15}, {QualityTypeShield.REINFORCED, 10}};
    private static final Object[][] SHIELD_POOLS_L4 = {
        {QualityTypeShield.FORTIFIED, 40}, {QualityTypeShield.REINFORCED, 40}, {QualityTypeShield.SOLID, 20}};
    private static final Object[][] SHIELD_POOLS_L5 = {
        {QualityTypeShield.REINFORCED, 70}, {QualityTypeShield.FORTIFIED, 30}};

    private static final Object[][] POOL_L1 = {
        {QualityTypeTool.STANDARD, 40}, {QualityTypeTool.POLISHED, 25}, {QualityTypeTool.WORN, 15},
        {QualityTypeWeapon.CHIPPED, 10}, {QualityTypeTool.WEATHERED, 10}};
    private static final Object[][] POOL_L2 = {
        {QualityTypeTool.POLISHED, 30}, {QualityTypeTool.STANDARD, 25}, {QualityTypeTool.REFINED, 20},
        {QualityTypeTool.WEATHERED, 10}, {QualityTypeTool.WORN, 10}, {QualityTypeTool.SUPERIOR, 5}};
    private static final Object[][] POOL_L3 = {
        {QualityTypeTool.SUPERIOR, 30}, {QualityTypeTool.REFINED, 25}, {QualityTypeTool.POLISHED, 20},
        {QualityTypeTool.MASTERWORK, 15}, {QualityTypeTool.STANDARD, 10}};
    private static final Object[][] POOL_L4 = {
        {QualityTypeTool.MASTERWORK, 30}, {QualityTypeTool.SUPERIOR, 25}, {QualityTypeTool.LEGENDARY, 25},
        {QualityTypeTool.PRISTINE, 20}};
    private static final Object[][] POOL_L5 = {
        {QualityTypeTool.LEGENDARY, 25}, {QualityTypeTool.MASTERWORK, 20}, {QualityTypeTool.PRISTINE, 20},
        {QualityTypeTool.ANCIENT, 15}, {QualityTypeTool.FLAWLESS, 20}};

    private static QualityType pick(Object[][] pool, QualityCategory category, RandomSource random) {
        int total = 0;
        for (Object[] e : pool) {
            if (category == null || category.allows((QualityType) e[0])) {
                total += (Integer) e[1];
            }
        }
        if (total <= 0) {
            return getDefaultForCategory(category);
        }
        int roll = random.nextInt(total);
        for (Object[] e : pool) {
            if (category != null && !category.allows((QualityType) e[0])) {
                continue;
            }
            roll -= (Integer) e[1];
            if (roll < 0) {
                return (QualityType) e[0];
            }
        }
        return getDefaultForCategory(category);
    }

    /** Random quality from a pool that gets better with the villager level (1-5). */
    private static QualityType getQualityForVillagerLevel(int level, ItemStack item, RandomSource random) {
        int l = Math.max(1, Math.min(5, level));
        if (ItemQuality.isShield(item)) {
            return pick(switch (l) {
                case 1 -> SHIELD_POOLS_L1;
                case 2 -> SHIELD_POOLS_L2;
                case 3 -> SHIELD_POOLS_L3;
                case 4 -> SHIELD_POOLS_L4;
                default -> SHIELD_POOLS_L5;
            }, QualityCategory.SHIELD, random);
        }
        return pick(switch (l) {
            case 1 -> POOL_L1;
            case 2 -> POOL_L2;
            case 3 -> POOL_L3;
            case 4 -> POOL_L4;
            default -> POOL_L5;
        }, QualityCategory.of(item), random);
    }

    private static QualityType getDefaultForCategory(QualityCategory category) {
        if (category == null) {
            return QualityTypeTool.STANDARD;
        }
        return switch (category) {
            case WEAPON -> QualityTypeWeapon.STANDARD;
            case TOOL -> QualityTypeTool.STANDARD;
            case ARMOR -> QualityTypeTool.STANDARD;
            case RANGED -> QualityTypeTool.STANDARD;
            case SHIELD -> QualityTypeShield.THICK;
        };
    }
}
