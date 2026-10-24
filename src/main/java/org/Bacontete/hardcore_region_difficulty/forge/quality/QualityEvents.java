package org.Bacontete.hardcore_region_difficulty.forge.quality;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityType;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class QualityEvents {

    private static final UUID DAMAGE_ID = UUID.nameUUIDFromBytes(
            "hardcore_region_difficulty:quality_damage".getBytes(StandardCharsets.UTF_8));
    private static final UUID REACH_ID = UUID.nameUUIDFromBytes(
            "hardcore_region_difficulty:quality_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPEED_ID = UUID.nameUUIDFromBytes(
            "hardcore_region_difficulty:quality_speed".getBytes(StandardCharsets.UTF_8));

    /** Unique, stable modifier id per purpose and equipment slot (the same id twice on one attribute is an error). */
    private static UUID slotId(String purpose, EquipmentSlot slot) {
        return UUID.nameUUIDFromBytes(("hardcore_region_difficulty:" + purpose + ":" + slot.getName())
                .getBytes(StandardCharsets.UTF_8));
    }

    private static final int INVENTORY_CHECK_INTERVAL = 10;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    private static final String LAST_DAMAGE_TAG = "HrdQualityLastDamage";
    private static final String WEAR_CARRY_TAG = "HrdQualityWearCarry";
    private static final String DRAW_CARRY_TAG = "HrdQualityDrawCarry";

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        if (!QualityConfig.isEnabled()) {
            return;
        }
        Player player = event.player;
        boolean wholeInventory = player.tickCount % INVENTORY_CHECK_INTERVAL == 0;
        checkStack(player, player.getMainHandItem(), EquipmentSlot.MAINHAND);
        checkStack(player, player.getOffhandItem(), EquipmentSlot.OFFHAND);

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack armor = player.getInventory().armor.get(i);
            if (!armor.isEmpty() && ItemQuality.has(armor)) {
                applyWear(player, armor, ARMOR_SLOTS[i]);
            }
        }
        if (wholeInventory) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack != player.getMainHandItem()) {
                    checkStack(player, stack, null);
                }
            }
        }
    }

    private static void checkStack(Player player, ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || !ItemQuality.isEligible(stack)) {
            return;
        }
        if (!ItemQuality.has(stack)) {
            if (QualityConfig.ROLL_IN_INVENTORY.get()) {
                // crafted / inventory items: same tier-based chances as the Quality Forge (no weights, no region level)
                ItemQuality.set(stack, ItemQuality.rollForge(player.getRandom(), stack, player.getLuck()));
            }
            return;
        }
        applyWear(player, stack, slot);
    }

    private static void applyWear(Player player, ItemStack stack, EquipmentSlot slot) {
        QualityType quality = ItemQuality.get(stack);
        if (!stack.isDamageableItem()) {
            return;
        }
        double durabilityPercent = quality.getDurabilityPercent();
        if (durabilityPercent == 0.0) {
            return;
        }
        var tag = stack.getOrCreateTag();
        int damage = stack.getDamageValue();
        int last = tag.contains(LAST_DAMAGE_TAG, 99) ? tag.getInt(LAST_DAMAGE_TAG) : damage;
        if (damage > last) {

            double factor = Math.max(0.1D, 1.0D + durabilityPercent);
            double adjust = (damage - last) * (1.0D / factor - 1.0D) + tag.getFloat(WEAR_CARRY_TAG);
            int whole = (int) adjust; 
            tag.putFloat(WEAR_CARRY_TAG, (float) (adjust - whole));
            damage = Math.max(last, damage + whole);
            if (damage >= stack.getMaxDamage()) {
                stack.shrink(1);
                if (slot != null) {
                    player.broadcastBreakEvent(slot);
                }
                return;
            }
            stack.setDamageValue(damage);
        }
        if (!tag.contains(LAST_DAMAGE_TAG, 99) || tag.getInt(LAST_DAMAGE_TAG) != damage) {
            tag.putInt(LAST_DAMAGE_TAG, damage);
        }
    }

    @SubscribeEvent
    public void onItemAttributes(ItemAttributeModifierEvent event) {
        if (!QualityConfig.isEnabled()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        QualityType quality = ItemQuality.find(stack);
        if (quality == null) {
            return;
        }

        if (ItemQuality.isArmor(stack)) {
            ArmorItem armorItem = (ArmorItem) stack.getItem();
            EquipmentSlot armorSlot = armorItem.getEquipmentSlot();
            if (event.getSlotType() == armorSlot) {
                // the % only counts the armor / toughness OF THIS PIECE alone. MULTIPLY_BASE is not used because it multiplies
                // the total armor of the whole set (every worn piece), so the % becomes a flat amount based on the base stat of the piece.
                double armorPercent = quality.getDamagePercent();
                if (armorPercent != 0) {
                    double armorDelta = armorItem.getDefense() * armorPercent;
                    if (armorDelta != 0) {
                        event.addModifier(Attributes.ARMOR, new AttributeModifier(slotId("armor_pct", armorSlot),
                                "hardcore_region_difficulty:armor_quality", armorDelta,
                                AttributeModifier.Operation.ADDITION));
                    }
                    double toughnessDelta = armorItem.getToughness() * armorPercent;
                    if (toughnessDelta != 0) {
                        event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(slotId("toughness_pct", armorSlot),
                                "hardcore_region_difficulty:toughness_quality", toughnessDelta,
                                AttributeModifier.Operation.ADDITION));
                    }
                }
                double armorFlat = quality.getArmorBonus();
                if (armorFlat != 0) {
                    event.addModifier(Attributes.ARMOR, new AttributeModifier(slotId("armor_flat", armorSlot),
                            "hardcore_region_difficulty:armor_bonus", armorFlat,
                            AttributeModifier.Operation.ADDITION));
                }
            }
            return;
        }

        if (ItemQuality.isShield(stack)) {
            EquipmentSlot hand = event.getSlotType();
            if (hand == EquipmentSlot.OFFHAND || hand == EquipmentSlot.MAINHAND) {
                double armorFlat = quality.getArmorBonus();
                if (armorFlat != 0) {
                    event.addModifier(Attributes.ARMOR, new AttributeModifier(slotId("shield_armor", hand),
                            "hardcore_region_difficulty:shield_armor", armorFlat,
                            AttributeModifier.Operation.ADDITION));
                }
                double move = quality.getMoveSpeedPercent();
                if (move != 0) {
                    event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(slotId("shield_move", hand),
                            "hardcore_region_difficulty:shield_move", move,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            return;
        }

        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        double reach = quality.getReach();
        if (reach != 0) {
            if (ItemQuality.isWeapon(stack)) {
                event.addModifier(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_ID,
                        "hardcore_region_difficulty:quality_entity_reach", reach,
                        AttributeModifier.Operation.ADDITION));
            }
            if (ItemQuality.isTool(stack)) {
                event.addModifier(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(REACH_ID,
                        "hardcore_region_difficulty:quality_block_reach", reach,
                        AttributeModifier.Operation.ADDITION));
            }
        }
        if (!ItemQuality.isWeapon(stack)) {
            return;
        }
        double damageBonus = quality.getDamagePercent();
        if (damageBonus != 0) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_ID,
                    "hardcore_region_difficulty:quality_damage", damageBonus,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        double speedBonus = quality.getSpeedPercent();
        if (speedBonus != 0) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_ID,
                    "hardcore_region_difficulty:quality_speed", speedBonus,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static final String ARROW_DONE_TAG = "HrdQualityArrow";

    @SubscribeEvent
    public void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof LivingEntity shooter)
                || arrow.getPersistentData().getBoolean(ARROW_DONE_TAG)) {
            return;
        }

        if (!(shooter instanceof net.minecraft.world.entity.player.Player)) {

            int difficultyLevel = org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher.getDifficultyLevel(event.getLevel(), shooter.blockPosition());
            if (difficultyLevel > 1) {

                double factor = 1.0 + (difficultyLevel - 1) * 0.1;
                arrow.setBaseDamage(arrow.getBaseDamage() * factor);
            }
            arrow.getPersistentData().putBoolean(ARROW_DONE_TAG, true);
            return;
        }

        if (!QualityConfig.isEnabled()) {
            arrow.getPersistentData().putBoolean(ARROW_DONE_TAG, true);
            return;
        }
        ItemStack launcher = shooter.getUseItem();
        if (!ItemQuality.isRanged(launcher)) {
            launcher = shooter.getMainHandItem();
        }
        if (!ItemQuality.isRanged(launcher)) {
            launcher = shooter.getOffhandItem();
        }
        arrow.getPersistentData().putBoolean(ARROW_DONE_TAG, true);
        if (!ItemQuality.isRanged(launcher) || !ItemQuality.has(launcher)) {
            return;
        }
        QualityType quality = ItemQuality.get(launcher);
        double damagePercent = quality.getDamagePercent();
        if (damagePercent != 0) {
            double factor = Math.max(0.1D, 1.0D + damagePercent);
            arrow.setBaseDamage(arrow.getBaseDamage() * factor);
        }
    }

    @SubscribeEvent
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (QualityConfig.isEnabled() && ItemQuality.isRanged(event.getItem())) {
            event.getEntity().getPersistentData().putFloat(DRAW_CARRY_TAG, 0.0F);
        }
    }

    @SubscribeEvent
    public void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!QualityConfig.isEnabled()) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!ItemQuality.isRanged(stack) || !ItemQuality.has(stack)) {
            return;
        }
        QualityType quality = ItemQuality.get(stack);
        double speedPercent = quality.getSpeedPercent();
        if (speedPercent == 0) {
            return;
        }
        var data = event.getEntity().getPersistentData();
        double speed = Math.max(0.1D, 1.0D + speedPercent);
        double extra = speed - 1.0D + data.getFloat(DRAW_CARRY_TAG);
        int whole = (int) extra;
        data.putFloat(DRAW_CARRY_TAG, (float) (extra - whole));
        if (whole != 0) {
            event.setDuration(event.getDuration() - whole);
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!QualityConfig.isEnabled()) {
            return;
        }
        ItemStack stack = event.getEntity().getMainHandItem();
        ItemStack off = event.getEntity().getOffhandItem();
        if (ItemQuality.isShield(off) && ItemQuality.has(off)) {
            double held = ItemQuality.get(off).getHoldMiningPercent();
            if (held != 0) {
                event.setNewSpeed(event.getNewSpeed() * (float) Math.max(0.1D, 1.0D + held));
            }
        }
        if (!ItemQuality.has(stack) || !ItemQuality.isTool(stack)) {
            return;
        }
        QualityType quality = ItemQuality.get(stack);
        double miningPercent = quality.getMiningPercent();
        if (miningPercent != 0) {
            float factor = (float) Math.max(0.1D, 1.0D + miningPercent);
            event.setNewSpeed(event.getNewSpeed() * factor);
        }
    }
}
