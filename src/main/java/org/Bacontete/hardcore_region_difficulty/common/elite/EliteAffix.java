package org.Bacontete.hardcore_region_difficulty.common.elite;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A random power of an elite mob. Each affix has an id (saved in the mob NBT), a display name (translation key),
 * a color, a particle and its effect: a passive part ({@link #applyPassive}: attribute modifiers or potion effects,
 * applied once when the mob becomes elite and saved by vanilla) and/or an on-hit part ({@link #onHit}).
 */
public enum EliteAffix {

    /** Burns its targets, immune to fire. */
    FIRE("fire", ChatFormatting.GOLD, ParticleTypes.FLAME) {
        @Override
        protected void applyPassive(Mob mob) {
            addEffect(mob, MobEffects.FIRE_RESISTANCE, 0);
        }

        @Override
        protected void onHit(Mob attacker, LivingEntity victim, float damage) {
            victim.setSecondsOnFire(5);
        }
    },

    /** Sometimes calls a lightning bolt on its target (short cooldown), immune to lightning. */
    LIGHTNING("lightning", ChatFormatting.YELLOW, ParticleTypes.ELECTRIC_SPARK) {
        @Override
        protected void applyPassive(Mob mob) {
            // a bolt next to the mob would also set it on fire
            addEffect(mob, MobEffects.FIRE_RESISTANCE, 0);
        }

        @Override
        protected void onHit(Mob attacker, LivingEntity victim, float damage) {
            if (!(victim.level() instanceof ServerLevel level)) {
                return;
            }
            long now = level.getGameTime();
            long last = attacker.getPersistentData().getLong(LIGHTNING_COOLDOWN_TAG);
            if (now - last < LIGHTNING_COOLDOWN_TICKS || attacker.getRandom().nextFloat() >= LIGHTNING_CHANCE) {
                return;
            }
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                return;
            }
            attacker.getPersistentData().putLong(LIGHTNING_COOLDOWN_TAG, now);
            bolt.moveTo(victim.getX(), victim.getY(), victim.getZ());
            bolt.setDamage(LIGHTNING_DAMAGE);
            level.addFreshEntity(bolt);
            HardcoreRegionDifficulty.LOGGER.debug("Elite lightning: {} struck {}",
                    attacker.getType().getDescription().getString(), victim.getName().getString());
        }
    },

    /** Poisons its targets. */
    POISON("poison", ChatFormatting.DARK_GREEN, ParticleTypes.ITEM_SLIME) {
        @Override
        protected void onHit(Mob attacker, LivingEntity victim, float damage) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), attacker);
        }
    },

    /** Heals itself over time (permanent Regeneration III, no icon / particles of its own). */
    REGENERATION("regeneration", ChatFormatting.LIGHT_PURPLE, ParticleTypes.HAPPY_VILLAGER) {
        @Override
        protected void applyPassive(Mob mob) {
            addEffect(mob, MobEffects.REGENERATION, 2);
        }
    },

    /** Extra armor and knockback resistance. */
    ARMORED("armored", ChatFormatting.GRAY, ParticleTypes.CRIT) {
        @Override
        protected void applyPassive(Mob mob) {
            addModifier(mob, Attributes.ARMOR, 6.0D, AttributeModifier.Operation.ADDITION);
            addModifier(mob, Attributes.KNOCKBACK_RESISTANCE, 0.4D, AttributeModifier.Operation.ADDITION);
        }
    },

    /** Faster movement. */
    SWIFT("swift", ChatFormatting.AQUA, ParticleTypes.CLOUD) {
        @Override
        protected void applyPassive(Mob mob) {
            addModifier(mob, Attributes.MOVEMENT_SPEED, 0.3D, AttributeModifier.Operation.MULTIPLY_BASE);
        }
    },

    /** More maximum health. */
    TOUGH("tough", ChatFormatting.DARK_RED, ParticleTypes.ENCHANTED_HIT) {
        @Override
        protected void applyPassive(Mob mob) {
            addModifier(mob, Attributes.MAX_HEALTH, 0.6D, AttributeModifier.Operation.MULTIPLY_BASE);
        }
    },

    /** Heals itself with part of the damage it deals. */
    VAMPIRIC("vampiric", ChatFormatting.RED, ParticleTypes.CRIMSON_SPORE) {
        @Override
        protected void onHit(Mob attacker, LivingEntity victim, float damage) {
            attacker.heal(damage * VAMPIRIC_HEAL_FRACTION);
        }
    };

    private static final String LIGHTNING_COOLDOWN_TAG = HardcoreRegionDifficulty.MODID + ":elite_lightning_time";
    private static final int LIGHTNING_COOLDOWN_TICKS = 100;
    private static final float LIGHTNING_CHANCE = 0.25F;
    private static final float LIGHTNING_DAMAGE = 4.0F;
    private static final float VAMPIRIC_HEAL_FRACTION = 0.35F;

    private final String id;
    private final ChatFormatting color;
    private final SimpleParticleType particle;
    private final String translationKey;
    private final UUID modifierId;

    EliteAffix(String id, ChatFormatting color, SimpleParticleType particle) {
        this.id = id;
        this.color = color;
        this.particle = particle;
        this.translationKey = HardcoreRegionDifficulty.MODID + ".elite.affix." + id;
        // one fixed id per affix, so the modifier is never added twice and can be recognized
        this.modifierId = UUID.nameUUIDFromBytes((HardcoreRegionDifficulty.MODID + ":elite:" + id).getBytes(StandardCharsets.UTF_8));
    }

    /** Id saved in the mob NBT and used in logs. */
    public String id() { return id; }

    public ChatFormatting color() { return color; }

    public SimpleParticleType particle() { return particle; }

    /** Translation key of the display name (lang file). */
    public String translationKey() { return translationKey; }

    /** Attribute modifiers / potion effects given once when the mob becomes elite. Nothing by default. */
    protected void applyPassive(Mob mob) {
    }

    /** Called after this elite dealt damage to a living entity (server side). Nothing by default. */
    protected void onHit(Mob attacker, LivingEntity victim, float damage) {
    }

    /** Applies the passive part (package-private entry point for {@link EliteManager}). */
    void applyTo(Mob mob) {
        applyPassive(mob);
    }

    /** Runs the on-hit part (package-private entry point for {@link EliteManager}). */
    void hit(Mob attacker, LivingEntity victim, float damage) {
        onHit(attacker, victim, damage);
    }

    /** Affix with this id, or null when unknown (e.g. removed in a later version). */
    public static EliteAffix byId(String id) {
        for (EliteAffix affix : values()) {
            if (affix.id.equals(id)) {
                return affix;
            }
        }
        return null;
    }

    /** Permanent, invisible effect (infinite duration): saved with the mob, no particles and no icon. */
    private static void addEffect(Mob mob, MobEffect effect, int amplifier) {
        mob.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, false));
    }

    protected void addModifier(Mob mob, Attribute attribute, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || instance.getModifier(modifierId) != null) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(modifierId,
                HardcoreRegionDifficulty.MODID + ":elite_" + id, amount, op));
    }
}
