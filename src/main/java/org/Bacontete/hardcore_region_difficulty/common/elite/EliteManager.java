package org.Bacontete.hardcore_region_difficulty.common.elite;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rolls, applies, saves and shows the affixes of elite mobs.
 * <p>
 * The affix ids are saved in the persistent data of the mob (so they survive unloading and restarts), the
 * attribute modifiers and potion effects are saved by vanilla, and the name is a normal custom name.
 */
public final class EliteManager {

    /** Persistent data key: list of affix ids. Its presence means "this mob is elite". */
    private static final String AFFIX_TAG = HardcoreRegionDifficulty.MODID + ":elite_affixes";
    /** Persistent data key: the difficulty level the mob became elite at (to count it again after a reload). */
    private static final String LEVEL_TAG = HardcoreRegionDifficulty.MODID + ":elite_level";
    private static final String PREFIX_KEY = HardcoreRegionDifficulty.MODID + ".elite.prefix";

    /** Living (loaded) elites by difficulty level, used for the elite_max_per_level limit. Memory only. */
    private static final Map<Integer, Set<UUID>> ALIVE_BY_LEVEL = new ConcurrentHashMap<>();

    private EliteManager() {
    }

    /**
     * Called by EntityScaler once per mob (right after it was scaled). With the chance of its level the mob
     * becomes elite: it gets 1-2 random affixes, a colored name and particles.
     *
     * @return true when the mob became elite
     */
    public static boolean tryRoll(Mob mob, int level, ScalingSettings scaling) {
        EliteSettings settings = scaling.getEliteSettings();
        if (!settings.isEnabled() || level < settings.getMinLevel() || isElite(mob)) {
            return false;
        }
        if (mob.hasCustomName()) {
            // never rename a mob that already has a name (name tag, other mods)
            HardcoreRegionDifficulty.LOGGER.debug("Elite skipped for {}: it already has a custom name",
                    mob.getType().getDescription().getString());
            return false;
        }
        int limit = settings.getMaxPerLevel();
        if (limit > 0 && aliveCount(level) >= limit) {
            // this level already has its elite(s): wait until one dies or unloads
            HardcoreRegionDifficulty.LOGGER.debug("Elite skipped for {}: level {} already has {} elite(s) (limit {})",
                    mob.getType().getDescription().getString(), level, aliveCount(level), limit);
            return false;
        }
        double chance = settings.chanceFor(level);
        RandomSource random = mob.getRandom();
        if (random.nextDouble() >= chance) {
            return false;
        }
        List<EliteAffix> affixes = pickAffixes(random, settings.affixCountFor(level));
        apply(mob, affixes, settings);
        mob.getPersistentData().putInt(LEVEL_TAG, level);
        track(mob);
        HardcoreRegionDifficulty.LOGGER.debug("Elite mob: {} at level {} (chance {}%) got affixes {}",
                mob.getType().getDescription().getString(), level,
                String.format(java.util.Locale.ROOT, "%.1f", chance * 100.0D), ids(affixes));
        return true;
    }

    /** Number of living elites counted at this difficulty level. */
    public static int aliveCount(int level) {
        Set<UUID> alive = ALIVE_BY_LEVEL.get(level);
        return alive == null ? 0 : alive.size();
    }

    /** Counts an elite at its level (a new elite, or one that was loaded again). Does nothing for normal mobs. */
    public static void track(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (isElite(mob) && data.contains(LEVEL_TAG, Tag.TAG_INT)) {
            ALIVE_BY_LEVEL.computeIfAbsent(data.getInt(LEVEL_TAG), l -> ConcurrentHashMap.newKeySet()).add(mob.getUUID());
        }
    }

