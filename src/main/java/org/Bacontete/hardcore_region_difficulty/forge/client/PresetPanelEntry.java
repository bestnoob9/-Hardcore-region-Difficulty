package org.Bacontete.hardcore_region_difficulty.forge.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyPreset;
import org.Bacontete.hardcore_region_difficulty.common.config.PresetStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The preset panel of the Difficulty tab. Unlike a normal Cloth entry it has real buttons that work at once:
 * <ul>
 *   <li><b>&lt; / &gt;</b> choose a preset (built-in first, then your own) and show what it contains,</li>
 *   <li><b>Apply</b> types its values into the fields below (press Save &amp; Quit to keep them),</li>
 *   <li><b>Save as preset</b> stores what is in the fields now under the typed name: it is in the list immediately,
 *       no need to leave the screen,</li>
 *   <li><b>Delete</b> removes an own preset (asks to click twice).</li>
 * </ul>
 * Own presets are written to config/hardcore_region_difficulty-presets.json the moment you click.
 * Only load this class when cloth_config is present.
 */
public final class PresetPanelEntry extends AbstractConfigListEntry<Object> {

    private static final String K = "hardcore_region_difficulty.config.";
    private static final int ROW = 20;
    private static final int GAP = 4;
    private static final int HEIGHT = 136;
    private static final int MAX_NAME = 32;

    private final PresetBinding binding;
    private final Supplier<double[]> fallback;
    private final File dir;
    private final List<DifficultyPreset> presets = new ArrayList<>();
    private int selected;
    private boolean confirmDelete;
    private Component message = Component.empty();
    private int messageColor = 0xAAAAAA;

    private final Button prev;
    private final Button name;
    private final Button next;
    private final Button apply;
    private final Button delete;
    private final Button save;
    private final EditBox nameBox;
    private final List<AbstractWidget> widgets;

    /**
     * @param binding  the fields of the screen (read to create a preset, written to apply one)
     * @param fallback the values to use for a field that is not built
     * @param dir      the config folder (where the presets file is)
     */
    public PresetPanelEntry(PresetBinding binding, Supplier<double[]> fallback, File dir) {
        super(t("preset"), false);
        this.binding = binding;
        this.fallback = fallback;
        this.dir = dir;
        presets.addAll(DifficultyPreset.builtIns());
        presets.addAll(PresetStore.load(dir));

        Font font = Minecraft.getInstance().font;
        prev = Button.builder(Component.literal("<"), b -> select(-1)).bounds(0, 0, ROW, ROW).build();
        next = Button.builder(Component.literal(">"), b -> select(1)).bounds(0, 0, ROW, ROW).build();
        name = Button.builder(Component.empty(), b -> select(1)).bounds(0, 0, 100, ROW).build();
        apply = Button.builder(t("preset.apply"), b -> onApply()).bounds(0, 0, 100, ROW).build();
        delete = Button.builder(t("preset.delete"), b -> onDelete()).bounds(0, 0, 100, ROW).build();
        save = Button.builder(t("preset.save"), b -> onSave()).bounds(0, 0, 100, ROW).build();
        nameBox = new EditBox(font, 0, 0, 100, ROW, t("preset.new_name"));
        nameBox.setMaxLength(MAX_NAME);
        nameBox.setHint(t("preset.new_name.hint").copy().withStyle(ChatFormatting.DARK_GRAY));
        widgets = List.of(prev, name, next, apply, delete, save, nameBox);

        // start on the preset that matches the current values (the mod default if none does)
        DifficultyPreset now = DifficultyPreset.detect(presets, currentValues());
        selected = now != null ? presets.indexOf(now) : presets.indexOf(DifficultyPreset.NORMAL);
        refreshButtons();
    }

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    private double[] currentValues() {
        return binding.read(fallback.get());
    }

    private DifficultyPreset selectedPreset() {
        return presets.get(selected);
    }

    private static Component labelOf(DifficultyPreset p) {
        return p.builtIn ? t("preset." + p.id) : Component.literal(p.id);
    }

    private void setMessage(Component text, int color) {
        message = text;
        messageColor = color;
    }

    // ------------------------------------------------------------------
    // Actions (all take effect at once)
    // ------------------------------------------------------------------

    private void select(int delta) {
        selected = Math.floorMod(selected + delta, presets.size());
        confirmDelete = false;
        setMessage(Component.empty(), 0xAAAAAA);
        refreshButtons();
    }

    private void onApply() {
        DifficultyPreset p = selectedPreset();
        boolean ok = binding.apply(p);
        setMessage(Component.translatable(K + (ok ? "preset.applied" : "preset.applied_partly"), labelOf(p)),
                ok ? 0x55FF55 : 0xFFAA00);
    }

    private void onSave() {
        String typed = nameBox.getValue().trim();
        if (typed.isEmpty()) {
            setMessage(t("preset.err.empty"), 0xFF5555);
            return;
        }
        if (isReserved(typed)) {
            setMessage(t("preset.err.reserved"), 0xFF5555);
            return;
        }
        // same name (any case) replaces the old own preset
        boolean replaced = presets.removeIf(p -> !p.builtIn && p.id.equalsIgnoreCase(typed));
        DifficultyPreset created = new DifficultyPreset(typed, false, currentValues());
        presets.add(created);
        if (!PresetStore.save(dir, presets)) {
            presets.remove(created);
            setMessage(t("preset.err.write"), 0xFF5555);
            selected = Math.min(selected, presets.size() - 1);
            refreshButtons();
            return;
        }
        selected = presets.indexOf(created);
        confirmDelete = false;
        nameBox.setValue("");
        setMessage(Component.translatable(K + (replaced ? "preset.overwritten" : "preset.saved"), typed), 0x55FF55);
        refreshButtons();
    }

