package org.Bacontete.hardcore_region_difficulty.forge.elite;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteAffix;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteManager;

/** Forge events of the elite mobs: on-hit affixes, lightning immunity and the particles. */
public class EliteEvents {

    /** How often (ticks) the particles of an elite are shown. */
    private static final int PARTICLE_INTERVAL = 20;

    /** An elite loaded from a chunk counts again for its level (new elites are counted when they are rolled). */
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && EliteManager.isElite(mob)) {
            EliteManager.track(mob);
        }
    }

    /** Dead, despawned or unloaded elites free their place (elite_max_per_level). */
    @SubscribeEvent
    public void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && EliteManager.isElite(mob)) {
            EliteManager.untrack(mob);
        }
    }

    /** Same as leaving, but at the moment of death (the mob stays in the world a second longer). */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Mob mob && EliteManager.isElite(mob)) {
            EliteManager.untrack(mob);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        EliteManager.clearTracked();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        EliteManager.clearTracked();
    }

    /** After an elite dealt damage (damage already reduced by armor): fire, poison, lightning, vampiric... */
    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof Mob attacker && EliteManager.isElite(attacker)) {
            EliteManager.onHit(attacker, victim, event.getAmount());
        }
    }

    /** An elite with the lightning affix takes no damage from lightning (its own bolts, or any other). */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide
                && event.getSource().is(DamageTypes.LIGHTNING_BOLT)
                && EliteManager.hasAffix(mob, EliteAffix.LIGHTNING)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide
                && mob.tickCount % PARTICLE_INTERVAL == 0 && EliteManager.isElite(mob)) {
            EliteManager.showParticles(mob);
        }
    }
}
