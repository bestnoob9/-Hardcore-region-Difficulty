package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyIcons;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyPreset;
import org.Bacontete.hardcore_region_difficulty.common.config.PresetStore;
import org.Bacontete.hardcore_region_difficulty.common.config.LootSettings;
import org.Bacontete.hardcore_region_difficulty.common.config.ScalingSettings;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteAffix;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteSettings;
import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;
import org.Bacontete.hardcore_region_difficulty.common.entity.BossLevel;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * In-game editor of the difficulty regions and of the difficulty levels (title, icon, color) of
 * config/hardcore_region_difficulty.json, built from Cloth Config entries. Adds two tabs.
 * Only load this class when cloth_config is present.
 * <p>
 * Editing is only offered in the main menu and in single player: on a remote server the server's file decides.
 */
public final class RegionEditorTabs {

    private static final String K = "hardcore_region_difficulty.config.";
    private static final Gson GSON = new Gson();
    private static final int NEW_LEVEL_SLOTS = 1;
    /** Levels up to this one always get an entry (region levels 1-5 and the entries of the boss classes). Own levels start after it. */
    private static final int BUILT_IN_LEVELS = 5;

    private RegionEditorTabs() {}

    private static final class LevelModel {
        JsonObject original;
        boolean isNew;
        boolean enable;
        boolean delete;
        int level = 1;
        String title = "";
        String icon = "";
        String color = "";
        // custom stats of this level (total bonus in %, armor flat in points); only stored when customStats is on
        boolean customStats;
        double health;
        double damage;
        double armor;
        double speed;
        double armorFlat;
        // own item quality chances of this level (% bonus); only stored when customQuality is on
        boolean customQuality;
        double quality;
        double highQuality;
    }

    /** Default step of the formula (scaling_settings). */
    private static final class ScalingModel {
        boolean linear;
        double health;
        double damage;
        double armor;
        double speed;
        double armorFlat;
        // soft cap + diminishing returns, and the own multipliers of boss / mini-boss
        int softCap;
        double diminishing;
        double miniBossHealthMul, miniBossDamageMul, miniBossArmorMul;
        double bossHealthMul, bossDamageMul, bossArmorMul;
        // elite mobs (elite_* keys)
        boolean eliteEnabled, eliteParticles, eliteShowNames;
        int eliteMinLevel, eliteMaxAffixes, eliteMaxPerLevel;
        double eliteBaseChance, eliteChancePerLevel;
        // extra bonus (%) of the boss classes, added on top of the bonus of the region level
        double miniBossHealth;
        double miniBossArmor;
        double miniBossDamage;
        double bossHealth;
        double bossArmor;
        double bossDamage;
        int raidLevel;
        // level cap by dimension
        boolean capEnabled;
        int capBonus;
        int maxLevel;
        List<String> dimensionCaps = new ArrayList<>();
        // loot by level (scaling_settings.loot)
        boolean lootEnabled, lootChests, lootMobs;
        int lootMinLevel, lootMaxRolls, lootMaxExtraDrops;
        double lootRollChance, lootCount, lootEnchant, lootMobDrop, lootMobCount, lootMobEnchant;
    }

    /** The values of the form (and of the boss config) in the order of {@link DifficultyPreset#KEYS}. */
    private static double[] currentValues(ScalingModel m) {
        return new double[] {
                m.health, m.damage, m.armor, m.speed, m.armorFlat,
                m.miniBossHealth, m.miniBossArmor, m.miniBossDamage,
                m.bossHealth, m.bossArmor, m.bossDamage,
                ForgeConfig.DRAGON_HEALTH.get(), ForgeConfig.DRAGON_DAMAGE_MULTIPLIER.get(),
                ForgeConfig.WITHER_HEALTH.get(), ForgeConfig.WITHER_DAMAGE_MULTIPLIER.get(),
                ForgeConfig.WARDEN_HEALTH.get(), ForgeConfig.WARDEN_DAMAGE_MULTIPLIER.get(),
                m.raidLevel };
    }