    private void onDelete() {
        DifficultyPreset p = selectedPreset();
        if (p.builtIn) return;
        if (!confirmDelete) {
            confirmDelete = true;
            refreshButtons();
            return;
        }
        presets.remove(p);
        PresetStore.save(dir, presets);
        selected = Math.min(selected, presets.size() - 1);
        confirmDelete = false;
        setMessage(Component.translatable(K + "preset.deleted", p.id), 0xFFAA00);
        refreshButtons();
    }

    private boolean isReserved(String typed) {
        if (typed.equalsIgnoreCase(t("preset.custom").getString())) return true;
        for (DifficultyPreset p : DifficultyPreset.builtIns()) {
            if (p.id.equalsIgnoreCase(typed) || labelOf(p).getString().equalsIgnoreCase(typed)) return true;
        }
        return false;
    }

    private boolean ownNameExists(String typed) {
        for (DifficultyPreset p : presets) {
            if (!p.builtIn && p.id.equalsIgnoreCase(typed)) return true;
        }
        return false;
    }

    private void refreshButtons() {
        DifficultyPreset p = selectedPreset();
        name.setMessage(p.builtIn ? labelOf(p) : labelOf(p).copy().withStyle(ChatFormatting.AQUA)
                .append(Component.literal("  ").append(t("preset.own_tag").copy().withStyle(ChatFormatting.GRAY))));
        delete.active = !p.builtIn;
        delete.setMessage(confirmDelete ? t("preset.delete.confirm").copy().withStyle(ChatFormatting.RED) : t("preset.delete"));
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    private static String fmt(double v) {
        return Math.abs(v - Math.rint(v)) < 0.0001D ? String.valueOf((long) Math.rint(v))
                : String.format(Locale.ROOT, "%.1f", v);
    }

    private Component summaryMobs(DifficultyPreset p) {
        return Component.translatable(K + "preset.summary.mobs",
                fmt(p.get(DifficultyPreset.HEALTH)), fmt(p.get(DifficultyPreset.DAMAGE)),
                fmt(p.get(DifficultyPreset.ARMOR)), fmt(p.get(DifficultyPreset.SPEED)),
                fmt(p.get(DifficultyPreset.ARMOR_FLAT)));
    }

    private Component summaryBosses(DifficultyPreset p) {
        return Component.translatable(K + "preset.summary.bosses",
                fmt(p.get(DifficultyPreset.MINI_HEALTH)), fmt(p.get(DifficultyPreset.BOSS_HEALTH)),
                fmt(p.get(DifficultyPreset.DRAGON_HEALTH)), fmt(p.get(DifficultyPreset.WITHER_HEALTH)),
                fmt(p.get(DifficultyPreset.WARDEN_HEALTH)), String.valueOf(p.raid()));
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(g, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = entryWidth;

        // header + what the fields hold now
        DifficultyPreset matching = DifficultyPreset.detect(presets, currentValues());
        g.drawString(font, t("preset").copy().withStyle(ChatFormatting.BOLD), x, y + 2, 0xFFFFFF, true);
        Component status = matching != null
                ? Component.translatable(K + "preset.status.match", labelOf(matching))
                : t("preset.status.custom");
        g.drawString(font, status, x + w - font.width(status), y + 2, matching != null ? 0x55FF55 : 0xFFAA00, true);

        // row 1: < name >
        int row1 = y + 16;
        place(prev, x, row1, ROW);
        place(next, x + w - ROW, row1, ROW);
        place(name, x + ROW + GAP, row1, w - 2 * (ROW + GAP));

        // row 2: what the chosen preset contains
        int row2 = row1 + ROW + 3;
        DifficultyPreset p = selectedPreset();
        g.drawString(font, summaryMobs(p), x + 2, row2, 0xBBBBBB, false);
        g.drawString(font, summaryBosses(p), x + 2, row2 + 11, 0xBBBBBB, false);

        // row 3: apply / delete
        int row3 = row2 + 26;
        int half = (w - GAP) / 2;
        place(apply, x, row3, half);
        place(delete, x + half + GAP, row3, w - half - GAP);

        // row 4: create a new preset from the fields
        int row4 = row3 + ROW + 8;
        g.drawString(font, t("preset.create.title"), x, row4 - 1, 0xDDDDDD, true);
        int row5 = row4 + 11;
        int saveWidth = Math.min(150, w / 2);
        place(nameBox, x, row5, w - saveWidth - GAP);
        String typed = nameBox.getValue().trim();
        save.setMessage(ownNameExists(typed) ? t("preset.overwrite") : t("preset.save"));
        place(save, x + w - saveWidth, row5, saveWidth);

        // feedback line
        if (!message.getString().isEmpty()) {
            List<FormattedCharSequence> lines = font.split(message, w);
            int ly = row5 + ROW + 4;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.drawString(font, lines.get(i), x + 2, ly + i * 10, messageColor, true);
            }
        }

        for (AbstractWidget widget : widgets) widget.render(g, mouseX, mouseY, delta);
    }

    private static void place(AbstractWidget widget, int x, int y, int width) {
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
    }

    // ------------------------------------------------------------------
    // Cloth entry plumbing: this row edits nothing by itself
    // ------------------------------------------------------------------

    @Override public int getItemHeight() { return HEIGHT; }
    @Override public Object getValue() { return null; }
    @Override public Optional<Object> getDefaultValue() { return Optional.empty(); }
    @Override public void save() {}
    @Override public boolean isEdited() { return false; }
    @Override public List<? extends GuiEventListener> children() { return widgets; }
    @Override public List<? extends NarratableEntry> narratables() { return widgets; }
}
