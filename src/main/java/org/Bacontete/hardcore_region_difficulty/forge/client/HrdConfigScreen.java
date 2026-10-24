package org.Bacontete.hardcore_region_difficulty.forge.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.forge.compat.DeeperDarkerCataclysmCompat;

import java.util.Optional;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;
import org.Bacontete.hardcore_region_difficulty.forge.ClientConfig;
import org.Bacontete.hardcore_region_difficulty.forge.ForgeConfig;

/**
 * In-game settings screen built with Cloth Config API. Only load this class when cloth_config is present.
 * Three tabs: General (switches, titles, mod compatibility), Difficulty (regions and levels) and Quality.
 */
public final class HrdConfigScreen {

    private static final String K = "hardcore_region_difficulty.config.";

    private HrdConfigScreen() {}

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    private static void addBoss(ConfigEntryBuilder entries,
                                me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder group, String id,
                                net.minecraftforge.common.ForgeConfigSpec.DoubleValue health,
                                net.minecraftforge.common.ForgeConfigSpec.DoubleValue damage,
                                double defaultHealth, double defaultDamage) {
        var sub = entries.startSubCategory(Component.translatable("entity.minecraft." + id)).setExpanded(false);
        sub.add(entries.startDoubleField(t("boss.health"), health.get())
                .setDefaultValue(defaultHealth).setMin(0.0D).setMax(100000.0D)
                .setTooltip(TooltipWrap.wrap(t("boss.health.tooltip")))
                .setSaveConsumer(health::set)
                .build());
        sub.add(entries.startDoubleField(t("boss.damage"), damage.get())
                .setDefaultValue(defaultDamage).setMin(0.1D).setMax(50.0D)
                .setTooltip(TooltipWrap.wrap(t("boss.damage.tooltip")))
                .setSaveConsumer(damage::set)
                .build());
        group.add(sub.build());
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(t("title"))
                .setTransparentBackground(true);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(t("category.general"));
        ConfigCategory difficulty = builder.getOrCreateCategory(t("category.difficulty"));
        ConfigCategory scanTab = builder.getOrCreateCategory(t("category.scan"));
        ConfigCategory shareTab = builder.getOrCreateCategory(t("category.share"));
        ConfigCategory whitelist = builder.getOrCreateCategory(t("category.whitelist"));
        ConfigCategory quality = builder.getOrCreateCategory(t("category.quality"));
        ConfigCategory enchantments = builder.getOrCreateCategory(t("category.enchantments"));

        // ---- General: master switch, titles, mod compatibility ------------------
        general.addEntry(entries.startBooleanToggle(t("difficulty_enabled"), ForgeConfig.ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("difficulty_enabled.tooltip"), t("difficulty_warning").copy().withStyle(ChatFormatting.RED)))
                .setSaveConsumer(ForgeConfig.ENABLED::set)
                .build());
        general.addEntry(entries.startTextDescription(t("difficulty_warning").copy().withStyle(ChatFormatting.RED)).build());
        general.addEntry(entries.startBooleanToggle(t("show_region_title"), ClientConfig.SHOW_REGION_TITLE.get())
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("show_region_title.tooltip")))
                .setSaveConsumer(ClientConfig.SHOW_REGION_TITLE::set)
                .build());
        general.addEntry(entries.startBooleanToggle(t("show_boss_title"), ClientConfig.SHOW_BOSS_TITLE.get())
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("show_boss_title.tooltip")))
                .setSaveConsumer(ClientConfig.SHOW_BOSS_TITLE::set)
                .build());

        // Stored in hardcore_region_difficulty.json (mod_compat_settings)
        DifficultyConfig difficultyConfig = DifficultyConfig.getInstance();
        final boolean currentDdCataclysm = difficultyConfig != null
                && difficultyConfig.getCompatSettings().isDeeperDarkerCataclysm();
        general.addEntry(entries.startBooleanToggle(t("compat_dd_cataclysm"), currentDdCataclysm)
                .setDefaultValue(false)
                .setTooltip(TooltipWrap.wrap(t("compat_dd_cataclysm.tooltip"), t("compat_note")))
                .setErrorSupplier(value -> value && !DeeperDarkerCataclysmCompat.modsPresent()
                        ? Optional.of(t("compat_missing")) : Optional.empty())
                .setSaveConsumer(value -> {
                    if (difficultyConfig != null && value != currentDdCataclysm) {
                        difficultyConfig.setDeeperDarkerCataclysm(value);
                    }
                })
                .build());

        // ---- Difficulty: regions and levels (edits hardcore_region_difficulty.json) ----
        Runnable regionEditorSave = RegionEditorTabs.add(entries, difficulty);

        // ---- Scan: dimensions and structures found by the scan, level and mob stats of each ----
        Runnable scanSave = ScanEditorTabs.add(entries, scanTab);
        Runnable shareSave = ShareEditorTabs.add(entries, shareTab);

        // ---- Whitelist tab, first the vanilla bosses (health / damage), stored in the common toml ----
        whitelist.addEntry(entries.startTextDescription(t("boss.note")).build());
        var bossGroup = entries.startSubCategory(t("boss.group")).setExpanded(true);
        addBoss(entries, bossGroup, "ender_dragon", ForgeConfig.DRAGON_HEALTH, ForgeConfig.DRAGON_DAMAGE_MULTIPLIER, 1024.0D, 2.0D);
        addBoss(entries, bossGroup, "wither", ForgeConfig.WITHER_HEALTH, ForgeConfig.WITHER_DAMAGE_MULTIPLIER, 512.0D, 2.0D);
        addBoss(entries, bossGroup, "warden", ForgeConfig.WARDEN_HEALTH, ForgeConfig.WARDEN_DAMAGE_MULTIPLIER, 0.0D, 1.0D);
        whitelist.addEntry(bossGroup.build());

        // ---- Whitelist: which mobs are scaled (global and per region) ----
        Runnable whitelistSave = WhitelistTabs.add(entries, whitelist);

        // ---- Quality: settings, then tier limits and one group per item category ----
        quality.addEntry(entries.startTextDescription(t("quality_warning").copy().withStyle(ChatFormatting.RED)).build());
        quality.addEntry(entries.startBooleanToggle(t("quality_enabled"), QualityConfig.ENABLE_QUALITY.get())
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("quality_enabled.tooltip"), t("quality_note")))
                .setSaveConsumer(QualityConfig.ENABLE_QUALITY::set)
                .build());
        quality.addEntry(entries.startBooleanToggle(t("roll_in_loot"), QualityConfig.ROLL_IN_LOOT.get())
                .setDefaultValue(true)
                .setSaveConsumer(QualityConfig.ROLL_IN_LOOT::set)
                .build());
        quality.addEntry(entries.startBooleanToggle(t("roll_in_inventory"), QualityConfig.ROLL_IN_INVENTORY.get())
                .setDefaultValue(true)
                .setSaveConsumer(QualityConfig.ROLL_IN_INVENTORY::set)
                .build());
        quality.addEntry(entries.startIntSlider(t("difficulty_bonus"),
                        (int) Math.round(QualityConfig.DIFFICULTY_QUALITY_BONUS.get() * 100.0D), 0, 100)
                .setDefaultValue(10)
                .setSaveConsumer(v -> QualityConfig.DIFFICULTY_QUALITY_BONUS.set(v / 100.0D))
                .build());
        quality.addEntry(entries.startIntSlider(t("low_quality_reduction"),
                        (int) Math.round(QualityConfig.LOW_QUALITY_REDUCTION.get() * 100.0D), 0, 100)
                .setDefaultValue(85)
                .setTooltip(TooltipWrap.wrap(t("low_quality_reduction.tooltip")))
                .setSaveConsumer(v -> QualityConfig.LOW_QUALITY_REDUCTION.set(v / 100.0D))
                .build());
        quality.addEntry(entries.startDoubleField(t("high_tier_multiplier"), QualityConfig.HIGH_TIER_BONUS_MULTIPLIER.get())
                .setDefaultValue(1.5D)
                .setMin(1.0D)
                .setMax(20.0D)
                .setTooltip(TooltipWrap.wrap(t("high_tier_multiplier.tooltip")))
                .setSaveConsumer(QualityConfig.HIGH_TIER_BONUS_MULTIPLIER::set)
                .build());
        quality.addEntry(entries.startBooleanToggle(t("unlock_high_qualities"), QualityConfig.UNLOCK_HIGH_QUALITIES.get())
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("unlock_high_qualities.tooltip")))
                .setSaveConsumer(QualityConfig.UNLOCK_HIGH_QUALITIES::set)
                .build());
        quality.addEntry(entries.startIntSlider(t("forge_xp_cost"), QualityConfig.XP_COST.get(), 0, 30)
                .setDefaultValue(3)
                .setSaveConsumer(QualityConfig.XP_COST::set)
                .build());
        quality.addEntry(entries.startIntSlider(t("forge_material_cost"), QualityConfig.MATERIAL_COST.get(), 1, 64)
                .setDefaultValue(1)
                .setSaveConsumer(QualityConfig.MATERIAL_COST::set)
                .build());
        quality.addEntry(entries.startDoubleField(t("tier_standard"), QualityConfig.TIER_STANDARD_CHANCE.get())
                .setDefaultValue(13.0D)
                .setMin(0.0D)
                .setMax(1000.0D)
                .setTooltip(TooltipWrap.wrap(t("tier_standard.tooltip")))
                .setSaveConsumer(QualityConfig.TIER_STANDARD_CHANCE::set)
                .build());
        quality.addEntry(entries.startDoubleField(t("tier_bad"), QualityConfig.TIER_BAD_CHANCE.get())
                .setDefaultValue(14.0D)
                .setMin(0.0D)
                .setMax(1000.0D)
                .setTooltip(TooltipWrap.wrap(t("tier_bad.tooltip")))
                .setSaveConsumer(QualityConfig.TIER_BAD_CHANCE::set)
                .build());
        quality.addEntry(entries.startDoubleField(t("tier_bad_falloff"), QualityConfig.TIER_BAD_FALLOFF.get())
                .setDefaultValue(0.5D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(TooltipWrap.wrap(t("tier_bad_falloff.tooltip")))
                .setSaveConsumer(QualityConfig.TIER_BAD_FALLOFF::set)
                .build());
        quality.addEntry(entries.startDoubleField(t("tier_good"), QualityConfig.TIER_GOOD_CHANCE.get())
                .setDefaultValue(9.0D)
                .setMin(0.0D)
                .setMax(1000.0D)
                .setTooltip(TooltipWrap.wrap(t("tier_good.tooltip")))
                .setSaveConsumer(QualityConfig.TIER_GOOD_CHANCE::set)
                .build());
        quality.addEntry(entries.startDoubleField(t("tier_good_falloff"), QualityConfig.TIER_GOOD_FALLOFF.get())
                .setDefaultValue(0.4D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(TooltipWrap.wrap(t("tier_good_falloff.tooltip")))
                .setSaveConsumer(QualityConfig.TIER_GOOD_FALLOFF::set)
                .build());
        quality.addEntry(entries.startTextDescription(t("quality_file_note")).build());
        Runnable qualityEditorSave = QualityEditorTabs.add(entries, quality);

        // ---- Enchantments of the mod: on / off ----
        enchantments.addEntry(entries.startTextDescription(t("enchant_warning").copy().withStyle(ChatFormatting.RED)).build());
        enchantments.addEntry(entries.startBooleanToggle(t("enchant_all"), ForgeConfig.ENCHANTMENTS_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(TooltipWrap.wrap(t("enchant_all.tooltip"), t("enchant_warning").copy().withStyle(ChatFormatting.RED)))
                .setSaveConsumer(ForgeConfig.ENCHANTMENTS_ENABLED::set)
                .build());
        for (String id : ForgeConfig.ENCHANT_IDS) {
            var toggle = ForgeConfig.ENCHANT_TOGGLES.get(id);
            String base = "enchantment.hardcore_region_difficulty." + id;
            enchantments.addEntry(entries.startBooleanToggle(Component.translatable(base), toggle.get())
                    .setDefaultValue(true)
                    .setTooltip(TooltipWrap.wrap(Component.translatable(base + ".desc"),
                            t("enchant_warning").copy().withStyle(ChatFormatting.RED)))
                    .setSaveConsumer(toggle::set)
                    .build());
        }

        builder.setSavingRunnable(() -> {
            ClientConfig.SPEC.save();
            QualityConfig.SPEC.save();
            ForgeConfig.SPEC.save();
            qualityEditorSave.run();
            regionEditorSave.run();
            whitelistSave.run(); // after the region editor: it re-reads the file and sets the whitelists
            scanSave.run(); // it re-reads the file and only touches the smart-auto levels and the scan_* regions
            shareSave.run(); // last: an imported setup replaces the files
        });

        return builder.build();
    }
}