    /** One entry of dimension_level_caps: "dimension_id=level", e.g. minecraft:the_end=3. */
    private static Optional<Component> capEntryError(String v) {
        if (v == null || v.isBlank()) return Optional.empty();
        int eq = v.lastIndexOf('=');
        try {
            int lvl = Integer.parseInt(v.substring(eq + 1).trim());
            if (eq > 0 && lvl >= 1 && lvl <= ScalingSettings.ABSOLUTE_MAX_LEVEL) return Optional.empty();
        } catch (NumberFormatException ignored) {
        }
        return Optional.of(t("levelcap.dimensions.invalid"));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Fills the stat fields with what the level gets now (custom value if set, else the formula). */
    private static void fillStatDefaults(LevelModel m, ScalingSettings sc, JsonObject o) {
        m.health = o != null && o.has("health_bonus_percent") ? num(o, "health_bonus_percent") : round1(sc.healthAmount(m.level) * 100.0);
        m.damage = o != null && o.has("damage_bonus_percent") ? num(o, "damage_bonus_percent") : round1(sc.damageAmount(m.level) * 100.0);
        m.armor = o != null && o.has("armor_bonus_percent") ? num(o, "armor_bonus_percent") : round1(sc.armorMultiplyAmount(m.level) * 100.0);
        m.speed = o != null && o.has("speed_bonus_percent") ? num(o, "speed_bonus_percent") : round1(sc.speedAmount(m.level) * 100.0);
        m.armorFlat = o != null && o.has("armor_flat_bonus") ? num(o, "armor_flat_bonus") : round1(sc.armorFlatAmount(m.level));
        m.customStats = o != null && (o.has("health_bonus_percent") || o.has("damage_bonus_percent")
                || o.has("armor_bonus_percent") || o.has("speed_bonus_percent") || o.has("armor_flat_bonus"));
        double step = QualityConfig.DIFFICULTY_QUALITY_BONUS.get() * Math.max(0, m.level - 1) * 100.0;
        m.quality = o != null && o.has("quality_bonus_percent") ? num(o, "quality_bonus_percent") : round1(step);
        m.highQuality = o != null && o.has("high_quality_bonus_percent") ? num(o, "high_quality_bonus_percent")
                : round1(step * QualityConfig.HIGH_TIER_BONUS_MULTIPLIER.get());
        m.customQuality = o != null && (o.has("quality_bonus_percent") || o.has("high_quality_bonus_percent"));
    }

    private static double num(JsonObject o, String key) {
        try {
            return o.get(key).getAsDouble();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    // ------------------------------------------------------------------
    // Screen
    // ------------------------------------------------------------------

    /**
     * Adds the "regions" and "levels" groups to the given tab.
     * @return the runnable to call from the screen's saving runnable (writes the file)
     */
    public static Runnable add(ConfigEntryBuilder entries, ConfigCategory tab) {
        Minecraft mc = Minecraft.getInstance();
        boolean editable = mc.level == null || mc.getSingleplayerServer() != null;
        DifficultyConfig cfg = DifficultyConfig.getInstance();

        if (!editable || cfg == null) {
            tab.addEntry(entries.startTextDescription(
                    t("region_editor.remote").copy().withStyle(ChatFormatting.RED)).build());
            return () -> {};
        }
        SubCategoryBuilder levelsGroup = entries.startSubCategory(t("category.levels")).setExpanded(false);

        JsonObject root = readRoot(cfg.getConfigFile());
        JsonArray levelArray = root.has("difficulty_levels") && root.get("difficulty_levels").isJsonArray()
                ? root.getAsJsonArray("difficulty_levels") : new JsonArray();

        // ---------------- levels ----------------
        TreeMap<Integer, LevelModel> levelModels = new TreeMap<>();
        for (JsonElement element : levelArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            if (!o.has("level") || !o.get("level").isJsonPrimitive()) {
                continue;
            }
            LevelModel m = new LevelModel();
            m.original = o;
            m.level = o.get("level").getAsInt();
            m.title = str(o, "title");
            m.icon = str(o, "icon");
            m.color = str(o, "title_color");
            fillStatDefaults(m, cfg.getScalingSettings(), o);
            levelModels.put(m.level, m);
        }
        List<Integer> builtIn = new ArrayList<>();
        for (int level = 1; level <= BUILT_IN_LEVELS; level++) builtIn.add(level);
        for (BossLevel boss : BossLevel.values()) builtIn.add(boss.configLevel());
        for (int level : builtIn) {
            if (!levelModels.containsKey(level)) {
                LevelModel m = new LevelModel();
                m.level = level;
                fillStatDefaults(m, cfg.getScalingSettings(), null);
                levelModels.put(level, m);
            }
        }
        Set<Integer> existingLevels = new HashSet<>(levelModels.keySet());
        int highest = BossLevel.lastConfigLevel();
        for (int lvl : levelModels.keySet()) {
            if (lvl <= BossLevel.MAX_CUSTOM_LEVEL) highest = Math.max(highest, lvl);
        }

        List<LevelModel> levelList = new ArrayList<>(levelModels.values());
        levelsGroup.add(entries.startTextDescription(t("level_editor.note")).build());

        // default step of the formula, shared by every level without custom stats
        ScalingSettings sc = cfg.getScalingSettings();
        ScalingModel scaling = new ScalingModel();
        scaling.linear = sc.isLinear();
        scaling.health = sc.getHealthPercent();
        scaling.damage = sc.getDamagePercent();
        scaling.armor = sc.getArmorPercent();
        scaling.speed = sc.getSpeedPercent();
        scaling.armorFlat = sc.getArmorFlatPerLevel();
        scaling.miniBossHealth = sc.getMiniBossHealthPercent();
        scaling.miniBossArmor = sc.getMiniBossArmorPercent();
        scaling.miniBossDamage = sc.getMiniBossDamagePercent();
        scaling.bossHealth = sc.getBossHealthPercent();
        scaling.bossArmor = sc.getBossArmorPercent();
        scaling.bossDamage = sc.getBossDamagePercent();
        scaling.raidLevel = sc.getRaidDifficultyLevel();
        scaling.softCap = sc.getSoftCapLevel();
        scaling.diminishing = sc.getDiminishingFactor();
        scaling.bossHealthMul = sc.getBossHealthMultiplier();
        scaling.bossDamageMul = sc.getBossDamageMultiplier();
        scaling.bossArmorMul = sc.getBossArmorMultiplier();
        scaling.miniBossHealthMul = sc.getMiniBossHealthMultiplier();
        scaling.miniBossDamageMul = sc.getMiniBossDamageMultiplier();
        scaling.miniBossArmorMul = sc.getMiniBossArmorMultiplier();
        {
            EliteSettings es = sc.getEliteSettings();
            scaling.eliteEnabled = es.isEnabled();
            scaling.eliteMinLevel = es.getMinLevel();
            scaling.eliteBaseChance = es.getBaseChance();
            scaling.eliteChancePerLevel = es.getChancePerLevel();
            scaling.eliteMaxAffixes = es.getMaxAffixes();
            scaling.eliteMaxPerLevel = es.getMaxPerLevel();
            scaling.eliteParticles = es.isParticles();
            scaling.eliteShowNames = es.isShowAffixNames();
        }
        {
            LootSettings ls = sc.getLootSettings();
            scaling.lootEnabled = ls.isEnabled();
            scaling.lootChests = ls.isChestsEnabled();
            scaling.lootMobs = ls.isMobsEnabled();
            scaling.lootMinLevel = ls.getMinLevel();
            scaling.lootMaxRolls = ls.getMaxExtraRolls();
            scaling.lootMaxExtraDrops = ls.getMobMaxExtraDrops();
            scaling.lootRollChance = ls.getExtraRollChancePerLevel();
            scaling.lootCount = ls.getCountPercentPerLevel();
            scaling.lootEnchant = ls.getEnchantChancePerLevel();
            scaling.lootMobDrop = ls.getMobExtraDropChancePerLevel();
            scaling.lootMobCount = ls.getMobCountPercentPerLevel();
            scaling.lootMobEnchant = ls.getMobEnchantChancePerLevel();
        }
        scaling.capEnabled = sc.isLevelCapEnabled();
        scaling.capBonus = sc.getDimensionLevelBonus();
        scaling.maxLevel = sc.getMaxRegionLevelSetting();
        for (var capEntry : sc.getDimensionLevelCaps().entrySet()) {
            scaling.dimensionCaps.add(capEntry.getKey() + "=" + capEntry.getValue());
        }
        SubCategoryBuilder scalingSub = entries.startSubCategory(t("scaling.group")).setExpanded(false);
        scalingSub.add(entries.startTextDescription(t("scaling.note")).build());
        scalingSub.add(entries.startBooleanToggle(t("scaling.linear"), scaling.linear)
                .setDefaultValue(false)
                .setTooltip(TooltipWrap.wrap(t("scaling.linear.tooltip")))
                .setSaveConsumer(v -> scaling.linear = v)
                .build());
        scalingSub.add(percentField("scaling.health", scaling.health, ScalingSettings.DEFAULT_HEALTH_PERCENT, v -> scaling.health = v));
        scalingSub.add(percentField("scaling.damage", scaling.damage, ScalingSettings.DEFAULT_DAMAGE_PERCENT, v -> scaling.damage = v));
        scalingSub.add(percentField("scaling.armor", scaling.armor, ScalingSettings.DEFAULT_ARMOR_PERCENT, v -> scaling.armor = v));
        scalingSub.add(percentField("scaling.speed", scaling.speed, ScalingSettings.DEFAULT_SPEED_PERCENT, v -> scaling.speed = v));
        scalingSub.add(percentField("scaling.armor_flat", scaling.armorFlat, ScalingSettings.DEFAULT_ARMOR_FLAT, v -> scaling.armorFlat = v));
        // soft cap: from this level on the % bonus grows more slowly (0 = off)
        scalingSub.add(entries.startIntField(t("scaling.soft_cap"), scaling.softCap)
                .setDefaultValue(ScalingSettings.DEFAULT_SOFT_CAP_LEVEL)
                .setMin(0)
                .setMax(ScalingSettings.ABSOLUTE_MAX_LEVEL)
                .setTooltip(TooltipWrap.wrap(t("scaling.soft_cap.tooltip")))
                .setSaveConsumer(v -> scaling.softCap = v)
                .build());
        scalingSub.add(entries.startDoubleField(t("scaling.diminishing"), scaling.diminishing)
                .setDefaultValue(ScalingSettings.DEFAULT_DIMINISHING_FACTOR)
                .setMin(0)
                .setMax(1)
                .setTooltip(TooltipWrap.wrap(t("scaling.diminishing.tooltip")))
                .setSaveConsumer(v -> scaling.diminishing = v)
                .build());
        SubCategoryBuilder capSub = entries.startSubCategory(t("levelcap.group")).setExpanded(false);
        capSub.add(entries.startTextDescription(t("levelcap.note")).build());
        capSub.add(entries.startBooleanToggle(t("levelcap.enabled"), scaling.capEnabled)
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("levelcap.enabled.tooltip")))
                .setSaveConsumer(v -> scaling.capEnabled = v)
                .build());
        capSub.add(entries.startIntField(t("levelcap.bonus"), scaling.capBonus)
                .setDefaultValue(ScalingSettings.DEFAULT_DIMENSION_LEVEL_BONUS)
                .setMin(0)
                .setMax(ScalingSettings.ABSOLUTE_MAX_LEVEL)
                .setTooltip(TooltipWrap.wrap(t("levelcap.bonus.tooltip")))
                .setSaveConsumer(v -> scaling.capBonus = v)
                .build());
        capSub.add(entries.startIntField(t("levelcap.max"), scaling.maxLevel)
                .setDefaultValue(0)
                .setMin(0)
                .setMax(ScalingSettings.ABSOLUTE_MAX_LEVEL)
                .setTooltip(TooltipWrap.wrap(t("levelcap.max.tooltip")))
                .setSaveConsumer(v -> scaling.maxLevel = v)
                .build());
        capSub.add(entries.startStrList(t("levelcap.dimensions"), scaling.dimensionCaps)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(TooltipWrap.wrap(t("levelcap.dimensions.tooltip")))
                .setCellErrorSupplier(v -> capEntryError(v))
                .setSaveConsumer(v -> scaling.dimensionCaps = new ArrayList<>(v))
                .build());
        levelsGroup.add(capSub.build());

        // elite mobs: random affixes on mobs of a high enough level
        SubCategoryBuilder eliteSub = entries.startSubCategory(t("elite.group")).setExpanded(false);
        eliteSub.add(entries.startTextDescription(t("elite.note")).build());
        eliteSub.add(entries.startBooleanToggle(t("elite.enabled"), scaling.eliteEnabled)
                .setDefaultValue(EliteSettings.DEFAULT_ENABLED)
                .setTooltip(TooltipWrap.wrap(t("elite.enabled.tooltip")))
                .setSaveConsumer(v -> scaling.eliteEnabled = v)
                .build());
        eliteSub.add(entries.startIntField(t("elite.min_level"), scaling.eliteMinLevel)
                .setDefaultValue(EliteSettings.DEFAULT_MIN_LEVEL)
                .setMin(1)
                .setMax(ScalingSettings.ABSOLUTE_MAX_LEVEL)
                .setTooltip(TooltipWrap.wrap(t("elite.min_level.tooltip")))
                .setSaveConsumer(v -> scaling.eliteMinLevel = v)
                .build());
        eliteSub.add(entries.startDoubleField(t("elite.base_chance"), scaling.eliteBaseChance)
                .setDefaultValue(EliteSettings.DEFAULT_BASE_CHANCE)
                .setMin(0)
                .setMax(1)
                .setTooltip(TooltipWrap.wrap(t("elite.base_chance.tooltip")))
                .setSaveConsumer(v -> scaling.eliteBaseChance = v)
                .build());
        eliteSub.add(entries.startDoubleField(t("elite.chance_per_level"), scaling.eliteChancePerLevel)
                .setDefaultValue(EliteSettings.DEFAULT_CHANCE_PER_LEVEL)
                .setMin(0)
                .setMax(1)
                .setTooltip(TooltipWrap.wrap(t("elite.chance_per_level.tooltip")))
                .setSaveConsumer(v -> scaling.eliteChancePerLevel = v)
                .build());
        eliteSub.add(entries.startIntField(t("elite.max_affixes"), scaling.eliteMaxAffixes)
                .setDefaultValue(EliteSettings.DEFAULT_MAX_AFFIXES)
                .setMin(1)
                .setMax(EliteAffix.values().length)
                .setTooltip(TooltipWrap.wrap(t("elite.max_affixes.tooltip")))
                .setSaveConsumer(v -> scaling.eliteMaxAffixes = v)
                .build());
        eliteSub.add(entries.startIntField(t("elite.max_per_level"), scaling.eliteMaxPerLevel)
                .setDefaultValue(EliteSettings.DEFAULT_MAX_PER_LEVEL)
                .setMin(0)
                .setMax(1000)
                .setTooltip(TooltipWrap.wrap(t("elite.max_per_level.tooltip")))
                .setSaveConsumer(v -> scaling.eliteMaxPerLevel = v)
                .build());
        eliteSub.add(entries.startBooleanToggle(t("elite.particles"), scaling.eliteParticles)
                .setDefaultValue(EliteSettings.DEFAULT_PARTICLES)
                .setTooltip(TooltipWrap.wrap(t("elite.particles.tooltip")))
                .setSaveConsumer(v -> scaling.eliteParticles = v)
                .build());
        eliteSub.add(entries.startBooleanToggle(t("elite.show_names"), scaling.eliteShowNames)
                .setDefaultValue(EliteSettings.DEFAULT_SHOW_AFFIX_NAMES)
                .setTooltip(TooltipWrap.wrap(t("elite.show_names.tooltip")))
                .setSaveConsumer(v -> scaling.eliteShowNames = v)
                .build());
        levelsGroup.add(eliteSub.build());

        SubCategoryBuilder lootSub = entries.startSubCategory(t("loot.group")).setExpanded(false);
        lootSub.add(entries.startTextDescription(t("loot.note")).build());
        lootSub.add(entries.startBooleanToggle(t("loot.enabled"), scaling.lootEnabled).setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("loot.enabled.tooltip"))).setSaveConsumer(v -> scaling.lootEnabled = v).build());
        lootSub.add(entries.startBooleanToggle(t("loot.chests"), scaling.lootChests).setDefaultValue(true)
                .setSaveConsumer(v -> scaling.lootChests = v).build());
        lootSub.add(entries.startBooleanToggle(t("loot.mobs"), scaling.lootMobs).setDefaultValue(true)
                .setSaveConsumer(v -> scaling.lootMobs = v).build());
        lootSub.add(entries.startIntField(t("loot.min_level"), scaling.lootMinLevel).setDefaultValue(2).setMin(1).setMax(98)
                .setTooltip(TooltipWrap.wrap(t("loot.min_level.tooltip"))).setSaveConsumer(v -> scaling.lootMinLevel = v).build());
        lootSub.add(entries.startDoubleField(t("loot.roll_chance"), scaling.lootRollChance).setDefaultValue(10).setMin(0).setMax(100)
                .setTooltip(TooltipWrap.wrap(t("loot.roll_chance.tooltip"))).setSaveConsumer(v -> scaling.lootRollChance = v).build());
        lootSub.add(entries.startIntField(t("loot.max_rolls"), scaling.lootMaxRolls).setDefaultValue(3).setMin(0).setMax(10)
                .setTooltip(TooltipWrap.wrap(t("loot.max_rolls.tooltip"))).setSaveConsumer(v -> scaling.lootMaxRolls = v).build());
        lootSub.add(entries.startDoubleField(t("loot.count"), scaling.lootCount).setDefaultValue(10).setMin(0).setMax(1000)
                .setTooltip(TooltipWrap.wrap(t("loot.count.tooltip"))).setSaveConsumer(v -> scaling.lootCount = v).build());
        lootSub.add(entries.startDoubleField(t("loot.enchant"), scaling.lootEnchant).setDefaultValue(4).setMin(0).setMax(100)
                .setTooltip(TooltipWrap.wrap(t("loot.enchant.tooltip"))).setSaveConsumer(v -> scaling.lootEnchant = v).build());
        lootSub.add(entries.startDoubleField(t("loot.mob_drop"), scaling.lootMobDrop).setDefaultValue(6).setMin(0).setMax(100)
                .setTooltip(TooltipWrap.wrap(t("loot.mob_drop.tooltip"))).setSaveConsumer(v -> scaling.lootMobDrop = v).build());
        lootSub.add(entries.startIntField(t("loot.mob_max_drops"), scaling.lootMaxExtraDrops).setDefaultValue(2).setMin(0).setMax(10)
                .setSaveConsumer(v -> scaling.lootMaxExtraDrops = v).build());
        lootSub.add(entries.startDoubleField(t("loot.mob_count"), scaling.lootMobCount).setDefaultValue(6).setMin(0).setMax(1000)
                .setSaveConsumer(v -> scaling.lootMobCount = v).build());
        lootSub.add(entries.startDoubleField(t("loot.mob_enchant"), scaling.lootMobEnchant).setDefaultValue(2).setMin(0).setMax(100)
                .setSaveConsumer(v -> scaling.lootMobEnchant = v).build());
        levelsGroup.add(lootSub.build());
        // ---- presets: built-in (easy / normal / hardcore) + the player's own; applied on save when changed ----
        final java.io.File presetDir = cfg.getConfigDirectory();
        final List<DifficultyPreset> allPresets = new ArrayList<>(DifficultyPreset.builtIns());
        allPresets.addAll(PresetStore.load(presetDir));
        final String customLabel = t("preset.custom").getString();
        java.util.function.Function<DifficultyPreset, String> presetLabel =
                p -> p.builtIn ? t("preset." + p.id).getString() : p.id;
        List<String> presetLabels = new ArrayList<>();
        for (DifficultyPreset p : allPresets) presetLabels.add(presetLabel.apply(p));
        presetLabels.add(customLabel);
        DifficultyPreset detected = DifficultyPreset.detect(allPresets, currentValues(scaling));
        final String initialLabel = detected == null ? customLabel : presetLabel.apply(detected);
        final String[] presetChoice = { initialLabel };
        final String[] newPresetName = { "" };
        final boolean[] deletePreset = { false };
        tab.addEntry(entries.startDropdownMenu(t("preset"), initialLabel, str -> str.trim(), Component::literal)
                .setSelections(presetLabels)
                .setDefaultValue(presetLabel.apply(DifficultyPreset.NORMAL))
                .setErrorSupplier(v -> presetLabels.contains(v) ? Optional.empty() : Optional.of(t("preset.invalid")))
                .setTooltip(TooltipWrap.wrap(t("preset.tooltip")))
                .setSaveConsumer(v -> presetChoice[0] = v)
                .build());
        SubCategoryBuilder ownPresets = entries.startSubCategory(t("preset.own")).setExpanded(false);
        ownPresets.add(entries.startTextDescription(t("preset.own.note")).build());
        ownPresets.add(entries.startStrField(t("preset.new_name"), "")
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("preset.new_name.tooltip")))
                .setSaveConsumer(v -> newPresetName[0] = v.trim())
                .build());
        ownPresets.add(entries.startBooleanToggle(t("preset.delete"), false)
                .setDefaultValue(false)
                .setTooltip(TooltipWrap.wrap(t("preset.delete.tooltip")))
                .setSaveConsumer(v -> deletePreset[0] = v)
                .build());
        tab.addEntry(ownPresets.build());
        // type just the number: 0 = off, 1-5 and every level that exists in the config (also the ones you added, 6+)
        List<Integer> raidChoices = new ArrayList<>();
        raidChoices.add(0);
        for (int lvl = 1; lvl <= 5; lvl++) raidChoices.add(lvl);
        for (int lvl : cfg.getLevelDefs().keySet()) {
            if (cfg.isUnlockedLevel(lvl) && !raidChoices.contains(lvl)) raidChoices.add(lvl);
        }
        java.util.Collections.sort(raidChoices);
        final String raidValid = raidChoices.toString().replace("[", "").replace("]", "");
        tab.addEntry(entries.startIntField(t("scaling.raid"), scaling.raidLevel)
                .setDefaultValue(ScalingSettings.DEFAULT_RAID_LEVEL)
                .setMin(0)
                .setMax(ScalingSettings.ABSOLUTE_MAX_LEVEL)
                .setErrorSupplier(v -> raidChoices.contains(v) ? java.util.Optional.empty()
                        : java.util.Optional.of(Component.translatable(K + "scaling.raid.invalid", raidValid)))
                .setTooltip(TooltipWrap.wrap(t("scaling.raid.tooltip")))
                .setSaveConsumer(v -> scaling.raidLevel = v)
                .build());
        levelsGroup.add(scalingSub.build());
        for (LevelModel m : levelList) {
            MutableComponent title = Component.literal("Level " + m.level
                    + (m.title.isEmpty() ? "" : "  -  " + m.title)
                    + (m.icon.isEmpty() ? "" : "  [" + m.icon + "]"));
            BossLevel boss = BossLevel.fromConfigLevel(m.level);
            if (boss != null) {
                // boss entries: used by the boss warning title, never by regions
                title.append(Component.literal("  -  ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(K + (boss == BossLevel.BOSS ? "level_editor.boss" : "level_editor.mini_boss"))
                                .withStyle(ChatFormatting.RED));
            }
            SubCategoryBuilder sub = entries.startSubCategory(title).setExpanded(false);
            addLevelFields(sub, entries, m, existingLevels, scaling);
            levelsGroup.add(sub.build());
        }
        for (int slot = 1; slot <= NEW_LEVEL_SLOTS && highest + slot <= BossLevel.MAX_CUSTOM_LEVEL; slot++) {
            LevelModel m = new LevelModel();
            m.isNew = true;
            m.level = highest + slot;
            fillStatDefaults(m, cfg.getScalingSettings(), null);
            levelList.add(m);
            SubCategoryBuilder sub = entries.startSubCategory(
                    Component.translatable(K + "level_editor.new", slot).withStyle(ChatFormatting.GREEN))
                    .setExpanded(false);
            addLevelFields(sub, entries, m, existingLevels, scaling);
            levelsGroup.add(sub.build());
        }

        // The regions are not edited here any more: the Scan tab edits the level of dimensions and structures, and
        // difficulty_regions of the file is kept as it is (see DifficultyConfig#writeRegionsAndLevels with null).

        tab.addEntry(levelsGroup.build());
        return () -> {
            // the boss values were already stored by their own entries, the rest is in "scaling"
            if (deletePreset[0]) {
                final String sel = presetChoice[0];
                allPresets.removeIf(p -> !p.builtIn && p.id.equals(sel));
                PresetStore.save(presetDir, allPresets);
            } else if (!newPresetName[0].isBlank()) {
                final String name = newPresetName[0];
                boolean reserved = false;
                for (DifficultyPreset p : DifficultyPreset.builtIns()) {
                    if (p.id.equalsIgnoreCase(name) || presetLabel.apply(p).equalsIgnoreCase(name)) reserved = true;
                }
                if (!reserved && !name.equalsIgnoreCase(customLabel)) {
                    allPresets.removeIf(p -> !p.builtIn && p.id.equals(name));
                    allPresets.add(new DifficultyPreset(name, false, currentValues(scaling)));
                    PresetStore.save(presetDir, allPresets);
                }
            } else if (!presetChoice[0].equals(initialLabel) && !presetChoice[0].equals(customLabel)) {
                for (DifficultyPreset p : allPresets) {
                    if (!presetLabel.apply(p).equals(presetChoice[0])) continue;
                    scaling.health = p.get(DifficultyPreset.HEALTH);
                    scaling.damage = p.get(DifficultyPreset.DAMAGE);
                    scaling.armor = p.get(DifficultyPreset.ARMOR);
                    scaling.speed = p.get(DifficultyPreset.SPEED);
                    scaling.armorFlat = p.get(DifficultyPreset.ARMOR_FLAT);
                    scaling.miniBossHealth = p.get(DifficultyPreset.MINI_HEALTH);
                    scaling.miniBossArmor = p.get(DifficultyPreset.MINI_ARMOR);
                    scaling.miniBossDamage = p.get(DifficultyPreset.MINI_DAMAGE);
                    scaling.bossHealth = p.get(DifficultyPreset.BOSS_HEALTH);
                    scaling.bossArmor = p.get(DifficultyPreset.BOSS_ARMOR);
                    scaling.bossDamage = p.get(DifficultyPreset.BOSS_DAMAGE);
                    scaling.raidLevel = p.raid();
                    ForgeConfig.DRAGON_HEALTH.set(p.get(DifficultyPreset.DRAGON_HEALTH));
                    ForgeConfig.DRAGON_DAMAGE_MULTIPLIER.set(p.get(DifficultyPreset.DRAGON_DAMAGE));
                    ForgeConfig.WITHER_HEALTH.set(p.get(DifficultyPreset.WITHER_HEALTH));
                    ForgeConfig.WITHER_DAMAGE_MULTIPLIER.set(p.get(DifficultyPreset.WITHER_DAMAGE));
                    ForgeConfig.WARDEN_HEALTH.set(p.get(DifficultyPreset.WARDEN_HEALTH));
                    ForgeConfig.WARDEN_DAMAGE_MULTIPLIER.set(p.get(DifficultyPreset.WARDEN_DAMAGE));
                    ForgeConfig.SPEC.save();
                    break;
                }
            }
            save(cfg, levelList, existingLevels, scaling);
        };
    }

    private static Optional<Component> iconError(String text) {
        return DifficultyIcons.isValid(text) ? Optional.empty() : Optional.of(t("region_editor.icon_invalid"));
    }

    private static Optional<Component> colorError(String text) {
        if (text == null || text.isBlank() || DifficultyIcons.parseColorRgb(text) >= 0) {
            return Optional.empty();
        }
        return Optional.of(t("region_editor.color_invalid"));
    }

    /**
     * Icon editor: a dropdown with the built-in icons (typing a texture path such as
     * mypack:textures/gui/icon.png also works) and a live preview of the chosen image under it.
     */
    private static void addIconFields(SubCategoryBuilder sub, ConfigEntryBuilder e, String current,
                                      java.util.function.Consumer<String> setter,
                                      java.util.function.Supplier<String> fallback) {
        List<String> choices = new ArrayList<>();
        choices.add("");
        choices.addAll(DifficultyIcons.BUILT_IN);
        choices.addAll(IconFolder.list()); // .png files of config/hardcore_region_config/icon/iconlevel
        var dropdown = e.startStringDropdownMenu(t("region_editor.icon"), current == null ? "" : current)
                .setSelections(choices)
                .setSuggestionMode(true)
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("region_editor.icon.tooltip")))
                .setErrorSupplier(RegionEditorTabs::iconError)
                .setSaveConsumer(s -> setter.accept(s == null ? "" : s.trim()))
                .build();
        sub.add(dropdown);
        sub.add(new IconPreviewEntry(t("region_editor.icon_preview"), dropdown::getValue, fallback));
    }

    private static void addLevelFields(SubCategoryBuilder sub, ConfigEntryBuilder e, LevelModel m, Set<Integer> existing,
                                       ScalingModel scaling) {
        if (m.isNew) {
            sub.add(e.startBooleanToggle(t("level_editor.enable"), false)
                    .setDefaultValue(false)
                    .setTooltip(TooltipWrap.wrap(t("level_editor.enable.tooltip")))
                    .setSaveConsumer(v -> m.enable = v)
                    .build());
            sub.add(e.startIntField(t("level_editor.level"), m.level)
                    .setDefaultValue(m.level)
                    .setMin(BossLevel.lastConfigLevel() + 1)
                    .setMax(BossLevel.MAX_CUSTOM_LEVEL)
                    .setTooltip(TooltipWrap.wrap(t("level_editor.level.tooltip")))
                    .setErrorSupplier(v -> existing.contains(v)
                            ? Optional.of(t("level_editor.level_duplicate")) : Optional.empty())
                    .setSaveConsumer(v -> m.level = v)
                    .build());
        } else {
            sub.add(e.startBooleanToggle(t("level_editor.delete"), false)
                    .setDefaultValue(false)
                    .setTooltip(TooltipWrap.wrap(t("level_editor.delete.tooltip")))
                    .setSaveConsumer(v -> m.delete = v)
                    .build());
        }
        sub.add(e.startStrField(t("level_editor.title"), m.title)
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("level_editor.title.tooltip")))
                .setSaveConsumer(s -> m.title = s.trim())
                .build());
        addIconFields(sub, e, m.icon, s -> m.icon = s, () -> DifficultyIcons.defaultIconForLevel(m.level));
        sub.add(e.startStrField(t("region_editor.color"), m.color)
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("region_editor.color.tooltip")))
                .setErrorSupplier(RegionEditorTabs::colorError)
                .setSaveConsumer(s -> m.color = s.trim())
                .build());
        BossLevel bossClass = m.isNew ? null : BossLevel.fromConfigLevel(m.level);
        if (bossClass != null) {
            // boss classes: extra bonus on top of the bonus of the level of the region where the boss is
            boolean boss = bossClass == BossLevel.BOSS;
            sub.add(e.startTextDescription(t("level_editor.boss_stats.note")).build());
            sub.add(bossField(e, "level_editor.boss_health",
                    boss ? scaling.bossHealth : scaling.miniBossHealth,
                    boss ? ScalingSettings.DEFAULT_BOSS_HEALTH_BONUS : ScalingSettings.DEFAULT_MINI_BOSS_HEALTH,
                    v -> { if (boss) scaling.bossHealth = v; else scaling.miniBossHealth = v; }));
            sub.add(bossField(e, "level_editor.boss_armor",
                    boss ? scaling.bossArmor : scaling.miniBossArmor,
                    boss ? ScalingSettings.DEFAULT_BOSS_ARMOR : ScalingSettings.DEFAULT_MINI_BOSS_ARMOR,
                    v -> { if (boss) scaling.bossArmor = v; else scaling.miniBossArmor = v; }));
            sub.add(bossField(e, "level_editor.boss_damage",
                    boss ? scaling.bossDamage : scaling.miniBossDamage,
                    boss ? ScalingSettings.DEFAULT_BOSS_DAMAGE : ScalingSettings.DEFAULT_MINI_BOSS_DAMAGE,
                    v -> { if (boss) scaling.bossDamage = v; else scaling.miniBossDamage = v; }));
            // own multipliers: multiply the whole bonus of the boss (1.0 = unchanged)
            sub.add(multiplierField(e, "level_editor.boss_health_mult",
                    boss ? scaling.bossHealthMul : scaling.miniBossHealthMul, ScalingSettings.DEFAULT_BOSS_MULTIPLIER,
                    v -> { if (boss) scaling.bossHealthMul = v; else scaling.miniBossHealthMul = v; }));
            sub.add(multiplierField(e, "level_editor.boss_damage_mult",
                    boss ? scaling.bossDamageMul : scaling.miniBossDamageMul, ScalingSettings.DEFAULT_BOSS_MULTIPLIER,
                    v -> { if (boss) scaling.bossDamageMul = v; else scaling.miniBossDamageMul = v; }));
            sub.add(multiplierField(e, "level_editor.boss_armor_mult",
                    boss ? scaling.bossArmorMul : scaling.miniBossArmorMul, ScalingSettings.DEFAULT_BOSS_MULTIPLIER,
                    v -> { if (boss) scaling.bossArmorMul = v; else scaling.miniBossArmorMul = v; }));
            return;
        }
        sub.add(e.startBooleanToggle(t("level_editor.custom_stats"), m.customStats)
                .setDefaultValue(false)
                .setTooltip(TooltipWrap.wrap(t("level_editor.custom_stats.tooltip")))
                .setSaveConsumer(v -> m.customStats = v)
                .build());
        sub.add(statField(e, "level_editor.stat_health", m.health, v -> m.health = v));
        sub.add(statField(e, "level_editor.stat_damage", m.damage, v -> m.damage = v));
        sub.add(statField(e, "level_editor.stat_armor", m.armor, v -> m.armor = v));
        sub.add(statField(e, "level_editor.stat_speed", m.speed, v -> m.speed = v));
        sub.add(e.startDoubleField(t("level_editor.stat_armor_flat"), m.armorFlat)
                .setDefaultValue(m.armorFlat)
                .setMin(0)
                .setMax(1000)
                .setTooltip(TooltipWrap.wrap(t("level_editor.stat.tooltip")))
                .setSaveConsumer(v -> m.armorFlat = v)
                .build());
        sub.add(e.startBooleanToggle(t("level_editor.custom_quality"), m.customQuality)
                .setDefaultValue(false)
                .setTooltip(TooltipWrap.wrap(t("level_editor.custom_quality.tooltip")))
                .setSaveConsumer(v -> m.customQuality = v)
                .build());
        sub.add(e.startDoubleField(t("level_editor.quality_bonus"), m.quality)
                .setDefaultValue(m.quality)
                .setMin(-90)
                .setMax(100000)
                .setTooltip(TooltipWrap.wrap(t("level_editor.quality_bonus.tooltip")))
                .setSaveConsumer(v -> m.quality = v)
                .build());
        sub.add(e.startDoubleField(t("level_editor.high_quality_bonus"), m.highQuality)
                .setDefaultValue(m.highQuality)
                .setMin(-90)
                .setMax(100000)
                .setTooltip(TooltipWrap.wrap(t("level_editor.high_quality_bonus.tooltip")))
                .setSaveConsumer(v -> m.highQuality = v)
                .build());
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> bossField(
            ConfigEntryBuilder e, String key, double value, double def, java.util.function.DoubleConsumer setter) {
        return e.startDoubleField(t(key), value)
                .setDefaultValue(def)
                .setMin(0)
                .setMax(100000)
                .setTooltip(TooltipWrap.wrap(t("level_editor.boss_stat.tooltip")))
                .setSaveConsumer(setter::accept)
                .build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> statField(
            ConfigEntryBuilder e, String key, double value, java.util.function.DoubleConsumer setter) {
        return e.startDoubleField(t(key), value)
                .setDefaultValue(value)
                .setMin(-90)
                .setMax(100000)
                .setTooltip(TooltipWrap.wrap(t("level_editor.stat.tooltip")))
                .setSaveConsumer(setter::accept)
                .build();
    }

    /** Input of a boss multiplier (0.1 - 100, 1.0 = unchanged). */
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> multiplierField(
            ConfigEntryBuilder e, String key, double value, double def, java.util.function.DoubleConsumer setter) {
        return e.startDoubleField(t(key), value)
                .setDefaultValue(def)
                .setMin(ScalingSettings.MIN_BOSS_MULTIPLIER)
                .setMax(ScalingSettings.MAX_BOSS_MULTIPLIER)
                .setTooltip(TooltipWrap.wrap(t("level_editor.boss_mult.tooltip")))
                .setSaveConsumer(setter::accept)
                .build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> percentField(
            String key, double value, double def, java.util.function.DoubleConsumer setter) {
        return ConfigEntryBuilder.create().startDoubleField(t(key), value)
                .setDefaultValue(def)
                .setMin(0)
                .setMax(100000)
                .setTooltip(TooltipWrap.wrap(t("scaling.tooltip")))
                .setSaveConsumer(setter::accept)
                .build();
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    private static void save(DifficultyConfig cfg, List<LevelModel> levels,
                             Set<Integer> existingLevels, ScalingModel scaling) {
        JsonArray levelsOut = new JsonArray();
        Set<Integer> usedLevels = new HashSet<>();
        for (LevelModel m : levels) {
            if (m.delete) {
                continue;
            }
            if (m.isNew && (!m.enable || existingLevels.contains(m.level))) {
                continue;
            }
            if (m.original == null && !m.isNew && m.title.isEmpty() && m.icon.isEmpty() && m.color.isEmpty() && !m.customStats && !m.customQuality) {
                continue; // untouched built-in level: nothing to store
            }
            if (!usedLevels.add(m.level)) {
                continue;
            }
            levelsOut.add(levelToJson(m));
        }
        // keep the levels sorted
        List<JsonObject> sorted = new ArrayList<>();
        for (JsonElement e : levelsOut) {
            sorted.add(e.getAsJsonObject());
        }
        sorted.sort((a, b) -> Integer.compare(a.get("level").getAsInt(), b.get("level").getAsInt()));
        JsonArray levelsSorted = new JsonArray();
        for (JsonObject o : sorted) {
            levelsSorted.add(o);
        }

        JsonObject scalingPatch = new JsonObject();
        scalingPatch.addProperty("scaling_mode", scaling.linear ? "linear" : "compound");
        scalingPatch.addProperty("health_percent_per_level", scaling.health);
        scalingPatch.addProperty("damage_percent_per_level", scaling.damage);
        scalingPatch.addProperty("armor_percent_per_level", scaling.armor);
        scalingPatch.addProperty("speed_percent_per_level", scaling.speed);
        scalingPatch.addProperty("armor_flat_per_level", scaling.armorFlat);
        scalingPatch.addProperty("mini_boss_health_percent", scaling.miniBossHealth);
        scalingPatch.addProperty("mini_boss_armor_percent", scaling.miniBossArmor);
        scalingPatch.addProperty("mini_boss_damage_percent", scaling.miniBossDamage);
        scalingPatch.addProperty("boss_health_percent", scaling.bossHealth);
        scalingPatch.addProperty("boss_armor_percent", scaling.bossArmor);
        scalingPatch.addProperty("boss_damage_percent", scaling.bossDamage);
        scalingPatch.addProperty("raid_difficulty_level", scaling.raidLevel);
        scalingPatch.addProperty("elite_enabled", scaling.eliteEnabled);
        scalingPatch.addProperty("elite_min_level", scaling.eliteMinLevel);
        scalingPatch.addProperty("elite_base_chance", scaling.eliteBaseChance);
        scalingPatch.addProperty("elite_chance_per_level", scaling.eliteChancePerLevel);
        scalingPatch.addProperty("elite_max_affixes", scaling.eliteMaxAffixes);
        scalingPatch.addProperty("elite_max_per_level", scaling.eliteMaxPerLevel);
        scalingPatch.addProperty("elite_particles", scaling.eliteParticles);
        scalingPatch.addProperty("elite_show_affix_names", scaling.eliteShowNames);
        scalingPatch.addProperty("scaling_soft_cap_level", scaling.softCap);
        scalingPatch.addProperty("diminishing_factor", scaling.diminishing);
        scalingPatch.addProperty("boss_health_multiplier", scaling.bossHealthMul);
        scalingPatch.addProperty("boss_damage_multiplier", scaling.bossDamageMul);
        scalingPatch.addProperty("boss_armor_multiplier", scaling.bossArmorMul);
        scalingPatch.addProperty("mini_boss_health_multiplier", scaling.miniBossHealthMul);
        scalingPatch.addProperty("mini_boss_damage_multiplier", scaling.miniBossDamageMul);
        scalingPatch.addProperty("mini_boss_armor_multiplier", scaling.miniBossArmorMul);
        scalingPatch.addProperty("level_cap_enabled", scaling.capEnabled);
        scalingPatch.addProperty("dimension_level_bonus", scaling.capBonus);
        scalingPatch.addProperty("max_region_level", scaling.maxLevel);
        JsonObject capsOut = new JsonObject();
        for (String entry : scaling.dimensionCaps) {
            int eq = entry.lastIndexOf('=');
            if (eq > 0) {
                try {
                    capsOut.addProperty(entry.substring(0, eq).trim(), Integer.parseInt(entry.substring(eq + 1).trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        scalingPatch.add("dimension_level_caps", capsOut);
        JsonObject lootOut = new JsonObject();
        lootOut.addProperty("enabled", scaling.lootEnabled);
        lootOut.addProperty("chests", scaling.lootChests);
        lootOut.addProperty("mobs", scaling.lootMobs);
        lootOut.addProperty("min_level", scaling.lootMinLevel);
        lootOut.addProperty("chest_extra_roll_chance_percent_per_level", scaling.lootRollChance);
        lootOut.addProperty("chest_max_extra_rolls", scaling.lootMaxRolls);
        lootOut.addProperty("chest_count_percent_per_level", scaling.lootCount);
        lootOut.addProperty("chest_enchant_chance_percent_per_level", scaling.lootEnchant);
        lootOut.addProperty("mob_extra_drop_chance_percent_per_level", scaling.lootMobDrop);
        lootOut.addProperty("mob_max_extra_drops", scaling.lootMaxExtraDrops);
        lootOut.addProperty("mob_count_percent_per_level", scaling.lootMobCount);
        lootOut.addProperty("mob_enchant_chance_percent_per_level", scaling.lootMobEnchant);
        scalingPatch.add("loot", lootOut);

        boolean ok = cfg.writeRegionsAndLevels(null, levelsSorted, scalingPatch);
        // no world running: nothing else would reload it
        if (ok && Minecraft.getInstance().getSingleplayerServer() == null) {
            cfg.loadConfig();
        }
    }

    private static JsonObject levelToJson(LevelModel m) {
        JsonObject o = new JsonObject();
        o.addProperty("level", m.level);
        if (!m.title.isEmpty()) {
            o.addProperty("title", m.title);
        }
        if (!m.icon.isEmpty()) {
            o.addProperty("icon", m.icon);
        }
        if (!m.color.isEmpty()) {
            o.addProperty("title_color", m.color);
        }
        if (m.customStats) {
            o.addProperty("health_bonus_percent", m.health);
            o.addProperty("damage_bonus_percent", m.damage);
            o.addProperty("armor_bonus_percent", m.armor);
            o.addProperty("speed_bonus_percent", m.speed);
            o.addProperty("armor_flat_bonus", m.armorFlat);
        }
        if (m.customQuality) {
            o.addProperty("quality_bonus_percent", m.quality);
            o.addProperty("high_quality_bonus_percent", m.highQuality);
        }
        copyUnknown(m.original, o);
        return o;
    }

    /** Keeps keys the editor does not know (comments, future fields) of the original entry. */
    private static void copyUnknown(JsonObject original, JsonObject target) {
        if (original == null) {
            return;
        }
        for (var entry : original.entrySet()) {
            if (!target.has(entry.getKey()) && !isEditorKey(entry.getKey())) {
                target.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean isEditorKey(String key) {
        return switch (key) {
            case "id", "region_name", "difficulty_title", "difficulty_level", "icon", "title_color",
                 "dimensions", "biomes", "biome_tags", "structures", "level", "title",
                 "health_bonus_percent", "damage_bonus_percent", "armor_bonus_percent",
                 "speed_bonus_percent", "armor_flat_bonus",
                 "quality_bonus_percent", "high_quality_bonus_percent", "loot" -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // JSON helpers
    // ------------------------------------------------------------------

    private static JsonObject readRoot(File file) {
        if (file == null || !file.exists()) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement e = GSON.fromJson(reader, JsonElement.class);
            if (e != null && e.isJsonObject()) {
                return e.getAsJsonObject();
            }
        } catch (Exception ignored) {
            // an unreadable file shows an empty editor; saving then refuses to overwrite it (see DifficultyConfig)
        }
        return new JsonObject();
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString().trim() : "";
    }

}
