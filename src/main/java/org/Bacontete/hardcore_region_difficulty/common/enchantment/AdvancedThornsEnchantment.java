package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class AdvancedThornsEnchantment extends AdvancedEnchantmentBase {

    public AdvancedThornsEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public void doPostHurt(LivingEntity user, Entity attacker, int level) {
        if (!isActive()) {
            return;
        }
        if (attacker == null || user.level().isClientSide) {
            return;
        }
        if (user.getRandom().nextFloat() < Math.min(1.0F, 0.3F + 0.2F * level)) {
            attacker.hurt(user.damageSources().thorns(user), level * 2.0F);
        }
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return other != Enchantments.THORNS && super.checkCompatibility(other);
    }
}
