package org.Bacontete.hardcore_region_difficulty.common.villager;

import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.AdvancedEnchantments;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.TrueDamageEnchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
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
    /**
     * Lowest price (emeralds) of an Advanced book, whatever the discounts: curing a zombie villager (reputation)
     * and Hero of the Village can lower the price, but never below this.
     */
    private static final int ADVANCED_BOOK_PRICE_FLOOR = 25;
    /** Fixed price (emeralds) of a True Damage book: no discount (zombie cure, Hero of the Village) applies. */
    private static final int TRUE_DAMAGE_PRICE = 64;
    /** Chance (0.025%) that an enchanted-book trade of a Master villager is a True Damage book. */
    private static final double TRUE_DAMAGE_TRADE_CHANCE = 0.00025D;

    /** Called right before the trade screen is sent: vanilla has just recomputed the special prices. */
    public static void applyPriceFloor(MerchantOffers offers) {
        if (offers == null) {
            return;
        }
        for (MerchantOffer offer : offers) {
            if (!isAdvancedBook(offer.getResult())) {
                continue;
            }
            int base = offer.getBaseCostA().getCount();
            int floor = Math.min(isTrueDamageBook(offer.getResult()) ? TRUE_DAMAGE_PRICE : ADVANCED_BOOK_PRICE_FLOOR,
                    base);
            int demandPart = Math.max(0, (int) Math.floor(base * offer.getDemand() * offer.getPriceMultiplier()));
            // price = base + demandPart + specialPriceDiff, so the smallest allowed diff is:
            int minDiff = floor - base - demandPart;
            if (offer.getSpecialPriceDiff() < minDiff) {
                offer.setSpecialPriceDiff(minDiff);
            }
        }
    }

    private static boolean isTrueDamageBook(ItemStack stack) {
        return stack != null && stack.is(Items.ENCHANTED_BOOK) && AdvancedEnchantments.TRUE_DAMAGE.isPresent()
                && EnchantmentHelper.getEnchantments(stack).containsKey(AdvancedEnchantments.TRUE_DAMAGE.get());
    }

    private static MerchantOffer trueDamageOffer(MerchantOffer offer, ItemStack book, QualityType quality,
                                                 EnchantmentSettings settings) {
        if (QualityConfig.isEnabled() && ItemQuality.isEligible(book) && !ItemQuality.has(book)) {
            ItemQuality.set(book, quality);
        }
        return new MerchantOffer(
                new ItemStack(Items.EMERALD, TRUE_DAMAGE_PRICE),
                offer.getCostB(),
                book,
                0,
                settings.getLibrarianMaxUses(),
                offer.getXp(),
                offer.getPriceMultiplier());
    }

    private static boolean isAdvancedBook(ItemStack stack) {
        if (stack == null || !stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        for (Enchantment ench : EnchantmentHelper.getEnchantments(stack).keySet()) {
            ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(ench);
            if (key != null && HardcoreRegionDifficulty.MODID.equals(key.getNamespace())) {
                return true;
            }
        }
        return false;
    }

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
        // True Damage: only Master librarians, very rare. (It is not tradeable in vanilla's pool anymore.)
        if (villagerLevel >= MAX_VILLAGER_LEVEL
                && AdvancedEnchantments.isEnabled(AdvancedEnchantments.TRUE_DAMAGE)
                && random.nextDouble() < TRUE_DAMAGE_TRADE_CHANCE) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(book,
                    new EnchantmentInstance(AdvancedEnchantments.TRUE_DAMAGE.get(), TrueDamageEnchantment.MAX_LEVEL));
            return trueDamageOffer(offer, book, quality, settings);
        }
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
