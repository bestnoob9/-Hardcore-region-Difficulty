package org.Bacontete.hardcore_region_difficulty.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;

/**
 * What a mob of a given type gets at a given level, from the formulas of the config (same as EntityScaler,
 * without the per-mob whitelist details). Used by the GUI preview and /hrd preview.
 */
public final class LevelPreview {

    /** Base value, value at the level, and the speed bonus (fraction) of one mob. A base < 0 means "mob has no such attribute". */
    public record Stats(double baseHealth, double health, double baseDamage, double damage,
                        double baseArmor, double armor, double speedBonus) {}

    private LevelPreview() {}

    /** The living entity type with this id, or null if it does not exist or has no attributes. */
    @SuppressWarnings("unchecked")
    public static EntityType<? extends LivingEntity> typeOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id.trim());
        if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
        try {
            return DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) type)
                    ? (EntityType<? extends LivingEntity>) type : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Stats of a mob at this level; boss = null for a normal mob. Returns null if the type is unknown. */
    public static Stats compute(String entityId, int level, BossLevel boss, ScalingSettings scaling) {
        EntityType<? extends LivingEntity> type = typeOf(entityId);
        if (type == null) return null;
        AttributeSupplier base = DefaultAttributes.getSupplier(type);
        double baseHealth = has(base, Attributes.MAX_HEALTH) ? base.getBaseValue(Attributes.MAX_HEALTH) : -1;
        double baseDamage = has(base, Attributes.ATTACK_DAMAGE) ? base.getBaseValue(Attributes.ATTACK_DAMAGE) : -1;
        double baseArmor = has(base, Attributes.ARMOR) ? base.getBaseValue(Attributes.ARMOR) : 0;

        int lvl = Math.max(1, level);
        double health = scaling.healthAmount(lvl);
        double armorMul = scaling.armorMultiplyAmount(lvl);
        double damage = scaling.damageAmount(lvl);
        double armorFlat = scaling.armorFlatAmount(lvl);
        double speed = scaling.speedAmount(lvl);
        if (boss == BossLevel.BOSS) {
            health += scaling.getBossHealthPercent() / 100.0;
            armorMul += scaling.getBossArmorPercent() / 100.0;
            damage += scaling.getBossDamagePercent() / 100.0;
            health = ScalingSettings.multiplyBonus(health, scaling.getBossHealthMultiplier());
            armorMul = ScalingSettings.multiplyBonus(armorMul, scaling.getBossArmorMultiplier());
            armorFlat *= scaling.getBossArmorMultiplier();
            damage = ScalingSettings.multiplyBonus(damage, scaling.getBossDamageMultiplier());
        } else if (boss == BossLevel.MINI_BOSS) {
            health += scaling.getMiniBossHealthPercent() / 100.0;
            armorMul += scaling.getMiniBossArmorPercent() / 100.0;
            damage += scaling.getMiniBossDamagePercent() / 100.0;
            health = ScalingSettings.multiplyBonus(health, scaling.getMiniBossHealthMultiplier());
            armorMul = ScalingSettings.multiplyBonus(armorMul, scaling.getMiniBossArmorMultiplier());
            armorFlat *= scaling.getMiniBossArmorMultiplier();
            damage = ScalingSettings.multiplyBonus(damage, scaling.getMiniBossDamageMultiplier());
        }
        return new Stats(baseHealth, baseHealth < 0 ? -1 : baseHealth * (1.0 + health),
                baseDamage, baseDamage < 0 ? -1 : baseDamage * (1.0 + damage),
                baseArmor, baseArmor * (1.0 + armorMul) + armorFlat, speed);
    }

    private static boolean has(AttributeSupplier supplier, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return supplier.hasAttribute(attribute);
    }

    /** One readable line, for chat and tooltips. */
    public static String line(String entityId, Stats s) {
        StringBuilder sb = new StringBuilder(entityId.startsWith("minecraft:") ? entityId.substring(10) : entityId).append(": ");
        sb.append("HP ").append(pair(s.baseHealth(), s.health()));
        sb.append(" | DMG ").append(pair(s.baseDamage(), s.damage()));
        sb.append(" | ARM ").append(pair(s.baseArmor(), s.armor()));
        sb.append(" | SPD +").append(Math.round(s.speedBonus() * 1000) / 10.0).append('%');
        return sb.toString();
    }

    private static String pair(double base, double now) {
        if (base < 0) return "-";
        return fmt(base) + " > " + fmt(now) + " (x" + fmt(base <= 0 ? 1 : now / base) + ")";
    }

    private static String fmt(double v) {
        return Math.abs(v - Math.rint(v)) < 0.05 ? String.valueOf((long) Math.rint(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
