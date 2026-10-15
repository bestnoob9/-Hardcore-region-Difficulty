package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class AdvancedFireAspectEnchantment extends AdvancedEnchantmentBase {

    public AdvancedFireAspectEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public void doPostAttack(LivingEntity attacker, Entity target, int level) {
        if (!attacker.level().isClientSide && !target.fireImmune()) {
            target.setSecondsOnFire(level * 6);
        }
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != Enchantments.FIRE_ASPECT && super.checkCompatibility(other);
    }
}
