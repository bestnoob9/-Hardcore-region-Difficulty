package org.Bacontete.hardcore_region_difficulty.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class SwiftStrikeEnchantment extends Enchantment {

    public static final int MAX_LEVEL = 5;

    public static final double SPEED_PER_LEVEL = 0.1D;

    public static final double LAST_LEVEL_STEP = 0.1D;

    public static double speedBonus(int level) {
        if (level <= 0) return 0.0D;
        int lower = Math.min(level, MAX_LEVEL - 1);
        double bonus = lower * SPEED_PER_LEVEL;
        if (level >= MAX_LEVEL) bonus += LAST_LEVEL_STEP;
        return bonus;
    }

    public static final double MAX_ITEM_ATTACK_SPEED = 1.1D;

    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

    public SwiftStrikeEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] slots) {
        super(rarity, category, slots);
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 6;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    public static double itemAttackSpeed(ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);
        if (!modifiers.containsKey(Attributes.ATTACK_SPEED)) {
            return -1.0D;
        }
        double speed = PLAYER_BASE_ATTACK_SPEED;
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_SPEED)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                speed += modifier.getAmount();
            }
        }
        return speed;
    }

    public static boolean isSlowEnough(ItemStack stack) {
        double speed = itemAttackSpeed(stack);
        return speed > 0.0D && speed <= MAX_ITEM_ATTACK_SPEED + 0.0001D;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return isSlowEnough(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return isSlowEnough(stack);
    }
}
