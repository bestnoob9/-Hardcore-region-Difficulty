package org.Bacontete.hardcore_region_difficulty.common.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

public class BossDetector {

    public static final double MINI_BOSS_HEALTH_THRESHOLD = 200.0;

    public static final double BOSS_HEALTH_THRESHOLD = 400.0;

    public static final TagKey<net.minecraft.world.entity.EntityType<?>> MINI_BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(HardcoreRegionDifficulty.MODID, "mini_boss"));

    public static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(HardcoreRegionDifficulty.MODID, "boss"));

    public static final TagKey<net.minecraft.world.entity.EntityType<?>> FORGE_BOSSES_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses"));

    public static boolean canDealDamage(Mob mob) {
        if (!(mob instanceof Enemy) || !mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            return false;
        }
        return mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) > 0.0;
    }

    public static boolean isMiniBoss(Mob mob) {
        if (mob == null || !canDealDamage(mob)) return false;
        double baseHealth = mob.getAttributeBaseValue(Attributes.MAX_HEALTH);
        return baseHealth >= MINI_BOSS_HEALTH_THRESHOLD && baseHealth < BOSS_HEALTH_THRESHOLD;
    }

    public static boolean isBoss(Mob mob) {
        if (mob == null || !canDealDamage(mob)) return false;
        double baseHealth = mob.getAttributeBaseValue(Attributes.MAX_HEALTH);
        return baseHealth >= BOSS_HEALTH_THRESHOLD;
    }

    public static int getBossDifficultyLevel(Mob mob) {
        if (mob == null || EntityScaler.isExcluded(mob)) return 0;   

        if (mob.getType().is(MINI_BOSS_TAG)) return 6;
        if (mob.getType().is(BOSS_TAG)) return 7;

        if (isBoss(mob)) return 7;
        if (isMiniBoss(mob)) return 6;

        if (mob.getType().is(FORGE_BOSSES_TAG)) return 6;
        return 0;
    }

    public static boolean hasBossTag(Mob mob) {
        if (mob == null) return false;
        return mob.getType().is(BOSS_TAG) || mob.getType().is(MINI_BOSS_TAG);
    }
}
