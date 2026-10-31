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
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyRegion;
import org.Bacontete.hardcore_region_difficulty.common.config.MobEntry;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.LocationMatcher;
import org.Bacontete.hardcore_region_difficulty.common.difficulty.PatternMatcher;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class EntityScaler {

    private static final String SCALED_TAG = HardcoreRegionDifficulty.MODID + ":scaled";
    private static final String LEVEL_TAG = HardcoreRegionDifficulty.MODID + ":level";
    private static final String BOSS_LEVEL_TAG = HardcoreRegionDifficulty.MODID + ":boss_level";
    private static final String RAID_TAG = HardcoreRegionDifficulty.MODID + ":raid_scaled";

    private static final String HEAL_TAG = HardcoreRegionDifficulty.MODID + ":heal_to_max";

    private static final int HEAL_WINDOW_TICKS = 200;

    private static final UUID HEALTH_ID = modifierId("health");
    private static final UUID ARMOR_MUL_ID = modifierId("armor_multiply");
    private static final UUID ARMOR_ADD_ID = modifierId("armor_add");
    private static final UUID DAMAGE_ID = modifierId("damage");
    private static final UUID SPEED_ID = modifierId("speed");
    private static final UUID KNOCKBACK_ID = modifierId("knockback_resistance");
    private static final UUID FOLLOW_ID = modifierId("follow_range");

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
        int regionLevel = raidLevel;
        DifficultyRegion region = null;
        if (raidLevel <= 0) {
            LocationMatcher.Match match = LocationMatcher.matchCheap(mob.level(), mob.blockPosition());
            region = match != null ? match.region() : null;
            regionLevel = match != null ? match.region().getDifficultyLevel() : 1;
            if (match != null && !match.region().getEntityWhitelist().isEmpty()
                    && !matchesPatterns(mob, match.region().getEntityWhitelist())) {
                // the region has a mob whitelist and this mob is not on it: not scaled here
                data.putBoolean(SCALED_TAG, true);
                data.putInt(LEVEL_TAG, 1);
                return;
            }
        }
        // per-mob details of the whitelist (region entry first, then the global one)
        MobEntry entry = findEntry(mob, region, config.getScalingSettings());
        if (entry != null && entry.getLevel() != null) {
            regionLevel = entry.getLevel();
        }
        if (raidLevel > 0) {
            data.putBoolean(RAID_TAG, true);
        }
        BossLevel bossLevel = BossDetector.getBossLevel(mob);
        if (bossLevel != null) {
            data.putBoolean("is_boss_entity", true);
            data.putString(BOSS_LEVEL_TAG, bossLevel.name());
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
        data.putInt(LEVEL_TAG, regionLevel);

        if (regionLevel <= 1 && bossLevel == null && !config.getScalingSettings().hasCustom(regionLevel)
                && (entry == null || !entry.hasStatOverride())) {
            // not scaled, but a low elite_min_level can still make it elite
            tryElite(mob, regionLevel, bossLevel, config.getScalingSettings(), region);
            return;
        }

        float oldMax = mob.getMaxHealth();
        float relativeHealth = oldMax > 0 ? mob.getHealth() / oldMax : 1.0F;
        applyModifiers(mob, config.getScalingSettings(), regionLevel, bossLevel, entry);

        mob.setHealth(relativeHealth * mob.getMaxHealth());
        if (mob.getMaxHealth() > oldMax + 0.01F) {
            data.putFloat(HEAL_TAG, oldMax);
        }
        // elite roll comes after the scaling, so the affixes work on top of the scaled stats
        tryElite(mob, regionLevel, bossLevel, config.getScalingSettings(), region);

        HardcoreRegionDifficulty.LOGGER.debug("Scaled {} at difficulty level {}{}",
                mob.getType().getDescription().getString(), regionLevel, raidLevel > 0 ? " (raid)" : "");
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
        BossLevel bossLevel = BossDetector.getBossLevel(mob);
        MobEntry entry = findEntry(mob, null, config.getScalingSettings());
        if (entry != null && entry.getLevel() != null) {
            raidLevel = entry.getLevel();
        }
        if (bossLevel == null && data.contains(LEVEL_TAG) && data.getInt(LEVEL_TAG) == raidLevel) {
            return;
        }

        float max = mob.getMaxHealth();
        float relativeHealth = max > 0 ? mob.getHealth() / max : 1.0F;
        removeModifiers(mob);
        data.putBoolean(SCALED_TAG, true);
        data.putInt(LEVEL_TAG, raidLevel);
        if (bossLevel != null) {
            data.putString(BOSS_LEVEL_TAG, bossLevel.name());
        }
        data.remove(HEAL_TAG);
        if (raidLevel > 1 || bossLevel != null || config.getScalingSettings().hasCustom(raidLevel)
                || (entry != null && entry.hasStatOverride())) {
            applyModifiers(mob, config.getScalingSettings(), raidLevel, bossLevel, entry);
        }
        mob.setHealth(Math.min(relativeHealth, 1.0F) * mob.getMaxHealth());

        HardcoreRegionDifficulty.LOGGER.debug("Re-scaled {} to raid difficulty level {}",
                mob.getType().getDescription().getString(), raidLevel);
    }

    private static double percent(Double value) {
        return Math.max(-0.9, value / 100.0);
    }

    private static void applyModifiers(Mob mob, ScalingSettings scaling, int regionLevel, BossLevel bossLevel, MobEntry entry) {
        // Soft cap: the *Amount() methods already use the effective level instead of the raw level,
        // so this only logs it, to make debugging easier when a region level goes over the soft cap.
        if (scaling.isSoftCapActive(regionLevel)) {
            HardcoreRegionDifficulty.LOGGER.debug("Soft cap applied to {}: level {} -> effective level {} (soft cap {}, diminishing factor {})",
                    mob.getType().getDescription().getString(), regionLevel,
                    String.format(java.util.Locale.ROOT, "%.2f", scaling.effectiveLevel(regionLevel)),
                    scaling.getSoftCapLevel(), scaling.getDiminishingFactor());
        }
        // the details of a mob replace the values of its level
        double health = entry != null && entry.getHealthBonusPercent() != null
                ? percent(entry.getHealthBonusPercent()) : scaling.healthAmount(regionLevel);
        double armorMul = entry != null && entry.getArmorBonusPercent() != null
                ? percent(entry.getArmorBonusPercent()) : scaling.armorMultiplyAmount(regionLevel);
        double damage = entry != null && entry.getDamageBonusPercent() != null
                ? percent(entry.getDamageBonusPercent()) : scaling.damageAmount(regionLevel);
        double armorFlat = entry != null && entry.getArmorFlatBonus() != null
                ? entry.getArmorFlatBonus() : scaling.armorFlatAmount(regionLevel);
        double speed = entry != null && entry.getSpeedBonusPercent() != null
                ? percent(entry.getSpeedBonusPercent()) : scaling.speedAmount(regionLevel);
        if (bossLevel == BossLevel.BOSS) {
            health += scaling.getBossHealthPercent() / 100.0;
            armorMul += scaling.getBossArmorPercent() / 100.0;
            damage += scaling.getBossDamagePercent() / 100.0;
            // Boss: extra multiplier on the whole bonus (after the boss % bonus was added)
            health = ScalingSettings.multiplyBonus(health, scaling.getBossHealthMultiplier());
            armorMul = ScalingSettings.multiplyBonus(armorMul, scaling.getBossArmorMultiplier());
            armorFlat *= scaling.getBossArmorMultiplier();
            damage = ScalingSettings.multiplyBonus(damage, scaling.getBossDamageMultiplier());
            logBossMultiplier(mob, "BOSS", scaling.getBossHealthMultiplier(), scaling.getBossDamageMultiplier(), scaling.getBossArmorMultiplier());
        } else if (bossLevel == BossLevel.MINI_BOSS) {
            health += scaling.getMiniBossHealthPercent() / 100.0;
            armorMul += scaling.getMiniBossArmorPercent() / 100.0;
            damage += scaling.getMiniBossDamagePercent() / 100.0;
            // Mini-Boss: its own multiplier, independent from Boss
            health = ScalingSettings.multiplyBonus(health, scaling.getMiniBossHealthMultiplier());
            armorMul = ScalingSettings.multiplyBonus(armorMul, scaling.getMiniBossArmorMultiplier());
            armorFlat *= scaling.getMiniBossArmorMultiplier();
            damage = ScalingSettings.multiplyBonus(damage, scaling.getMiniBossDamageMultiplier());
            logBossMultiplier(mob, "MINI_BOSS", scaling.getMiniBossHealthMultiplier(), scaling.getMiniBossDamageMultiplier(), scaling.getMiniBossArmorMultiplier());
        }
        add(mob, Attributes.MAX_HEALTH, HEALTH_ID, "health", health, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.ARMOR, ARMOR_MUL_ID, "armor_multiply", armorMul, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.ARMOR, ARMOR_ADD_ID, "armor_add", armorFlat, AttributeModifier.Operation.ADDITION);
        add(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "damage", damage, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, "speed", speed, AttributeModifier.Operation.MULTIPLY_BASE);
        if (entry != null) {
            if (entry.getKnockbackResistance() != null) {
                add(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "knockback_resistance",
                        entry.getKnockbackResistance(), AttributeModifier.Operation.ADDITION);
            }
            if (entry.getFollowRangeBonusPercent() != null) {
                add(mob, Attributes.FOLLOW_RANGE, FOLLOW_ID, "follow_range",
                        percent(entry.getFollowRangeBonusPercent()), AttributeModifier.Operation.MULTIPLY_BASE);
            }
        }
    }

    /** Rolls the elite affixes of a mob (once, together with the scaling). Bosses are never elite. */
    private static void tryElite(Mob mob, int level, BossLevel bossLevel, ScalingSettings scaling,
                                 DifficultyRegion region) {
        if (bossLevel == null) {
            EliteManager.tryRoll(mob, level, scaling, region != null ? region.getEliteEnabled() : null);
        }
    }

    /** Debug log when a boss multiplier is not 1.0 (nothing is logged if it has no effect). */
    private static void logBossMultiplier(Mob mob, String kind, double health, double damage, double armor) {
        if (health != 1.0 || damage != 1.0 || armor != 1.0) {
            HardcoreRegionDifficulty.LOGGER.debug("{} multiplier applied to {}: health x{}, damage x{}, armor x{}",
                    kind, mob.getType().getDescription().getString(), health, damage, armor);
        }
    }

    /** Whitelist entry with details for this mob: the region's entries first, then the global ones. */
    private static MobEntry findEntry(Mob mob, DifficultyRegion region, ScalingSettings scaling) {
        EntityType<?> type = mob.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        java.util.function.Predicate<ResourceLocation> tagTest = tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag));
        if (region != null) {
            for (MobEntry e : region.getMobEntries()) {
                if (e.hasDetails() && PatternMatcher.matchesSingle(e.getPattern(), id, tagTest)) {
                    return e;
                }
            }
        }
        // the global entries only depend on the mob type: cached per type
        return scaling != null ? MobTypeCache.get(mob).globalEntry() : null;
    }

    private static void removeModifiers(Mob mob) {
        remove(mob, Attributes.MAX_HEALTH, HEALTH_ID);
        remove(mob, Attributes.ARMOR, ARMOR_MUL_ID);
        remove(mob, Attributes.ARMOR, ARMOR_ADD_ID);
        remove(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        remove(mob, Attributes.MOVEMENT_SPEED, SPEED_ID);
        remove(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
        remove(mob, Attributes.FOLLOW_RANGE, FOLLOW_ID);
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

    /** Does the mob's type match the patterns (id, #tag, ~regex, *, !not)? */
    private static boolean matchesPatterns(Mob mob, java.util.List<String> patterns) {
        EntityType<?> type = mob.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return PatternMatcher.matches(patterns, id,
                tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag)));
    }

    public static boolean isExcluded(Mob mob) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        ScalingSettings scaling = config == null ? null : config.getScalingSettings();
        if (scaling == null || scaling.getExcludePatterns().isEmpty()) {
            return false;
        }
        return MobTypeCache.get(mob).excluded();
    }

    /** True when the mob already got its level (a mob loaded from the world keeps it), so joining again does nothing. */
    public static boolean isScaled(Mob mob) {
        return mob.getPersistentData().getBoolean(SCALED_TAG);
    }

    public static boolean shouldScale(Mob mob) {
        if (!ForgeConfig.isEnabled()) {
            return false;
        }
        EntityType<?> type = mob.getType();

        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.WARDEN) {
            return false;
        }
        BossLevel bossLevel = BossDetector.getBossLevel(mob);

        if (!(mob instanceof Enemy) && bossLevel == null) {
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

        if (!MobTypeCache.get(mob).whitelistOk()) {
            return false;
        }

        if (bossLevel != null) {
            return true;
        }

        double threshold = scaling.getBossHealthThreshold();
        if (threshold > 0 && mob.getAttributeBaseValue(Attributes.MAX_HEALTH) >= threshold) {
            return false;
        }
        return true;
    }
}
