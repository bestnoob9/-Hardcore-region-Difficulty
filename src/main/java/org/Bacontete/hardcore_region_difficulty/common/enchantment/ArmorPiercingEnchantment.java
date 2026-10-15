package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ArmorPiercingEnchantment extends Enchantment {

    public static final int MAX_LEVEL = 3;

    public ArmorPiercingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    public static float ignoredFraction(int level) {
        if (level <= 0) return 0.0F;
        return Math.min(level, MAX_LEVEL) * 0.1F;
    }

    private static boolean fitsItem(ItemStack stack) {
        return stack.getItem() instanceof AxeItem || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return fitsItem(stack) || super.canEnchant(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return fitsItem(stack) || super.canApplyAtEnchantingTable(stack);
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return !(other instanceof TrueDamageEnchantment) && super.checkCompatibility(other);
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }
}
