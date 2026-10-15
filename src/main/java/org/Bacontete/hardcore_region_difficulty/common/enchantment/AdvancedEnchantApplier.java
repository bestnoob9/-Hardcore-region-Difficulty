package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.EnchantmentSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AdvancedEnchantApplier {
    private AdvancedEnchantApplier() {}

    public static int rollLevel(int max, double decay, RandomSource random) {
        if (max <= 1) return 1;
        if (decay >= 1.0) return 1 + random.nextInt(max);
        double total = 0;
        double weight = 1;
        for (int k = 1; k <= max; k++) {
            total += weight;
            weight *= decay;
        }
        double r = random.nextDouble() * total;
        weight = 1;
        for (int k = 1; k <= max; k++) {
            if (r < weight) return k;
            r -= weight;
            weight *= decay;
        }
        return max;
    }

    public static boolean rollChance(int difficultyLevel, RandomSource random) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || config.getEnchantmentSettings() == null) {
            return false;
        }
        return random.nextDouble() < config.getEnchantmentSettings().getBaseChance(difficultyLevel);
    }

    public static ItemStack createBook(int difficultyLevel, RandomSource random) {
        return createBook(difficultyLevel, random, Integer.MAX_VALUE);
    }

    public static ItemStack createBook(int difficultyLevel, RandomSource random, int levelCap) {
        return createBook(difficultyLevel, random, levelCap, 1.0);
    }

    public static ItemStack createBook(int difficultyLevel, RandomSource random, int levelCap, double levelDecay) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        return apply(book, difficultyLevel, random, levelCap, levelDecay) ? book : ItemStack.EMPTY;
    }

    public static ItemStack createBook(int difficultyLevel, RandomSource random, int levelCap, double levelDecay,
                                       List<Enchantment> pool) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        return apply(book, difficultyLevel, random, levelCap, levelDecay, pool) ? book : ItemStack.EMPTY;
    }

    public static int totalLevel(ItemStack book) {
        int total = 0;
        for (int lvl : EnchantmentHelper.getEnchantments(book).values()) {
            total += lvl;
        }
        return total;
    }

    public static boolean apply(ItemStack stack, int difficultyLevel, RandomSource random) {
        return apply(stack, difficultyLevel, random, Integer.MAX_VALUE);
    }

    public static boolean apply(ItemStack stack, int difficultyLevel, RandomSource random, int levelCap) {
        return apply(stack, difficultyLevel, random, levelCap, 1.0);
    }

    public static boolean apply(ItemStack stack, int difficultyLevel, RandomSource random, int levelCap,
                                double levelDecay) {
        return apply(stack, difficultyLevel, random, levelCap, levelDecay, AdvancedEnchantments.lootPool());
    }

    public static boolean apply(ItemStack stack, int difficultyLevel, RandomSource random, int levelCap,
                                double levelDecay, List<Enchantment> pool) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || stack.isEmpty()) {
            return false;
        }
        EnchantmentSettings settings = config.getEnchantmentSettings();
        boolean book = stack.is(Items.ENCHANTED_BOOK);
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);

        List<Enchantment> candidates = new ArrayList<>();
        for (Enchantment e : pool) {
            if (existing.containsKey(e)) continue;
            if (!book) {
                if (!e.canEnchant(stack)) continue;
                boolean conflict = false;
                for (Enchantment other : existing.keySet()) {
                    if (!e.isCompatibleWith(other)) {
                        conflict = true;
                        break;
                    }
                }
                if (conflict) continue;
            }
            candidates.add(e);
        }
        if (candidates.isEmpty()) {
            return false;
        }

        Enchantment enchant = candidates.get(random.nextInt(candidates.size()));

        int maxLevel = Math.max(1, Math.min(Math.min(settings.getMaxLevel(difficultyLevel), enchant.getMaxLevel()),
                Math.max(1, levelCap)));
        int level = rollLevel(maxLevel, levelDecay, random);

        if (book) {
            EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchant, level));
        } else {
            stack.enchant(enchant, level);
        }
        HardcoreRegionDifficulty.LOGGER.debug("Added advanced enchant {} level {} at difficulty {}",
                enchant.getDescriptionId(), level, difficultyLevel);
        return true;
    }
}
