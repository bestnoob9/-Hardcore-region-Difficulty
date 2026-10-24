package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class AdvancedFeatherFallingEnchantment extends AdvancedEnchantmentBase {

    public AdvancedFeatherFallingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getDamageProtection(int level, DamageSource source) {
        if (!isActive()) {
            return 0;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return 0;
        }
        return source.is(DamageTypeTags.IS_FALL) ? level * 5 : 0;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != Enchantments.FALL_PROTECTION && super.checkCompatibility(other);
    }
}
