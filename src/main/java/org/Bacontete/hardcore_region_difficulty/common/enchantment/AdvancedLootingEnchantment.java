package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class AdvancedLootingEnchantment extends AdvancedEnchantmentBase {

    public AdvancedLootingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != Enchantments.MOB_LOOTING && super.checkCompatibility(other);
    }
}
