package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;

public class AdvancedProtectionEnchantment extends AdvancedEnchantmentBase {

    public AdvancedProtectionEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public int getDamageProtection(int level, DamageSource source) {
        if (!isActive()) {
            return 0;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return 0;
        }
        return level * 2;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {

        return !(other instanceof ProtectionEnchantment) && super.checkCompatibility(other);
    }
}
