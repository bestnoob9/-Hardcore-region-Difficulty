package org.Bacontete.hardcore_region_difficulty.forge.client;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyRegion;
import org.Bacontete.hardcore_region_difficulty.common.config.MobEntry;
import org.Bacontete.hardcore_region_difficulty.common.elite.EliteMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "Whitelist" tab: which mobs are scaled by the difficulty, and how, mob by mob.
 * One global list and one list per region. Every entry is a mob pattern (id, #tag, ~regex, *, !not) with
 * optional details (level, health, damage, armor, flat armor, speed, knockback resistance, follow range).
 * Details left empty = automatic. Stored in whitelist_entities (scaling_settings) and entity_whitelist (regions)
 * of hardcore_region_difficulty.json. Only load this class when cloth_config is present.
 */
public final class WhitelistTabs {

    private static final String K = "hardcore_region_difficulty.config.";
    private static final int NEW_GLOBAL_SLOTS = 3;
    private static final int NEW_REGION_SLOTS = 1;

    private WhitelistTabs() {}

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    private static final class EntryModel {
        boolean delete;
        String pattern = "";
        String level = "";
        String health = "";
        String damage = "";
        String armor = "";
        String armorFlat = "";
        String speed = "";
        String knockback = "";
        String follow = "";

        static EntryModel of(MobEntry e) {
            EntryModel m = new EntryModel();
            m.pattern = e.getPattern();
            m.level = e.getLevel() == null ? "" : String.valueOf(e.getLevel());
            m.health = text(e.getHealthBonusPercent());
            m.damage = text(e.getDamageBonusPercent());
            m.armor = text(e.getArmorBonusPercent());
            m.armorFlat = text(e.getArmorFlatBonus());
            m.speed = text(e.getSpeedBonusPercent());
            m.knockback = text(e.getKnockbackResistance());
            m.follow = text(e.getFollowRangeBonusPercent());
            return m;
        }

        MobEntry toEntry() {
            Integer lvl = null;
            try {
                if (!level.isBlank()) {
                    lvl = Integer.valueOf(level.trim());
                }
            } catch (NumberFormatException ignored) {
                // invalid: automatic
            }
            return new MobEntry(pattern, lvl, parse(health), parse(damage), parse(armor), parse(armorFlat),
                    parse(speed), parse(knockback), parse(follow));
        }
    }

    /** All entries of one list (the global one or one region), saved together. */
    private static final class ListModel {
        final List<EntryModel> entries = new ArrayList<>();

        List<MobEntry> toEntries() {
            List<MobEntry> out = new ArrayList<>();
            for (EntryModel m : entries) {
                if (m.delete || m.pattern.isBlank()) {
                    continue;
                }
                out.add(m.toEntry());
            }
            return out;
        }
    }

    private static String text(Double v) {
        if (v == null) {
            return "";
        }
        return v == Math.rint(v) ? String.valueOf(v.longValue()) : String.valueOf(v);
    }

    private static Double parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Optional<Component> numberError(String s) {
        if (s == null || s.isBlank() || parse(s) != null) {
            return Optional.empty();
        }
        return Optional.of(t("whitelist.number_invalid"));
    }

    private static Optional<Component> levelError(String s) {
        if (s == null || s.isBlank()) {
            return Optional.empty();
        }
        try {
            return Integer.parseInt(s.trim()) >= 1 ? Optional.empty() : Optional.of(t("whitelist.level_invalid"));
        } catch (NumberFormatException e) {
            return Optional.of(t("whitelist.level_invalid"));
        }
    }

    /** @return the runnable to call from the screen's saving runnable (writes the file) */
    public static Runnable add(ConfigEntryBuilder entries, ConfigCategory tab) {
        Minecraft mc = Minecraft.getInstance();
        boolean editable = mc.level == null || mc.getSingleplayerServer() != null;
        DifficultyConfig cfg = DifficultyConfig.getInstance();
        if (!editable || cfg == null) {
            tab.addEntry(entries.startTextDescription(
                    t("region_editor.remote").copy().withStyle(ChatFormatting.RED)).build());
            return () -> {};
        }

        tab.addEntry(entries.startTextDescription(t("whitelist.note")).build());

        ListModel global = new ListModel();
        for (MobEntry e : cfg.getScalingSettings().getWhitelistEntries()) {
            global.entries.add(EntryModel.of(e));
        }
        SubCategoryBuilder globalGroup = entries.startSubCategory(t("whitelist.global")).setExpanded(true);
        addList(entries, globalGroup, global, NEW_GLOBAL_SLOTS);
        tab.addEntry(globalGroup.build());

        Map<String, ListModel> perRegion = new LinkedHashMap<>();
        Map<String, Boolean> regionElite = new LinkedHashMap<>();
        SubCategoryBuilder regionsGroup = entries.startSubCategory(t("whitelist.regions")).setExpanded(false);
        regionsGroup.add(entries.startTextDescription(t("whitelist.regions.note")).build());
        for (DifficultyRegion region : cfg.getRegionList()) {
            ListModel list = new ListModel();
            for (MobEntry e : region.getMobEntries()) {
                list.entries.add(EntryModel.of(e));
            }
            perRegion.put(region.getId(), list);
            Component title = Component.literal(region.getRegionName() + "  [" + region.getId() + "]  Lv "
                    + region.getDifficultyLevel()
                    + (list.entries.isEmpty() ? "" : "  (" + list.entries.size() + ")"));
            SubCategoryBuilder sub = entries.startSubCategory(title).setExpanded(false);
            sub.add(entries.startEnumSelector(t("elite.region"), EliteMode.class, EliteMode.of(region.getEliteEnabled()))
                    .setDefaultValue(EliteMode.FOLLOW)
                    .setEnumNameProvider(v -> t("elite.mode." + ((EliteMode) v).name().toLowerCase(java.util.Locale.ROOT)))
                    .setTooltip(TooltipWrap.wrap(t("elite.region.tooltip")))
                    .setSaveConsumer(v -> regionElite.put(region.getId(), v.toBoolean()))
                    .build());
            addList(entries, sub, list, NEW_REGION_SLOTS);
            regionsGroup.add(sub.build());
        }
        tab.addEntry(regionsGroup.build());

        return () -> {
            Map<String, List<MobEntry>> regionLists = new LinkedHashMap<>();
            perRegion.forEach((id, list) -> regionLists.put(id, list.toEntries()));
            boolean ok = cfg.writeWhitelists(global.toEntries(), regionLists, regionElite);
            // no world running: nothing else would reload it
            if (ok && Minecraft.getInstance().getSingleplayerServer() == null) {
                cfg.loadConfig();
            }
        };
    }

    /** One group per existing mob entry, then empty "+ New mob" slots. */
    private static void addList(ConfigEntryBuilder e, SubCategoryBuilder parent, ListModel list, int newSlots) {
        List<EntryModel> existing = new ArrayList<>(list.entries);
        for (EntryModel m : existing) {
            Component title = Component.literal(m.pattern + detailMarker(m));
            SubCategoryBuilder sub = e.startSubCategory(title).setExpanded(false);
            addEntryFields(e, sub, m, false);
            parent.add(sub.build());
        }
        for (int slot = 1; slot <= newSlots; slot++) {
            EntryModel m = new EntryModel();
            list.entries.add(m);
            SubCategoryBuilder sub = e.startSubCategory(
                    Component.translatable(K + "whitelist.new", slot).withStyle(ChatFormatting.GREEN)).setExpanded(false);
            addEntryFields(e, sub, m, true);
            parent.add(sub.build());
        }
    }

    private static String detailMarker(EntryModel m) {
        boolean any = !(m.level.isBlank() && m.health.isBlank() && m.damage.isBlank() && m.armor.isBlank()
                && m.armorFlat.isBlank() && m.speed.isBlank() && m.knockback.isBlank() && m.follow.isBlank());
        return any ? "  *" : "";
    }

    private static void addEntryFields(ConfigEntryBuilder e, SubCategoryBuilder sub, EntryModel m, boolean isNew) {
        if (isNew) {
            sub.add(e.startStrField(t("whitelist.entry.pattern"), "")
                    .setDefaultValue("")
                    .setTooltip(TooltipWrap.wrap(t("whitelist.entry.pattern.tooltip"), t("whitelist.syntax")))
                    .setSaveConsumer(s -> m.pattern = s.trim())
                    .build());
        } else {
            sub.add(e.startBooleanToggle(t("whitelist.entry.delete"), false)
                    .setDefaultValue(false)
                    .setTooltip(TooltipWrap.wrap(t("whitelist.entry.delete.tooltip")))
                    .setSaveConsumer(v -> m.delete = v)
                    .build());
            sub.add(e.startStrField(t("whitelist.entry.pattern"), m.pattern)
                    .setDefaultValue(m.pattern)
                    .setTooltip(TooltipWrap.wrap(t("whitelist.entry.pattern.tooltip"), t("whitelist.syntax")))
                    .setSaveConsumer(s -> m.pattern = s.trim())
                    .build());
        }
        sub.add(e.startTextDescription(t("whitelist.entry.details")).build());
        sub.add(e.startStrField(t("whitelist.entry.level"), m.level)
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("whitelist.entry.level.tooltip")))
                .setErrorSupplier(WhitelistTabs::levelError)
                .setSaveConsumer(s -> m.level = s.trim())
                .build());
        sub.add(numberField(e, "whitelist.entry.health", m.health, v -> m.health = v));
        sub.add(numberField(e, "whitelist.entry.damage", m.damage, v -> m.damage = v));
        sub.add(numberField(e, "whitelist.entry.armor", m.armor, v -> m.armor = v));
        sub.add(numberField(e, "whitelist.entry.armor_flat", m.armorFlat, v -> m.armorFlat = v));
        sub.add(numberField(e, "whitelist.entry.speed", m.speed, v -> m.speed = v));
        sub.add(numberField(e, "whitelist.entry.knockback", m.knockback, v -> m.knockback = v));
        sub.add(numberField(e, "whitelist.entry.follow", m.follow, v -> m.follow = v));
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> numberField(
            ConfigEntryBuilder e, String key, String value, java.util.function.Consumer<String> setter) {
        return e.startStrField(t(key), value)
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t(key + ".tooltip"), t("whitelist.entry.empty_note")))
                .setErrorSupplier(WhitelistTabs::numberError)
                .setSaveConsumer(s -> setter.accept(s.trim()))
                .build();
    }
}
