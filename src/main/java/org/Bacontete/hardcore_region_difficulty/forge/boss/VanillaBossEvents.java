package org.Bacontete.hardcore_region_difficulty.forge.boss;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;

/**
 * Makes the Ender Dragon, the Wither and the Warden configurable (health and damage). Their warning title uses the boss icon
 * because both are in the hardcore_region_difficulty:boss entity tag.
 */
public class VanillaBossEvents {

    /** Vanilla cap of the max health attribute. */
    private static final double ATTRIBUTE_MAX_HEALTH = 1024.0D;

    private static double effectiveHealth(Entity entity) {
        if (entity.getType() == EntityType.ENDER_DRAGON) {
            return ForgeConfig.DRAGON_HEALTH.get();
        }
        if (entity.getType() == EntityType.WITHER) {
            return ForgeConfig.WITHER_HEALTH.get();
        }
        if (entity.getType() == EntityType.WARDEN) {
            return ForgeConfig.WARDEN_HEALTH.get();
        }
        return 0.0D;
    }

    private static double damageMultiplier(Entity entity) {
        if (entity.getType() == EntityType.ENDER_DRAGON) {
            return ForgeConfig.DRAGON_DAMAGE_MULTIPLIER.get();
        }
        if (entity.getType() == EntityType.WITHER) {
            return ForgeConfig.WITHER_DAMAGE_MULTIPLIER.get();
        }
        if (entity.getType() == EntityType.WARDEN) {
            return ForgeConfig.WARDEN_DAMAGE_MULTIPLIER.get();
        }
        return 1.0D;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !ForgeConfig.isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        double effective = effectiveHealth(living);
        if (effective <= 0.0D || living.getHealth() <= 0.0F) {
            return;
        }
        AttributeInstance attribute = living.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double target = Math.min(effective, ATTRIBUTE_MAX_HEALTH);
        double old = attribute.getBaseValue();
        if (Math.abs(old - target) < 0.5D) {
            return;
        }
        // keep the health ratio (a freshly spawned dragon becomes full health, a wither keeps its spawn charge)
        double ratio = old > 0.0D ? Math.min(1.0D, living.getHealth() / old) : 1.0D;
        attribute.setBaseValue(target);
        living.setHealth((float) (ratio * target));
    }

    /** Duration of the Wither spawn charge (vanilla). */
    private static final float WITHER_SPAWN_CHARGE_TICKS = 220.0F;

    /**
     * Vanilla heals the Wither by 10 HP every 10 ticks while it charges (22 times = 220 HP), enough to go from 1/3 to full
     * health with the vanilla 300 HP but not with a bigger health pool. The health therefore follows the charge here: it
     * is at least 1/3 at the start and exactly full when the charge ends.
     */
    @SubscribeEvent
    public void onWitherTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide()
                || !ForgeConfig.isEnabled()) {
            return;
        }
        int invulnerable = wither.getInvulnerableTicks();
        if (invulnerable <= 0 || wither.getHealth() <= 0.0F) {
            return;
        }
        float progress = Math.min(1.0F, Math.max(0.0F, 1.0F - invulnerable / WITHER_SPAWN_CHARGE_TICKS));
        float target = wither.getMaxHealth() * (1.0F / 3.0F + 2.0F / 3.0F * progress);
        if (invulnerable <= 1) {
            target = wither.getMaxHealth();
        }
        if (wither.getHealth() < target) {
            wither.setHealth(target);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!ForgeConfig.isEnabled()) {
            return;
        }
        float amount = event.getAmount();

        // damage taken: above the attribute cap the boss takes less damage instead
        LivingEntity victim = event.getEntity();
        double effective = effectiveHealth(victim);
        if (effective > ATTRIBUTE_MAX_HEALTH) {
            amount *= (float) (ATTRIBUTE_MAX_HEALTH / effective);
        }

        // damage dealt (the owner of a wither skull counts as the attacker)
        Entity attacker = event.getSource().getEntity();
        if (attacker != null) {
            double mult = damageMultiplier(attacker);
            if (mult != 1.0D) {
                amount *= (float) mult;
            }
        }
        event.setAmount(amount);
    }
}
