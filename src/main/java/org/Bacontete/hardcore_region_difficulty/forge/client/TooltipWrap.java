package org.Bacontete.hardcore_region_difficulty.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloth Config does not wrap tooltips, so a long sentence becomes one very wide line.
 * This splits every tooltip part into lines of at most {@link #WIDTH} pixels (keeping its color/style).
 */
public final class TooltipWrap {

    private static final int WIDTH = 230;

    private TooltipWrap() {}

    public static Component[] wrap(Component... parts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) {
            return parts;
        }
        List<Component> out = new ArrayList<>();
        for (Component part : parts) {
            Style style = part.getStyle();
            for (var line : mc.font.getSplitter().splitLines(part, WIDTH, style)) {
                out.add(Component.literal(line.getString()).withStyle(style));
            }
        }
        return out.toArray(new Component[0]);
    }
}