    /** Stops counting an elite (died, despawned or unloaded), freeing its place for another one. */
    public static void untrack(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(LEVEL_TAG, Tag.TAG_INT)) {
            Set<UUID> alive = ALIVE_BY_LEVEL.get(data.getInt(LEVEL_TAG));
            if (alive != null && alive.remove(mob.getUUID())) {
                HardcoreRegionDifficulty.LOGGER.debug("Elite place freed at level {} ({} left)",
                        data.getInt(LEVEL_TAG), alive.size());
            }
        }
    }

    /** Forgets every counted elite (server starting / stopping, so a new world starts clean). */
    public static void clearTracked() {
        ALIVE_BY_LEVEL.clear();
    }

    /** Picks {@code count} different affixes at random. */
    static List<EliteAffix> pickAffixes(RandomSource random, int count) {
        List<EliteAffix> pool = new ArrayList<>(List.of(EliteAffix.values()));
        List<EliteAffix> picked = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            picked.add(pool.remove(random.nextInt(pool.size())));
        }
        return picked;
    }

    private static void apply(Mob mob, List<EliteAffix> affixes, EliteSettings settings) {
        // Tough raises the max health: keep the same health fraction so the mob does not spawn wounded
        float oldMax = mob.getMaxHealth();
        float fraction = oldMax > 0 ? mob.getHealth() / oldMax : 1.0F;

        ListTag saved = new ListTag();
        for (EliteAffix affix : affixes) {
            affix.applyTo(mob);
            saved.add(StringTag.valueOf(affix.id()));
        }
        mob.getPersistentData().put(AFFIX_TAG, saved);
        mob.setHealth(fraction * mob.getMaxHealth());

        mob.setCustomName(buildName(mob, affixes, settings.isShowAffixNames()));
        mob.setCustomNameVisible(true);
    }

    /** Name shown above the mob: "[Elite] Zombie (Fire, Swift)" with colors; the affix list can be turned off. */
    static Component buildName(Mob mob, List<EliteAffix> affixes, boolean showAffixes) {
        MutableComponent name = Component.translatable(PREFIX_KEY).withStyle(ChatFormatting.RED)
                .append(Component.literal(" "))
                .append(mob.getType().getDescription().copy().withStyle(ChatFormatting.WHITE));
        if (showAffixes && !affixes.isEmpty()) {
            MutableComponent list = Component.literal(" (").withStyle(ChatFormatting.GRAY);
            for (int i = 0; i < affixes.size(); i++) {
                if (i > 0) {
                    list.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                EliteAffix affix = affixes.get(i);
                list.append(Component.translatable(affix.translationKey()).withStyle(affix.color()));
            }
            list.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
            name.append(list);
        }
        return name;
    }

    /** True when the mob has elite affixes saved. */
    public static boolean isElite(Mob mob) {
        return mob.getPersistentData().contains(AFFIX_TAG, Tag.TAG_LIST);
    }

    /** The affixes of this mob (empty when it is not elite). Unknown ids (older / newer versions) are skipped. */
    public static List<EliteAffix> getAffixes(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(AFFIX_TAG, Tag.TAG_LIST)) {
            return Collections.emptyList();
        }
        ListTag saved = data.getList(AFFIX_TAG, Tag.TAG_STRING);
        List<EliteAffix> result = new ArrayList<>(saved.size());
        for (int i = 0; i < saved.size(); i++) {
            EliteAffix affix = EliteAffix.byId(saved.getString(i));
            if (affix != null) {
                result.add(affix);
            }
        }
        return result;
    }

    public static boolean hasAffix(Mob mob, EliteAffix affix) {
        return isElite(mob) && getAffixes(mob).contains(affix);
    }

    /** Runs the on-hit part of every affix of the attacker. Call on the server only. */
    public static void onHit(Mob attacker, LivingEntity victim, float damage) {
        if (attacker == victim || damage <= 0.0F) {
            return;
        }
        for (EliteAffix affix : getAffixes(attacker)) {
            affix.hit(attacker, victim, damage);
        }
    }

    /** A few particles around the elite, one kind per affix (light "glow"); off with elite_particles = false. */
    public static void showParticles(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        DifficultyConfig config = DifficultyConfig.getInstance();
        ScalingSettings scaling = config == null ? null : config.getScalingSettings();
        if (scaling == null || !scaling.getEliteSettings().isParticles()) {
            return;
        }
        double w = mob.getBbWidth() * 0.5D;
        double h = mob.getBbHeight();
        for (EliteAffix affix : getAffixes(mob)) {
            level.sendParticles(affix.particle(), mob.getX(), mob.getY() + h * 0.5D, mob.getZ(),
                    2, w, h * 0.3D, w, 0.01D);
        }
    }

    private static String ids(List<EliteAffix> affixes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < affixes.size(); i++) {
            sb.append(i > 0 ? ", " : "").append(affixes.get(i).id());
        }
        return sb.append(']').toString();
    }
}
