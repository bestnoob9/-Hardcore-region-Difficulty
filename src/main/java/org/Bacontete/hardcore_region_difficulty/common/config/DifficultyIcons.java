package org.Bacontete.hardcore_region_difficulty.common.config;

import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.util.List;
import java.util.Locale;

/** Icons and title colors that a region or a difficulty level can use. */
public final class DifficultyIcons {

    /** Icons shipped with the mod (textures/gui/NAME.png). */
    public static final List<String> BUILT_IN = List.of(
            "skull_1", "skull_2", "skull_3", "skull_4", "skull_5", "skull_miniboss", "skull_boss",
            "flame", "crown", "bolt", "star", "gem", "snowflake");

    private DifficultyIcons() {}

    /** Empty = default icon (skull of the level). A name without ':' must be a built-in icon; with ':' any texture path. */
    public static boolean isValid(String icon) {
        if (icon == null || icon.isBlank()) {
            return true;
        }
        String s = icon.trim();
        if (s.contains(":")) {
            return ResourceLocation.tryParse(s) != null;
        }
        return BUILT_IN.contains(normalize(s.toLowerCase(Locale.ROOT)));
    }

    /**
     * Texture of the icon, or null for "use the default skull".
     * Built-in name: hardcore_region_difficulty:textures/gui/NAME.png.
     * "namespace:path/to/icon.png": that exact texture (any square size, e.g. from a resource pack).
     */
    public static ResourceLocation texture(String icon) {
        if (icon == null || icon.isBlank()) {
            return null;
        }
        ResourceLocation cached = TEXTURE_CACHE.get(icon);
        if (cached != null) {
            return cached;
        }
        ResourceLocation resolved = resolveTexture(icon);
        if (resolved != null) {
            if (TEXTURE_CACHE.size() > 128) {
                TEXTURE_CACHE.clear();
            }
            TEXTURE_CACHE.put(icon, resolved);
        }
        return resolved;
    }

    private static final java.util.Map<String, ResourceLocation> TEXTURE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static ResourceLocation resolveTexture(String icon) {
        String s = icon.trim();
        if (s.contains(":")) {
            return ResourceLocation.tryParse(s);
        }
        String name = normalize(s.toLowerCase(Locale.ROOT));
        if (!BUILT_IN.contains(name)) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(HardcoreRegionDifficulty.MODID, "textures/gui/" + name + ".png");
    }

    /** Old names of the boss skulls (skull_6 / skull_7) still work in existing config files. */
    private static String normalize(String name) {
        return switch (name) {
            case "skull_6" -> "skull_miniboss";
            case "skull_7" -> "skull_boss";
            default -> name;
        };
    }

    /** Built-in icon used when no icon is set: skull_1..5 for region levels, boss skulls for the boss entries. */
    public static String defaultIconForLevel(int level) {
        var boss = org.Bacontete.hardcore_region_difficulty.common.entity.BossLevel.fromConfigLevel(level);
        if (boss != null) {
            return boss.skullIcon();
        }
        return "skull_" + Math.max(1, Math.min(5, level));
    }

    /** Vanilla color name or #RRGGBB to 0xRRGGBB; -1 if empty or unknown. */
    public static int parseColorRgb(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        String s = text.trim();
        TextColor color = s.startsWith("#") ? TextColor.parseColor(s) : TextColor.parseColor(s.toLowerCase(Locale.ROOT));
        return color == null ? -1 : (color.getValue() & 0xFFFFFF);
    }
}
