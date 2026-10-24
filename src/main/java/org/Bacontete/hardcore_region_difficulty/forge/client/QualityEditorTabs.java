package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityCategory;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityDefinition;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityRegistry;
import org.Bacontete.hardcore_region_difficulty.forge.NetworkHandler;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

/**
 * In-game editor for config/hardcore_region_difficulty-qualities.json, built from Cloth Config entries.
 * One collapsible group per item category inside the Quality tab; every quality is a collapsible block (delete / color / name / chance / effects) and
 * there are empty "new quality" blocks to add qualities. Only load this class when cloth_config is present.
 * <p>
 * Editing is only offered in the main menu and in single player: on a remote server the server's file decides.
 */
public final class QualityEditorTabs {

    private static final String K = "hardcore_region_difficulty.config.";
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z0-9_]{1,40}");
    private static final int NEW_SLOTS = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private QualityEditorTabs() {}

    /** One editable effect: JSON key, flat value (else percent) and where to read the current value. */
    private record Effect(String key, boolean flat, ToDoubleFunction<QualityDefinition> getter) {}

    /** Mutable copy of one quality that the entries write into when the screen is saved. */
    private static final class Model {
        boolean isNew;
        boolean delete;
        String id = "";
        String name = "";
        int rgb = 0xFFFFFF;
        int tier;
        /** Chance in % (share of the category), shown / edited instead of a weight. */
        double chance;
        QualityDefinition.WeightGroup group = QualityDefinition.WeightGroup.NEUTRAL;
        int unlock;
        double[] effects;

        Model(int effectCount) {
            this.effects = new double[effectCount];
        }
    }

    /** Tier limits and strict mode of the file; the entries of the quality tab write into it. */
    private static final class Limits {
        boolean strict = QualityRegistry.isStrictLimits();
        int min = QualityRegistry.getTierMin();
        int max = QualityRegistry.getTierMax();
    }

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    private static double round(double v, int digits) {
        double f = Math.pow(10, digits);
        return Math.round(v * f) / f;
    }

    private static List<Effect> effectsFor(QualityCategory category) {
        List<Effect> list = new ArrayList<>();
        switch (category) {
            case WEAPON -> {
                list.add(new Effect("damage", false, QualityDefinition::getDamagePercent));
                list.add(new Effect("attack_speed", false, QualityDefinition::getSpeedPercent));
                list.add(new Effect("durability", false, QualityDefinition::getDurabilityPercent));
                list.add(new Effect("reach", true, QualityDefinition::getReach));
            }
            case TOOL -> {
                list.add(new Effect("damage", false, QualityDefinition::getDamagePercent));
                list.add(new Effect("attack_speed", false, QualityDefinition::getSpeedPercent));
                list.add(new Effect("durability", false, QualityDefinition::getDurabilityPercent));
                list.add(new Effect("mining_speed", false, QualityDefinition::getMiningPercent));
                list.add(new Effect("reach", true, QualityDefinition::getReach));
            }
            case ARMOR -> {
                list.add(new Effect("armor", false, QualityDefinition::getDamagePercent));
                list.add(new Effect("durability", false, QualityDefinition::getDurabilityPercent));
                list.add(new Effect("armor_bonus", true, QualityDefinition::getArmorBonus));
            }
            case RANGED -> {
                list.add(new Effect("arrow_damage", false, QualityDefinition::getDamagePercent));
                list.add(new Effect("draw_speed", false, QualityDefinition::getSpeedPercent));
                list.add(new Effect("durability", false, QualityDefinition::getDurabilityPercent));
            }
            case SHIELD -> {
                list.add(new Effect("durability", false, QualityDefinition::getDurabilityPercent));
                list.add(new Effect("armor_bonus", true, QualityDefinition::getArmorBonus));
                list.add(new Effect("move_speed", false, QualityDefinition::getMoveSpeedPercent));
                list.add(new Effect("hold_mining", false, QualityDefinition::getHoldMiningPercent));
            }
        }
        return list;
    }

    /**
     * Adds the tier limits and one collapsible group per item category to the given tab.
     * @return the runnable to call from the screen's saving runnable (writes the file and reloads)
     */
    public static Runnable add(ConfigEntryBuilder entries, ConfigCategory tab) {
        Minecraft mc = Minecraft.getInstance();
        boolean editable = mc.level == null || mc.getSingleplayerServer() != null;
        if (!editable) {
            tab.addEntry(entries.startTextDescription(
                    t("quality_editor.remote").copy().withStyle(ChatFormatting.RED)).build());
            return () -> {};
        }
        Map<QualityCategory, List<Model>> models = new EnumMap<>(QualityCategory.class);
        Limits limits = new Limits();

        SubCategoryBuilder limitsGroup = entries.startSubCategory(t("quality_editor.limits")).setExpanded(false);
        limitsGroup.add(entries.startTextDescription(t("quality_editor.limits_note")).build());
        limitsGroup.add(entries.startIntField(t("quality_editor.tier_min"), limits.min)
                .setDefaultValue(QualityRegistry.DEFAULT_TIER_MIN)
                .setMin(-50)
                .setMax(0)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.tier_min.tooltip")))
                .setSaveConsumer(v -> limits.min = v)
                .build());
        limitsGroup.add(entries.startIntField(t("quality_editor.tier_max"), limits.max)
                .setDefaultValue(QualityRegistry.DEFAULT_TIER_MAX)
                .setMin(0)
                .setMax(50)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.tier_max.tooltip")))
                .setSaveConsumer(v -> limits.max = v)
                .build());
        limitsGroup.add(entries.startBooleanToggle(t("quality_editor.strict"), limits.strict)
                .setDefaultValue(false)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.strict.tooltip")))
                .setSaveConsumer(v -> limits.strict = v)
                .build());
        tab.addEntry(limitsGroup.build());

        for (QualityCategory category : QualityCategory.values()) {
            SubCategoryBuilder group = entries.startSubCategory(t("category.quality_" + category.key()))
                    .setExpanded(false);
            group.add(entries.startTextDescription(t("quality_editor.note")).build());

            List<Effect> fields = effectsFor(category);
            List<Model> list = new ArrayList<>();
            models.put(category, list);
            Set<String> existing = new HashSet<>();
            for (QualityDefinition d : QualityRegistry.all(category)) {
                existing.add(d.name());
            }

            double weightTotal = 0.0D;
            for (QualityDefinition q : QualityRegistry.all(category)) {
                weightTotal += q.getWeight();
            }
            for (QualityDefinition d : QualityRegistry.all(category)) {
                Model m = new Model(fields.size());
                m.id = d.name();
                m.name = d.getCustomName() == null ? "" : d.getCustomName();
                m.rgb = d.getTextColor().getValue() & 0xFFFFFF;
                m.tier = d.getTier();
                m.chance = weightTotal > 0 ? round(d.getWeight() * 100.0D / weightTotal, 2) : 0.0D;
                m.group = d.getWeightGroup();
                m.unlock = d.getUnlockLevel();
                for (int i = 0; i < fields.size(); i++) {
                    Effect f = fields.get(i);
                    double v = f.getter().applyAsDouble(d);
                    m.effects[i] = f.flat() ? round(v, 3) : round(v * 100.0D, 3);
                }
                list.add(m);

                Component title = Component.empty()
                        .append(d.getDisplayName())
                        .append(Component.literal("  [" + d.name() + "]").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("  ").append(chancesLabel(d, weightTotal)).withStyle(ChatFormatting.DARK_GRAY));
                SubCategoryBuilder sub = entries.startSubCategory(title).setExpanded(false);
                addFields(sub, entries, m, fields, existing);
                group.add(sub.build());
            }

            for (int slot = 1; slot <= NEW_SLOTS; slot++) {
                Model m = new Model(fields.size());
                m.isNew = true;
                m.tier = Math.max(QualityRegistry.getTierMin(), Math.min(QualityRegistry.getTierMax(), 1));
                m.chance = 5.0D;
                m.group = QualityDefinition.WeightGroup.POSITIVE;
                list.add(m);
                SubCategoryBuilder sub = entries.startSubCategory(
                        Component.translatable(K + "quality_editor.new", slot).withStyle(ChatFormatting.GREEN))
                        .setExpanded(false);
                addFields(sub, entries, m, fields, existing);
                group.add(sub.build());
            }
            tab.addEntry(group.build());
        }

        return () -> save(models, limits);
    }

    /** "forge 0.2% | loot 5%" (or "loot lv 5+" for a quality that only unlocks in dangerous regions). */
    private static Component chancesLabel(QualityDefinition d, double weightTotal) {
        String forge = trim(org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality.forgeChancePercent(d));
        if (d.getWeight() <= 0) {
            if (d.getUnlockLevel() > 0) {
                return Component.translatable(K + "quality_editor.chances_unlock", forge, d.getUnlockLevel());
            }
            return Component.translatable(K + "quality_editor.chances", forge, "0");
        }
        return Component.translatable(K + "quality_editor.chances", forge, trim(d.getWeight() * 100.0D / weightTotal));
    }

    private static String trim(double v) {
        double r = round(v, 2);
        return r == Math.rint(r) ? Integer.toString((int) r) : Double.toString(r);
    }

    private static Optional<Component> validateId(String text, Set<String> existing) {
        String id = text.trim().toUpperCase(Locale.ROOT);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            return Optional.of(t("quality_editor.id_invalid"));
        }
        if (existing.contains(id)) {
            return Optional.of(t("quality_editor.id_duplicate"));
        }
        return Optional.empty();
    }

    private static void addFields(SubCategoryBuilder sub, ConfigEntryBuilder e, Model m,
                                  List<Effect> fields, Set<String> existing) {
        if (m.isNew) {
            sub.add(e.startStrField(t("quality_editor.new_id"), "")
                    .setDefaultValue("")
                    .setTooltip(TooltipWrap.wrap(t("quality_editor.new_id.tooltip")))
                    .setErrorSupplier(s -> validateId(s, existing))
                    .setSaveConsumer(s -> m.id = s.trim().toUpperCase(Locale.ROOT))
                    .build());
        } else {
            sub.add(e.startBooleanToggle(t("quality_editor.delete"), false)
                    .setDefaultValue(false)
                    .setTooltip(TooltipWrap.wrap(t("quality_editor.delete.tooltip")))
                    .setSaveConsumer(v -> m.delete = v)
                    .build());
        }
        sub.add(e.startStrField(t("quality_editor.name"), m.name)
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("quality_editor.name.tooltip")))
                .setSaveConsumer(s -> m.name = s.trim())
                .build());
        sub.add(e.startColorField(t("quality_editor.color"), m.rgb)
                .setDefaultValue(m.rgb)
                .setAlphaMode(false)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.color.tooltip")))
                .setSaveConsumer(v -> m.rgb = v & 0xFFFFFF)
                .build());
        sub.add(e.startIntField(t("quality_editor.tier"), m.tier)
                .setDefaultValue(m.tier)
                .setMin(QualityRegistry.getTierMin())
                .setMax(QualityRegistry.getTierMax())
                .setTooltip(TooltipWrap.wrap(t("quality_editor.tier.tooltip")))
                .setSaveConsumer(v -> m.tier = v)
                .build());
        sub.add(e.startDoubleField(t("quality_editor.weight"), m.chance)
                .setDefaultValue(m.chance)
                .setMin(0)
                .setMax(100)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.weight.tooltip")))
                .setSaveConsumer(v -> m.chance = v)
                .build());
        sub.add(e.startEnumSelector(t("quality_editor.group"), QualityDefinition.WeightGroup.class, m.group)
                .setDefaultValue(m.group)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.group.tooltip")))
                .setSaveConsumer(v -> m.group = v)
                .build());
        sub.add(e.startIntField(t("quality_editor.unlock"), m.unlock)
                .setDefaultValue(m.unlock)
                .setMin(0)
                .setMax(98)
                .setTooltip(TooltipWrap.wrap(t("quality_editor.unlock.tooltip")))
                .setSaveConsumer(v -> m.unlock = v)
                .build());
        for (int i = 0; i < fields.size(); i++) {
            final int index = i;
            Effect f = fields.get(i);
            double current = m.effects[i];
            sub.add(e.startDoubleField(t("quality_editor.effect." + f.key()), current)
                    .setDefaultValue(current)
                    .setMin(f.flat() ? -10.0D : -90.0D)
                    .setMax(f.flat() ? 10.0D : 500.0D)
                    .setSaveConsumer(v -> m.effects[index] = v)
                    .build());
        }
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    private static void save(Map<QualityCategory, List<Model>> models, Limits limits) {
        File file = QualityRegistry.getFile();
        if (file == null) {
            return;
        }
        JsonObject root = readRoot(file);
        JsonObject categories = new JsonObject();
        for (QualityCategory category : QualityCategory.values()) {
            List<Model> list = models.get(category);
            if (list == null) {
                continue;
            }
            List<Effect> fields = effectsFor(category);
            JsonArray array = new JsonArray();
            Set<String> used = new HashSet<>();
            for (Model m : list) {
                if (m.delete) {
                    continue;
                }
                if (m.isNew && !ID_PATTERN.matcher(m.id).matches()) {
                    continue; // empty or invalid slot: nothing to add
                }
                if (!used.add(m.id)) {
                    continue;
                }
                m.tier = Math.max(limits.min, Math.min(limits.max, m.tier));
                array.add(toJson(m, fields));
            }
            categories.add(category.key(), array);
        }
        root.addProperty("strict_limits", limits.strict);
        root.addProperty("tier_min", limits.min);
        root.addProperty("tier_max", limits.max);
        root.add("categories", categories);

        try {
            Files.writeString(file.toPath(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write the quality file {}", file, ex);
            return;
        }

        boolean ok = QualityRegistry.reload();
        Minecraft mc = Minecraft.getInstance();
        if (ok && mc.getSingleplayerServer() != null) {
            // LAN guests get the new definitions too
            mc.getSingleplayerServer().execute(NetworkHandler::sendQualitiesToAll);
        }
    }

    private static JsonObject readRoot(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement e = GSON.fromJson(reader, JsonElement.class);
            if (e != null && e.isJsonObject()) {
                return e.getAsJsonObject();
            }
        } catch (Exception ignored) {
            // fall through: write a fresh file
        }
        return new JsonObject();
    }

    /** The file stores weights: the chance in % * 100 (so 12.5 % = 1250). Only the ratio between them matters. */
    private static int toWeight(double percent) {
        return (int) Math.max(0, Math.min(10000, Math.round(percent * 100.0D)));
    }

    private static JsonObject toJson(Model m, List<Effect> fields) {
        JsonObject o = new JsonObject();
        o.addProperty("id", m.id);
        if (m.name != null && !m.name.isBlank()) {
            o.addProperty("name", m.name);
        }
        o.addProperty("color", String.format(Locale.ROOT, "#%06X", m.rgb & 0xFFFFFF));
        o.addProperty("tier", m.tier);
        o.addProperty("weight", toWeight(m.chance));
        o.addProperty("weight_group", m.group.name().toLowerCase(Locale.ROOT));
        if (m.unlock > 0) {
            o.addProperty("unlock_level", m.unlock);
        }
        JsonObject effects = new JsonObject();
        for (int i = 0; i < fields.size(); i++) {
            double v = m.effects[i];
            if (Math.abs(v) < 1.0E-9D) {
                continue;
            }
            Effect f = fields.get(i);
            effects.addProperty(f.key(), f.flat() ? round(v, 3) : round(v / 100.0D, 5));
        }
        o.add("effects", effects);
        return o;
    }
}
