package org.Bacontete.hardcore_region_difficulty.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.ArmorPiercingEnchantment;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.TrueDamageEnchantment;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.Bacontete.hardcore_region_difficulty.common.enchantment.AdvancedEnchantments;

public class AdvancedEnchantEvents {

    private static final String LAST_DAMAGE_TAG = "HrdLastDamage";

    private static final String ARROW_PIERCING_TAG = "HrdArmorPiercing";
    private static final String ARROW_TRUE_DAMAGE_TAG = "HrdTrueDamage";
    private static final UUID SWIFT_STRIKE_ID = UUID.nameUUIDFromBytes(
            "hardcore_region_difficulty:swift_strike".getBytes(StandardCharsets.UTF_8));

    private record PendingTrueHit(UUID target, float normal, float desired) {}
    private static PendingTrueHit pendingTrueHit;

    private static float damageAfterDefenses(float raw, float armor, float toughness, float resistanceMultiplier,
                                             int protection) {
        float amount = CombatRules.getDamageAfterAbsorb(raw, armor, toughness) * resistanceMultiplier;
        if (amount > 0.0F && protection > 0) {
            amount = CombatRules.getDamageAfterMagicAbsorb(amount, protection);
        }
        return Math.max(amount, 0.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurtTrueHit(LivingHurtEvent event) {
        pendingTrueHit = null;
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        int piercing;
        int trueDamage;
        if (source.getDirectEntity() == attacker) {

            ItemStack weapon = attacker.getMainHandItem();
            piercing = AdvancedEnchantments.levelOf(weapon, AdvancedEnchantments.ARMOR_PIERCING);
            trueDamage = AdvancedEnchantments.levelOf(weapon, AdvancedEnchantments.TRUE_DAMAGE);
        } else if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() == attacker) {

            piercing = arrow.getPersistentData().getInt(ARROW_PIERCING_TAG);
            trueDamage = arrow.getPersistentData().getInt(ARROW_TRUE_DAMAGE_TAG);
        } else {
            return;
        }
        if (piercing <= 0 && trueDamage <= 0) {
            return;
        }
        float trueFraction = TrueDamageEnchantment.ignoredFraction(trueDamage);
        float armorIgnored = source.is(DamageTypeTags.BYPASSES_ARMOR) ? 0.0F
                : Math.max(ArmorPiercingEnchantment.ignoredFraction(piercing), trueFraction);

        LivingEntity target = event.getEntity();
        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        float resistance = 1.0F;
        int protection = 0;
        if (!source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            MobEffectInstance effect = target.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (effect != null && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                resistance = Math.max(25 - (effect.getAmplifier() + 1) * 5, 0) / 25.0F;
            }
            protection = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), source);
        }
        float resistanceIgnoredTo = resistance + (1.0F - resistance) * trueFraction;

        float raw = event.getAmount();
        float normal = damageAfterDefenses(raw, armor, toughness, resistance, protection);
        float keep = 1.0F - armorIgnored;
        int protectionKept = Math.round(protection * (1.0F - trueFraction));
        float desired = damageAfterDefenses(raw, armor * keep, toughness * keep, resistanceIgnoredTo, protectionKept);
        if (desired > normal + 0.0001F) {
            pendingTrueHit = new PendingTrueHit(target.getUUID(), normal, desired);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDamageTrueHit(LivingDamageEvent event) {
        PendingTrueHit pending = pendingTrueHit;
        pendingTrueHit = null;
        if (pending == null || !pending.target().equals(event.getEntity().getUUID())) {
            return;
        }
        if (pending.normal() > 0.0F) {

            event.setAmount(event.getAmount() * (pending.desired() / pending.normal()));
        } else {

            event.setAmount(pending.desired());
        }
    }

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!AdvancedEnchantments.SWIFT_STRIKE.isPresent()) {
            return;
        }
        var swift = AdvancedEnchantments.SWIFT_STRIKE.get();
        if (EnchantmentHelper.getEnchantments(event.getRight()).containsKey(swift)
                && AdvancedEnchantments.levelOf(event.getLeft(), AdvancedEnchantments.SWIFT_STRIKE) == 0
                && !swift.canEnchant(event.getLeft())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        int level = AdvancedEnchantments.levelOf(event.getItemStack(), AdvancedEnchantments.SWIFT_STRIKE);
        if (level > 0) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(SWIFT_STRIKE_ID,
                    "hardcore_region_difficulty:swift_strike", org.Bacontete.hardcore_region_difficulty.common.enchantment.SwiftStrikeEnchantment.speedBonus(level), AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getNewSpeed() <= 1.0F) {
            return;
        }
        int level = AdvancedEnchantments.levelOf(event.getEntity().getMainHandItem(), AdvancedEnchantments.ADVANCED_EFFICIENCY);
        if (level > 0) {
            event.setNewSpeed(event.getNewSpeed() + level * level * 2 + 2);
        }
    }

