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

/** In-game settings screen built with Cloth Config API. Only load this class when cloth_config is present. */
public final class HrdConfigScreen {

    private static final String K = "hardcore_region_difficulty.config.";

    private HrdConfigScreen() {}

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(t("title"))
                .setTransparentBackground(true)
                .setSavingRunnable(() -> {
                    ClientConfig.SPEC.save();
                    QualityConfig.SPEC.save();
                    ForgeConfig.SPEC.save();
                });
        ConfigEntryBuilder entries = builder.entryBuilder();

        // ---- General: whole difficulty evaluation -----------------------------
        ConfigCategory general = builder.getOrCreateCategory(t("category.general"));
        general.addEntry(entries.startBooleanToggle(t("difficulty_enabled"), ForgeConfig.ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(t("difficulty_enabled.tooltip"), t("difficulty_warning").copy().withStyle(ChatFormatting.RED))
                .setSaveConsumer(ForgeConfig.ENABLED::set)
                .build());
        general.addEntry(entries.startTextDescription(t("difficulty_warning").copy().withStyle(ChatFormatting.RED)).build());

        // ---- Difficulty title -------------------------------------------------
        ConfigCategory title = builder.getOrCreateCategory(t("category.title"));
        title.addEntry(entries.startBooleanToggle(t("show_region_title"), ClientConfig.SHOW_REGION_TITLE.get())
                .setDefaultValue(true)
                .setTooltip(t("show_region_title.tooltip"))
                .setSaveConsumer(ClientConfig.SHOW_REGION_TITLE::set)
                .build());
        title.addEntry(entries.startBooleanToggle(t("show_boss_title"), ClientConfig.SHOW_BOSS_TITLE.get())
                .setDefaultValue(true)
                .setTooltip(t("show_boss_title.tooltip"))
                .setSaveConsumer(ClientConfig.SHOW_BOSS_TITLE::set)
                .build());

        // ---- Equipment quality ------------------------------------------------
        ConfigCategory quality = builder.getOrCreateCategory(t("category.quality"));
        quality.addEntry(entries.startTextDescription(t("quality_warning").copy().withStyle(ChatFormatting.RED)).build());
        quality.addEntry(entries.startTextDescription(t("quality_note")).build());
        quality.addEntry(entries.startBooleanToggle(t("quality_enabled"), QualityConfig.ENABLE_QUALITY.get())
                .setDefaultValue(true)
                .setTooltip(t("quality_enabled.tooltip"))
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
        quality.addEntry(entries.startIntSlider(t("forge_xp_cost"), QualityConfig.XP_COST.get(), 0, 30)
                .setDefaultValue(3)
                .setSaveConsumer(QualityConfig.XP_COST::set)
                .build());
        quality.addEntry(entries.startIntSlider(t("forge_material_cost"), QualityConfig.MATERIAL_COST.get(), 1, 64)
                .setDefaultValue(1)
                .setSaveConsumer(QualityConfig.MATERIAL_COST::set)
                .build());

        // ---- Mod compatibility (stored in hardcore_region_difficulty.json) -----
        ConfigCategory compat = builder.getOrCreateCategory(t("category.compat"));
        compat.addEntry(entries.startTextDescription(t("compat_note")).build());
        DifficultyConfig difficultyConfig = DifficultyConfig.getInstance();
        final boolean currentDdCataclysm = difficultyConfig != null
                && difficultyConfig.getCompatSettings().isDeeperDarkerCataclysm();
        compat.addEntry(entries.startBooleanToggle(t("compat_dd_cataclysm"), currentDdCataclysm)
                .setDefaultValue(false)
                .setTooltip(t("compat_dd_cataclysm.tooltip"))
                .setErrorSupplier(value -> value && !DeeperDarkerCataclysmCompat.modsPresent()
                        ? Optional.of(t("compat_missing")) : Optional.empty())
                .setSaveConsumer(value -> {
                    if (difficultyConfig != null && value != currentDdCataclysm) {
                        difficultyConfig.setDeeperDarkerCataclysm(value);
                    }
                })
                .build());

        return builder.build();
    }
}
