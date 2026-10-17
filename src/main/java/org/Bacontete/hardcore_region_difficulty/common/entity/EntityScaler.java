package org.Bacontete.hardcore_region_difficulty.common.entity;

import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.raid.Raider;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PatternMatcher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class EntityScaler {

    private static final String SCALED_TAG = HardcoreRegionDifficulty.MODID + ":scaled";
    private static final String LEVEL_TAG = HardcoreRegionDifficulty.MODID + ":level";
    private static final String RAID_TAG = HardcoreRegionDifficulty.MODID + ":raid_scaled";

    private static final String HEAL_TAG = HardcoreRegionDifficulty.MODID + ":heal_to_max";

    private static final int HEAL_WINDOW_TICKS = 200;

    private static final UUID HEALTH_ID = modifierId("health");
    private static final UUID ARMOR_MUL_ID = modifierId("armor_multiply");
    private static final UUID ARMOR_ADD_ID = modifierId("armor_add");
    private static final UUID DAMAGE_ID = modifierId("damage");
    private static final UUID SPEED_ID = modifierId("speed");

    private static UUID modifierId(String name) {
        return UUID.nameUUIDFromBytes((HardcoreRegionDifficulty.MODID + ":" + name).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fixed raid difficulty for this mob, or 0 when it is not a raid member (or the feature is disabled).
     * Every vanilla and modded raid mob is a {@link Raider} (Raid.WaveMember only accepts Raider types),
     * so this also covers mobs added by other mods that are spawned by a raid.
     */
    public static int getRaidLevel(Mob mob) {
        if (!(mob instanceof Raider raider) || raider.getCurrentRaid() == null) {
            return 0;
        }
        DifficultyConfig config = DifficultyConfig.getInstance();
        ScalingSettings scaling = config == null ? null : config.getScalingSettings();
        return scaling == null ? 0 : scaling.getRaidDifficultyLevel();
    }

    public static void applyScaling(Mob mob) {
        if (mob.level().isClientSide) {
            return;
        }
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || config.getScalingSettings() == null) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(SCALED_TAG)) {
            return;
        }

        int raidLevel = getRaidLevel(mob);
        int regionLevel = raidLevel > 0 ? raidLevel : LocationMatcher.getDifficultyLevel(mob.level(), mob.blockPosition());
        if (raidLevel > 0) {
            data.putBoolean(RAID_TAG, true);
        }
        int bossLevel = BossDetector.getBossDifficultyLevel(mob);
        int difficultyLevel = bossLevel > 0 ? bossLevel : regionLevel;
        if (bossLevel > 0) {
            data.putBoolean("is_boss_entity", true);
            HardcoreRegionDifficulty.LOGGER.debug("Boss detected: {} [{}] level {} (base HP {}, base attack damage {}, hostile {}, boss tags: {})",
                    mob.getType().getDescription().getString(),
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()), bossLevel,
                    mob.getAttributeBaseValue(Attributes.MAX_HEALTH),
                    mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                            ? mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) : "none",
                    mob instanceof net.minecraft.world.entity.monster.Enemy,
                    BossDetector.hasBossTag(mob) || mob.getType().is(BossDetector.FORGE_BOSSES_TAG));
        }

        data.putBoolean(SCALED_TAG, true);
        data.putInt(LEVEL_TAG, difficultyLevel);

        if (regionLevel <= 1 && bossLevel == 0) {
            return;
        }

        float oldMax = mob.getMaxHealth();
        float relativeHealth = oldMax > 0 ? mob.getHealth() / oldMax : 1.0F;
        applyModifiers(mob, config.getScalingSettings(), regionLevel, bossLevel);

        mob.setHealth(relativeHealth * mob.getMaxHealth());
        if (mob.getMaxHealth() > oldMax + 0.01F) {
            data.putFloat(HEAL_TAG, oldMax);
        }

        HardcoreRegionDifficulty.LOGGER.debug("Scaled {} at difficulty level {}{}",
                mob.getType().getDescription().getString(), difficultyLevel, raidLevel > 0 ? " (raid)" : "");
    }

    /**
     * An already existing raider can join a raid later (Raider.aiStep -> Raid.joinRaid(existing = true)),
     * after it was scaled by its region. Re-scale it once to the fixed raid level.
     */
    public static void checkRaidJoin(Mob mob) {
        if (mob.level().isClientSide) {
            return;
        }
        int raidLevel = getRaidLevel(mob);
        if (raidLevel <= 0) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(RAID_TAG)) {
            return;
        }
        if (!shouldScale(mob)) {
            return;
        }
        data.putBoolean(RAID_TAG, true);

        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config == null || config.getScalingSettings() == null) {
            return;
        }
        int bossLevel = BossDetector.getBossDifficultyLevel(mob);
        if (bossLevel == 0 && data.contains(LEVEL_TAG) && data.getInt(LEVEL_TAG) == raidLevel) {
            return;
        }

        float max = mob.getMaxHealth();
        float relativeHealth = max > 0 ? mob.getHealth() / max : 1.0F;
        removeModifiers(mob);
        data.putBoolean(SCALED_TAG, true);
        data.putInt(LEVEL_TAG, bossLevel > 0 ? bossLevel : raidLevel);
        data.remove(HEAL_TAG);
        if (raidLevel > 1 || bossLevel > 0) {
            applyModifiers(mob, config.getScalingSettings(), raidLevel, bossLevel);
        }
        mob.setHealth(Math.min(relativeHealth, 1.0F) * mob.getMaxHealth());

        HardcoreRegionDifficulty.LOGGER.debug("Re-scaled {} to raid difficulty level {}",
                mob.getType().getDescription().getString(), raidLevel);
    }

    private static void applyModifiers(Mob mob, ScalingSettings scaling, int regionLevel, int bossLevel) {
        double health = scaling.healthAmount(regionLevel);
        double armorMul = scaling.armorMultiplyAmount(regionLevel);
        double damage = scaling.damageAmount(regionLevel);
        if (bossLevel == 7) {
            health += scaling.getBossHealthPercent() / 100.0;
            armorMul += scaling.getBossArmorPercent() / 100.0;
            damage += scaling.getBossDamagePercent() / 100.0;
        } else if (bossLevel == 6) {
            health += scaling.getMiniBossHealthPercent() / 100.0;
            armorMul += scaling.getMiniBossArmorPercent() / 100.0;
            damage += scaling.getMiniBossDamagePercent() / 100.0;
        }
        add(mob, Attributes.MAX_HEALTH, HEALTH_ID, "health", health, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.ARMOR, ARMOR_MUL_ID, "armor_multiply", armorMul, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.ARMOR, ARMOR_ADD_ID, "armor_add", scaling.armorFlatAmount(regionLevel), AttributeModifier.Operation.ADDITION);
        add(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "damage", damage, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, "speed", scaling.speedAmount(regionLevel), AttributeModifier.Operation.MULTIPLY_BASE);
    }

    private static void removeModifiers(Mob mob) {
        remove(mob, Attributes.MAX_HEALTH, HEALTH_ID);
        remove(mob, Attributes.ARMOR, ARMOR_MUL_ID);
        remove(mob, Attributes.ARMOR, ARMOR_ADD_ID);
        remove(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        remove(mob, Attributes.MOVEMENT_SPEED, SPEED_ID);
    }

    private static void remove(Mob mob, Attribute attribute, UUID id) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }

    public static void fixInitialHealth(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(HEAL_TAG)) {
            return;
        }
        if (mob.tickCount > HEAL_WINDOW_TICKS) {
            data.remove(HEAL_TAG);
            return;
        }
        float preMax = data.getFloat(HEAL_TAG);
        float max = mob.getMaxHealth();
        if (Math.abs(mob.getHealth() - preMax) < 0.01F && max > preMax + 0.01F) {
            mob.setHealth(max);
            data.remove(HEAL_TAG);
            HardcoreRegionDifficulty.LOGGER.debug("Restored full health of {} ({} -> {})",
                    mob.getType().getDescription().getString(), preMax, max);
        }
    }

    private static void add(Mob mob, Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {
        if (amount == 0.0D) {
            return;
        }
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(id, HardcoreRegionDifficulty.MODID + ":" + name, amount, op));
    }

    public static boolean isExcluded(Mob mob) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        ScalingSettings scaling = config == null ? null : config.getScalingSettings();
        if (scaling == null || scaling.getExcludePatterns().isEmpty()) {
            return false;
        }
        EntityType<?> type = mob.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return PatternMatcher.matches(scaling.getExcludePatterns(), id,
                tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag)));
    }

    public static boolean shouldScale(Mob mob) {
        if (!ForgeConfig.isEnabled()) {
            return false;
        }
        EntityType<?> type = mob.getType();

        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.WARDEN) {
            return false;
        }
        int bossLevel = BossDetector.getBossDifficultyLevel(mob);

        if (!(mob instanceof Enemy) && bossLevel == 0) {
            return false;
        }

        DifficultyConfig config = DifficultyConfig.getInstance();
        ScalingSettings scaling = config == null ? null : config.getScalingSettings();
        if (scaling == null) {
            return true;
        }

        if (isExcluded(mob)) {
            return false;
        }

        if (bossLevel > 0) {
            return true;
        }

        double threshold = scaling.getBossHealthThreshold();
        if (threshold > 0 && mob.getAttributeBaseValue(Attributes.MAX_HEALTH) >= threshold) {
            return false;
        }
        return true;
    }
}