    @SubscribeEvent
    public void onLootingLevel(LootingLevelEvent event) {
        DamageSource source = event.getDamageSource();
        if (source != null && source.getEntity() instanceof LivingEntity killer) {
            int level = AdvancedEnchantments.levelOf(killer.getMainHandItem(), AdvancedEnchantments.ADVANCED_LOOTING);
            if (level > 0) {
                event.setLootingLevel(event.getLootingLevel() + level * 2);
            }
        }
    }

    @SubscribeEvent
    public void onKnockBack(LivingKnockBackEvent event) {
        if (Math.abs(event.getStrength() - 0.4F) > 0.001F) {
            return;
        }
        LivingEntity attacker = event.getEntity().getLastHurtByMob();
        if (attacker == null) {
            return;
        }
        int level = AdvancedEnchantments.levelOf(attacker.getMainHandItem(), AdvancedEnchantments.ADVANCED_KNOCKBACK);
        if (level > 0) {
            event.setStrength(event.getStrength() + level * 0.5F);
        }
    }

    private static boolean isArrowLauncher(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    @SubscribeEvent
    public void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity shooter) {
            ItemStack bow = shooter.getUseItem();
            if (!isArrowLauncher(bow)) {
                bow = shooter.getMainHandItem();
            }
            if (!isArrowLauncher(bow)) {
                bow = shooter.getOffhandItem();
            }
            int piercingLevel = AdvancedEnchantments.levelOf(bow, AdvancedEnchantments.ARMOR_PIERCING);
            if (piercingLevel > 0) {
                arrow.getPersistentData().putInt(ARROW_PIERCING_TAG, piercingLevel);
            }
            int trueDamageLevel = AdvancedEnchantments.levelOf(bow, AdvancedEnchantments.TRUE_DAMAGE);
            if (trueDamageLevel > 0) {
                arrow.getPersistentData().putInt(ARROW_TRUE_DAMAGE_TAG, trueDamageLevel);
            }
            int level = AdvancedEnchantments.levelOf(bow, AdvancedEnchantments.ADVANCED_POWER);
            if (level > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + level * 1.0D);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        Player player = event.player;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            int level = AdvancedEnchantments.levelOf(stack, AdvancedEnchantments.ADVANCED_UNBREAKING);
            if (level <= 0) {
                continue;
            }
            CompoundTag tag = stack.getOrCreateTag();
            int damage = stack.getDamageValue();
            if (tag.contains(LAST_DAMAGE_TAG)) {
                int last = tag.getInt(LAST_DAMAGE_TAG);
                if (damage > last) {
                    double keepChance = 1.0D / (1 + 2 * level);
                    int kept = 0;
                    for (int i = 0; i < damage - last; i++) {
                        if (player.getRandom().nextDouble() < keepChance) {
                            kept++;
                        }
                    }
                    damage = last + kept;
                    stack.setDamageValue(damage);
                }
            }
            tag.putInt(LAST_DAMAGE_TAG, damage);
        }
    }
}
