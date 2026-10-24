package org.Bacontete.hardcore_region_difficulty.common.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.LootSettings;
import org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * More and better loot in regions of a higher level (see LootSettings): extra rolls of the chest's loot table,
 * bigger stacks, a chance to enchant gear, and (mobs) extra copies of a drop. The quality of the dropped gear
 * follows the quality chances of the level and the Luck of the killer.
 */
public final class RegionLootBonus {

    private RegionLootBonus() {}

    /** The loot settings that apply at a place: the global ones, changed by the "loot" of the region found there. */
    private static LootSettings settingsAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null) return null;
        LootSettings global = config.getScalingSettings().getLootSettings();
        try {
            org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher.Match match =
                    org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher.match(level, pos);
            if (match != null && match.region() != null && match.region().getLootOverride() != null) {
                return global.overriddenBy(match.region().getLootOverride());
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.warn("Could not read the loot of the region at {}", pos, e);
        }
        return global;
    }

    /** Chest loot: called by the global loot modifier before the enchant / quality steps. */
    public static void applyToChest(ObjectArrayList<ItemStack> loot, LootContext context, ResourceLocation tableId, int level) {
        net.minecraft.world.phys.Vec3 origin = context.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
        LootSettings s = origin == null ? null : settingsAt(context.getLevel(), net.minecraft.core.BlockPos.containing(origin));
        if (s == null || !s.appliesToChests(level)) return;
        RandomSource random = context.getRandom();

        // more rolls of the same table: more items and a better chance of the rare ones
        double rollChance = LootSettings.chance(s.getExtraRollChancePerLevel(), level);
        if (s.getMaxExtraRolls() > 0 && rollChance > 0 && tableId != null) {
            try {
                LootTable table = context.getLevel().getServer().getLootData().getLootTable(tableId);
                if (table != LootTable.EMPTY) {
                    for (int i = 0; i < s.getMaxExtraRolls(); i++) {
                        if (random.nextDouble() < rollChance) {
                            table.getRandomItemsRaw(context, stack -> addSplit(loot, stack));
                        }
                    }
                }
            } catch (Exception e) {
                HardcoreRegionDifficulty.LOGGER.warn("Extra loot rolls failed for {}", tableId, e);
            }
        }

        double countMul = LootSettings.countMultiplier(s.getCountPercentPerLevel(), level);
        double enchantChance = LootSettings.chance(s.getEnchantChancePerLevel(), level);
        for (ItemStack stack : loot) {
            scaleCount(stack, countMul, random);
            maybeEnchant(stack, enchantChance, level, random);
        }
    }

    /** Drops of a mob killed at this region level. Items are changed in place, extra copies are added. */
    public static void applyToMobDrops(Collection<ItemEntity> drops, net.minecraft.world.entity.LivingEntity mob, int level, float luck) {
        LootSettings s = settingsAt(mob.level(), mob.blockPosition());
        if (s == null || !s.appliesToMobs(level) || drops.isEmpty()) return;
        RandomSource random = mob.getRandom();

        double countMul = LootSettings.countMultiplier(s.getMobCountPercentPerLevel(), level);
        double enchantChance = LootSettings.chance(s.getMobEnchantChancePerLevel(), level);
        List<ItemEntity> current = new ArrayList<>(drops);
        for (ItemEntity drop : current) {
            ItemStack stack = drop.getItem();
            scaleCount(stack, countMul, random);
            maybeEnchant(stack, enchantChance, level, random);
            rollQuality(stack, level, random, luck);
            drop.setItem(stack);
        }

        double extraChance = LootSettings.chance(s.getMobExtraDropChancePerLevel(), level);
        if (extraChance <= 0 || s.getMobMaxExtraDrops() <= 0) return;
        for (int i = 0; i < s.getMobMaxExtraDrops(); i++) {
            if (random.nextDouble() >= extraChance) continue;
            ItemEntity source = current.get(random.nextInt(current.size()));
            ItemStack copy = source.getItem().copy();
            if (ItemQuality.isEligible(copy)) {
                // a copy of gear must not be an exact clone: new quality roll
                if (copy.getTag() != null) copy.getTag().remove(ItemQuality.TAG);
                rollQuality(copy, level, random, luck);
            }
            drops.add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), copy));
        }
    }

    private static void rollQuality(ItemStack stack, int level, RandomSource random, float luck) {
        if (QualityConfig.isEnabled() && QualityConfig.ROLL_IN_LOOT.get()
                && ItemQuality.isEligible(stack) && !ItemQuality.has(stack)) {
            ItemQuality.set(stack, ItemQuality.roll(random, level, stack, luck));
        }
    }

    private static void scaleCount(ItemStack stack, double multiplier, RandomSource random) {
        if (multiplier <= 1.0D || stack.isEmpty() || stack.getMaxStackSize() <= 1) return;
        double scaled = stack.getCount() * multiplier;
        int whole = (int) Math.floor(scaled);
        if (random.nextDouble() < scaled - whole) whole++;
        stack.setCount(Math.max(stack.getCount(), Math.min(stack.getMaxStackSize(), whole)));
    }

    private static void maybeEnchant(ItemStack stack, double chance, int level, RandomSource random) {
        if (chance <= 0 || stack.isEmpty() || stack.getMaxStackSize() != 1 || !stack.isDamageableItem()
                || stack.isEnchanted() || !stack.isEnchantable()) return;
        if (random.nextDouble() < chance) {
            EnchantmentHelper.enchantItem(random, stack, Math.min(30, 5 + level * 5), false);
        }
    }

    /** Adds a stack, splitting it if a table gave more than the stack size. */
    private static void addSplit(List<ItemStack> list, ItemStack stack) {
        if (stack.isEmpty()) return;
        int max = stack.getMaxStackSize();
        if (stack.getCount() <= max) {
            list.add(stack);
            return;
        }
        int left = stack.getCount();
        while (left > 0) {
            ItemStack part = stack.copy();
            part.setCount(Math.min(max, left));
            left -= part.getCount();
            list.add(part);
        }
    }
}
