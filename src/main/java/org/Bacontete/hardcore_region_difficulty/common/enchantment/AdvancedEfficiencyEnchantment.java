package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class AdvancedEfficiencyEnchantment extends AdvancedEnchantmentBase {

    public AdvancedEfficiencyEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != Enchantments.BLOCK_EFFICIENCY && super.checkCompatibility(other);
    }
}
