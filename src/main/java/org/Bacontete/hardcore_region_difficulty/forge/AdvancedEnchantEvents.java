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

    private static boolean isModEnchant(net.minecraft.world.item.enchantment.Enchantment enchantment) {
        var key = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return key != null && key.getNamespace().equals(
                org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty.MODID);
    }

    private static boolean isBlockedEnchant(net.minecraft.world.item.enchantment.Enchantment enchantment,
                                            java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> leftEnchants) {
        var key = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return key != null
                && key.getNamespace().equals(org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty.MODID)
                && !AdvancedEnchantments.isEnabled(enchantment)
                && leftEnchants.get(enchantment) == null;
    }

    /**
     * A switched-off enchantment of this mod can no longer be applied (anvil), only kept on items that have it.
     * Only that enchantment is skipped: the other enchantments of the same book still get applied
     * (the event used to be cancelled, which blocked the whole anvil result).
     * Also: True Damage replaces Armor Piercing (any level) already on the item, and every enchant of this mod
     * (Advanced Sharpness, Advanced Protection...) replaces the normal enchants it is not compatible with.
     * In these special cases the result is built here, mirroring vanilla (AnvilMenu): material repair,
     * durability merge of two identical items, enchant merge, rename and XP cost.
     */
    @SubscribeEvent
    public void onAnvilDisabledEnchant(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        var leftEnchants = EnchantmentHelper.getEnchantments(left);
        var rightEnchants = EnchantmentHelper.getEnchantments(right);
        boolean anyBlocked = false;
        for (var enchantment : rightEnchants.keySet()) {
            if (isBlockedEnchant(enchantment, leftEnchants)) {
                anyBlocked = true;
                break;
            }
        }
        // True Damage pushes Armor Piercing (any level) off the item instead of being refused.
        boolean replacesPiercing = AdvancedEnchantments.TRUE_DAMAGE.isPresent()
                && AdvancedEnchantments.ARMOR_PIERCING.isPresent()
                && rightEnchants.containsKey(AdvancedEnchantments.TRUE_DAMAGE.get())
                && leftEnchants.containsKey(AdvancedEnchantments.ARMOR_PIERCING.get())
                && AdvancedEnchantments.isEnabled(AdvancedEnchantments.TRUE_DAMAGE);
        // An enchant of this mod (Advanced Sharpness, Advanced Protection...) pushes the normal enchants it is not
        // compatible with (Sharpness / Smite / Bane, Protection...) off the item instead of being refused.
        boolean pushesOutNormal = false;
        for (var enchantment : rightEnchants.keySet()) {
            if (isModEnchant(enchantment) && AdvancedEnchantments.isEnabled(enchantment)) {
                for (var other : leftEnchants.keySet()) {
                    if (other != enchantment && !isModEnchant(other) && !enchantment.isCompatibleWith(other)) {
                        pushesOutNormal = true;
                    }
                }
            }
        }
        if (!anyBlocked && !replacesPiercing && !pushesOutNormal) {
            return;
        }
        if (left.isEmpty() || right.isEmpty()) {
            event.setCanceled(true);
            return;
        }
        boolean rightIsBook = right.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)
                && !net.minecraft.world.item.EnchantedBookItem.getEnchantments(right).isEmpty();
        boolean leftIsBook = left.is(net.minecraft.world.item.Items.ENCHANTED_BOOK);

        ItemStack output = left.copy();
        var result = new java.util.LinkedHashMap<>(leftEnchants);
        long baseCost = (long) left.getBaseRepairCost() + right.getBaseRepairCost();
        int extra = 0;
        int renameCost = 0;
        int materialCost = 0;

        if (output.isDamageableItem() && output.getItem().isValidRepairItem(left, right)) {
            // Repair with the material (e.g. iron ingots on an iron sword): 25% of max durability per unit.
            int step = Math.min(output.getDamageValue(), output.getMaxDamage() / 4);
            if (step <= 0) {
                event.setCanceled(true);
                return;
            }
            int used;
            for (used = 0; step > 0 && used < right.getCount(); used++) {
                output.setDamageValue(output.getDamageValue() - step);
                extra++;
                step = Math.min(output.getDamageValue(), output.getMaxDamage() / 4);
            }
            materialCost = used;
        } else {
            if (!rightIsBook && (!output.is(right.getItem()) || !output.isDamageableItem())) {
                event.setCanceled(true);
                return;
            }
            if (output.isDamageableItem() && !rightIsBook) {
                // Merge the durability of two identical items (+12% bonus).
                int leftLeft = left.getMaxDamage() - left.getDamageValue();
                int rightLeft = right.getMaxDamage() - right.getDamageValue();
                int bonus = rightLeft + output.getMaxDamage() * 12 / 100;
                int newDamage = Math.max(0, output.getMaxDamage() - (leftLeft + bonus));
                if (newDamage < output.getDamageValue()) {
                    output.setDamageValue(newDamage);
                    extra += 2;
                }
            }
            boolean applied = false;
            boolean failed = false;
            for (var entry : rightEnchants.entrySet()) {
                var enchantment = entry.getKey();
                if (isBlockedEnchant(enchantment, leftEnchants)) {
                    failed = true;
                    continue;
                }
                int current = result.getOrDefault(enchantment, 0);
                int level = entry.getValue();
                level = current == level ? level + 1 : Math.max(level, current);
                level = Math.min(level, enchantment.getMaxLevel());
                boolean canApply = leftIsBook || enchantment.canEnchant(left);
                if (canApply && replacesPiercing && enchantment == AdvancedEnchantments.TRUE_DAMAGE.get()) {
                    result.remove(AdvancedEnchantments.ARMOR_PIERCING.get());
                }
                if (canApply && isModEnchant(enchantment) && AdvancedEnchantments.isEnabled(enchantment)) {
                    result.keySet().removeIf(other -> other != enchantment && !isModEnchant(other)
                            && !enchantment.isCompatibleWith(other));
                }
                for (var other : result.keySet()) {
                    if (other != enchantment && !enchantment.isCompatibleWith(other)) {
                        canApply = false;
                        extra++;
                    }
                }
                if (!canApply) {
                    failed = true;
                    continue;
                }
                int multiplier = switch (enchantment.getRarity()) {
                    case COMMON -> 1;
                    case UNCOMMON -> 2;
                    case RARE -> 4;
                    case VERY_RARE -> 8;
                };
                if (rightIsBook) {
                    multiplier = Math.max(1, multiplier / 2);
                }
                result.put(enchantment, level);
                extra += multiplier * level;
                applied = true;
            }
            if (failed && !applied) {
                event.setCanceled(true);
                return;
            }
        }

        // Rename, like vanilla.
        String name = event.getName();
        if (name == null || name.isBlank()) {
            if (left.hasCustomHoverName()) {
                renameCost = 1;
                extra += 1;
                output.resetHoverName();
            }
        } else if (!name.equals(left.getHoverName().getString())) {
            renameCost = 1;
            extra += 1;
            output.setHoverName(net.minecraft.network.chat.Component.literal(name));
        }

        if (leftIsBook == false && rightIsBook && !output.isBookEnchantable(right)) {
            event.setCanceled(true);
            return;
        }
        if (extra <= 0) {
            event.setCanceled(true);
            return;
        }
        long cost = baseCost + extra;
        if (renameCost == extra && renameCost > 0 && cost >= 40) {
            cost = 39;
        }
        boolean creative = event.getPlayer() != null && event.getPlayer().getAbilities().instabuild;
        if (cost >= 40 && !creative) {
            event.setCanceled(true);
            return;
        }
        int repairCost = Math.max(output.getBaseRepairCost(), right.getBaseRepairCost());
        if (renameCost != extra || renameCost == 0) {
            repairCost = repairCost * 2 + 1;
        }
        output.setRepairCost(repairCost);
        EnchantmentHelper.setEnchantments(result, output);
        event.setOutput(output);
        event.setCost((int) cost);
        event.setMaterialCost(materialCost > 0 ? materialCost : 1);
    }

    // NOTE: the old onAnvilUpdate handler cancelled the whole anvil operation whenever the right-hand item
    // carried Swift Strike and the left-hand item could not take it (any sword, any enchanted book).
    // Vanilla already skips an enchantment that cannot be applied and still applies the others, so no handler is needed.

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
