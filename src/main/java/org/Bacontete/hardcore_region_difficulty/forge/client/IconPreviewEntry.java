package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyIcons;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Read-only row of the Cloth Config screen that shows the icon currently typed / selected in the
 * icon field above it. Only load this class when cloth_config is present.
 */
public final class IconPreviewEntry extends AbstractConfigListEntry<Object> {

    private static final int SIZE = 24;

    private final Supplier<String> iconSupplier;
    private final Supplier<String> fallbackSupplier;
    private final Component label;

    /**
     * @param iconSupplier     current text of the icon field
     * @param fallbackSupplier built-in icon shown when the field is empty (e.g. "skull_1")
     */
    public IconPreviewEntry(Component label, Supplier<String> iconSupplier, Supplier<String> fallbackSupplier) {
        super(label, false);
        this.label = label;
        this.iconSupplier = iconSupplier;
        this.fallbackSupplier = fallbackSupplier;
    }

    @Override
    public void render(GuiGraphics g, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(g, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Minecraft mc = Minecraft.getInstance();
        g.drawString(mc.font, label, x, y + (entryHeight - mc.font.lineHeight) / 2, 0xFFFFFF, true);

        String text = iconSupplier.get();
        boolean valid = DifficultyIcons.isValid(text);
        ResourceLocation tex = valid ? DifficultyIcons.texture(text) : null;
        boolean usingDefault = tex == null && valid;
        if (tex == null) {
            tex = DifficultyIcons.texture(fallbackSupplier.get());
        }
        int left = x + entryWidth - SIZE - 4;
        int top = y + (entryHeight - SIZE) / 2;
        g.fill(left - 2, top - 2, left + SIZE + 2, top + SIZE + 2, 0x66000000);
        if (IconFolder.ensure(tex)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(tex, left, top, SIZE, SIZE, 0f, 0f, 1, 1, 1, 1);
            RenderSystem.disableBlend();
        } else {
            // invalid name or missing texture file: red cross
            g.drawCenteredString(mc.font, "?", left + SIZE / 2, top + (SIZE - mc.font.lineHeight) / 2, 0xFF5555);
        }
        if (usingDefault) {
            g.drawString(mc.font, Component.translatable("hardcore_region_difficulty.config.region_editor.icon_default"),
                    left - 6 - mc.font.width(Component.translatable("hardcore_region_difficulty.config.region_editor.icon_default")),
                    y + (entryHeight - mc.font.lineHeight) / 2, 0xAAAAAA, true);
        }
    }

    @Override public int getItemHeight() { return SIZE + 8; }
    @Override public Object getValue() { return null; }
    @Override public Optional<Object> getDefaultValue() { return Optional.empty(); }
    @Override public void save() {}
    @Override public boolean isEdited() { return false; }
    @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
    @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
}
