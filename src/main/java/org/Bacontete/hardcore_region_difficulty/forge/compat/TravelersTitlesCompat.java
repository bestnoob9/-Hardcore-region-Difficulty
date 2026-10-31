package org.Bacontete.hardcore_region_difficulty.forge.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.forge.client.RegionTitleOverlay;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional integration with Traveler's Titles (modid "travelerstitles", client side only).
 *
 * The integration is switched on only at client startup ({@link #init()}), and only if Traveler's Titles is loaded.
 * Without Traveler's Titles nothing here is ever touched and HRD uses its own title.
 *
 * With it, HRD never draws its own title. Biome titles are the ones of Traveler's Titles; HRD draws just the
 * difficulty line (skull icon + difficulty name) right under it, with the same position and fade, and it disappears
 * at the same moment as that title. Structure titles do not exist in Traveler's Titles, so for a structure HRD hands
 * the structure name to the Traveler's Titles renderer (same font, size, position, fade, color) and the difficulty
 * line goes under it in exactly the same way.
 *
 * The difficulty line is bound to ONE specific title (the object Traveler's Titles displays). It is shown only
 * while that title is on screen: if the title is replaced or its timer ends, the difficulty line is dropped.
 *
 * There is no compile-time dependency: everything is read from Traveler's Titles through reflection.
 * If anything cannot be resolved, {@link #isActive()} returns false and the normal HRD title is used.
 */
public final class TravelersTitlesCompat {
    public static final String MOD_ID = "travelerstitles";

    private static final String PKG = "com.yungnickyoung.minecraft.travelerstitles.";
    /** How long to wait for Traveler's Titles to start a title after HRD got the region packet. */
    private static final long ATTACH_WAIT_MS = 8000L;
    /** A Traveler's Titles title that started up to this long BEFORE the HRD packet still belongs to it (packet/tick order). */
    private static final long RECENT_TITLE_MS = 4000L;
    private static final long MAX_PENDING_MS = 8000L;
    private static final int FONT_HEIGHT = 9;

    /** Set once at client startup, only when Traveler's Titles is loaded and readable. Nothing works before that. */
    private static volatile boolean enabled;

    private static boolean resolved;
    private static boolean usable;
    private static Field managerField;
    private static Field biomeRendererField;
    private static Field dimensionRendererField;
    private static Field enabledField;
    private static Field displayedTitleField;
    /** Subtitle line of a Traveler's Titles title (the dimension title has one); null when it cannot be found. */
    private static Field subtitleField;
    private static Field titleTimerField;
    private static Field fadeInField;
    private static Field displayTimeField;
    private static Field fadeOutField;
    private static Field textSizeField;
    private static Field xOffsetField;
    private static Field yOffsetField;
    private static Field centeredField;
    private static Field defaultColorField;
    private static Method displayTitleMethod;
    private static Method setColorMethod;

    /** What a Traveler's Titles renderer showed last frame, used to know when a NEW title started on it. */
    private static final class Seen {
        Object title;
        int timer;
        long startMillis;

        void observe(Object newTitle, int newTimer) {
            if (newTimer > 0 && (newTitle != title || newTimer > timer)) {
                startMillis = System.currentTimeMillis();
            }
            title = newTitle;
            timer = newTimer;
        }
    }

    private static final Seen BIOME_SEEN = new Seen();
    private static final Seen DIMENSION_SEEN = new Seen();

    /** When the player has just arrived in a dimension, a dimension title gets the line before the biome title does. */
    private static final long DIMENSION_GRACE_MS = 1200L;
    private static java.lang.ref.WeakReference<Object> lastLevel = new java.lang.ref.WeakReference<>(null);

    /** One difficulty line waiting for / attached to a Traveler's Titles title. */
    private static final class Pending {
        final String subtitle;
        final String skullIcon;
        final String icon;
        final int color;
        final long startMillis;
        /** Structure titles do not exist in Traveler's Titles: HRD hands the name to its renderer. */
        final String structureName;
        /** True when the packet came with a new dimension (a new level object): Traveler's Titles shows a dimension title. */
        final boolean dimensionEntry;
        /** The exact Traveler's Titles title this line is bound to (null until attached). */
        Object boundTitle;
        /** The Traveler's Titles renderer (biome or dimension) that shows the bound title. */
        Object boundRenderer;

        Pending(String structureName, String subtitle, String skullIcon, String icon, int color, boolean dimensionEntry) {
            this.structureName = structureName;
            this.subtitle = subtitle;
            this.skullIcon = skullIcon;
            this.icon = icon;
            this.color = color;
            this.dimensionEntry = dimensionEntry;
            this.startMillis = System.currentTimeMillis();
        }
    }

    private static volatile Pending pending;

    private TravelersTitlesCompat() {}

    public static boolean isInstalled() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(MOD_ID);
    }

    /**
     * Called once at client startup. Checks that Traveler's Titles is loaded and only then reads its classes and
     * turns the integration on. When it is not loaded this does nothing at all.
     */
    public static void init() {
        if (!isInstalled()) {
            enabled = false;
            HardcoreRegionDifficulty.LOGGER.info("Traveler's Titles not installed: HRD uses its own region title.");
            return;
        }
        enabled = resolve();
    }

    /**
     * Traveler's Titles is loaded and readable: HRD never draws its own region title (so no \"Overworld\" title on
     * join); it only adds the difficulty line under the Traveler's Titles title.
     */
    public static boolean isIntegrated() {
        return enabled && usable;
    }

    /** Integration was enabled at startup and Traveler's Titles biome titles are switched on. */
    public static boolean isActive() {
        if (!enabled || !usable) {
            return false;
        }
        try {
            Object biome = biomeRenderer();
            return biome != null && enabledField.getBoolean(biome);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Called when the server says the player entered a new region / difficulty.
     *
     * @param structureName name to show as a Traveler's Titles title when the player entered a structure, or null/empty for a biome
     */
    public static void show(String structureName, String subtitle, String skullIcon, String icon, int color) {
        Object level = Minecraft.getInstance().level;
        boolean dimensionEntry = level != lastLevel.get();
        lastLevel = new java.lang.ref.WeakReference<>(level);
        pending = new Pending(structureName == null || structureName.isEmpty() ? null : structureName,
                subtitle, skullIcon, icon == null ? "" : icon, color, dimensionEntry);
    }

    /** Called every frame by the HRD overlay while Traveler's Titles is active. */
    public static void render(GuiGraphics graphics, float partialTick, int width, int height) {
        try {
            Object biome = biomeRenderer();
            if (biome == null) {
                pending = null;
                return;
            }
            Object dimension = dimensionRenderer();
            Object biomeTitle = displayedTitleField.get(biome);
            int biomeTimer = titleTimerField.getInt(biome);
            BIOME_SEEN.observe(biomeTitle, biomeTimer);
            Object dimensionTitle = null;
            int dimensionTimer = 0;
            boolean dimensionEnabled = dimension != null && enabledField.getBoolean(dimension);
            if (dimension != null) {
                dimensionTitle = displayedTitleField.get(dimension);
                dimensionTimer = titleTimerField.getInt(dimension);
                DIMENSION_SEEN.observe(dimensionTitle, dimensionTimer);
            }

            Pending p = pending;
            if (p == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui || minecraft.options.renderDebug) {
                return; // Traveler's Titles hides its titles in the same cases
            }

            if (p.boundRenderer == null) {
                long age = System.currentTimeMillis() - p.startMillis;
                if (age > MAX_PENDING_MS) {
                    pending = null;
                    return;
                }
                if (p.structureName != null) {
                    // a dimension title is showing: wait until it is gone
                    if (dimension != null && dimensionTimer > 0) {
                        return;
                    }
                    pushTitle(biome, p.structureName);
                    Object pushed = displayedTitleField.get(biome);
                    BIOME_SEEN.observe(pushed, titleTimerField.getInt(biome));
                    p.boundTitle = pushed;
                    p.boundRenderer = biome;
                } else if (p.dimensionEntry && dimensionEnabled && dimensionTimer > 0 && dimensionTitle instanceof Component
                        && DIMENSION_SEEN.startMillis >= p.startMillis - RECENT_TITLE_MS) {
                    // just arrived in a dimension: the line lives as long as the DIMENSION title (and is placed under both titles)
                    p.boundTitle = dimensionTitle;
                    p.boundRenderer = dimension;
                } else if (p.dimensionEntry && dimensionEnabled && age < DIMENSION_GRACE_MS) {
                    return; // the dimension title may still start
                } else if (biomeTimer > 0 && biomeTitle instanceof Component
                        && BIOME_SEEN.startMillis >= p.startMillis - RECENT_TITLE_MS) {
                    p.boundTitle = biomeTitle; // Traveler's Titles is showing the title of this region: attach to it
                    p.boundRenderer = biome;
                } else if (age >= ATTACH_WAIT_MS) {
                    pending = null; // Traveler's Titles chose not to show a title here, so nothing is shown
                    return;
                } else {
                    return; // Traveler's Titles may still react in the next ticks
                }
            }

            Object renderer = p.boundRenderer;
            Object title = displayedTitleField.get(renderer);
            int timer = titleTimerField.getInt(renderer);
            // same lifetime as the Traveler's Titles title: gone when it ends or when it is replaced
            if (timer <= 0 || title != p.boundTitle) {
                pending = null;
                return;
            }
            draw(graphics, partialTick, width, height, renderer, biome, timer, p);
        } catch (ReflectiveOperationException | RuntimeException e) {
            HardcoreRegionDifficulty.LOGGER.warn("Traveler's Titles integration failed, using the normal HRD title: {}", e.toString());
            usable = false;
            pending = null;
        }
    }

    /** Shows a title through the Traveler's Titles renderer, exactly as Traveler's Titles does for a biome. */
    private static void pushTitle(Object biomeRenderer, String name) throws ReflectiveOperationException {
        Object color = defaultColorField.get(biomeRenderer);
        if (color instanceof String c) {
            setColorMethod.invoke(biomeRenderer, c);
        }
        displayTitleMethod.invoke(biomeRenderer, Component.literal(name), null);
    }

    /**
     * Layout copied from TitleRenderer.renderText of Traveler's Titles 4.0.2: with isTextCentered the origin is the middle of the
     * screen, the title is scaled by titleTextSize and drawn at x = titleXOffset - width / 2, y = titleYOffset.
     * When the dimension title is shown, Traveler's Titles shows the biome title AT THE SAME TIME with its own renderer
     * (smaller, lower). The line goes below the lowest of them, so it never overlaps.
     */
    private static void draw(GuiGraphics graphics, float partialTick, int width, int height,
                             Object bound, Object biome, int timer, Pending p) throws ReflectiveOperationException {
        int fadeIn = Math.max(1, fadeInField.getInt(bound));
        int display = displayTimeField.getInt(bound);
        int fadeOut = Math.max(1, fadeOutField.getInt(bound));

        // exactly the same opacity curve as Traveler's Titles
        float age = timer - partialTick;
        int opacity = 255;
        if (timer > fadeOut + display) {
            float r = (fadeIn + display + fadeOut) - age;
            opacity = (int) (r * 255.0F / fadeIn);
        }
        if (timer <= fadeOut) {
            opacity = (int) (age * 255.0F / fadeOut);
        }
        opacity = Math.max(0, Math.min(255, opacity));
        if (opacity <= 8) {
            return; // Traveler's Titles does not draw below this either
        }

        // the line sits in the biome title column when the biome renderer is on (dimension title case), else under the bound title
        Object layout = bound != biome && enabledField.getBoolean(biome) ? biome : bound;
        float bottomY = Math.max(rowBottom(layout, height), rowBottom(bound, height));
        float centerX = rowCenterX(layout, width);

        // a real subtitle (drawn at scale 1.3, y = -35 from the origin) of the bound title: stay below it too
        if (subtitleField != null) {
            Object sub = subtitleField.get(bound);
            if (sub != null && !(sub instanceof Component c && c.getString().isEmpty())) {
                float origin = centeredField.getBoolean(bound) ? height / 2f : 0f;
                bottomY = Math.max(bottomY, origin + (-35f + FONT_HEIGHT) * 1.3f);
            }
        }

        float subScale = Math.max(1f, textSizeField.getFloat(layout) * 0.55f);
        RegionTitleOverlay.drawDifficultyLine(graphics, Minecraft.getInstance().font, p.subtitle, p.icon, p.skullIcon,
                p.color, centerX, bottomY + 3f, subScale, opacity / 255f);
    }

    /** Bottom edge (screen px) of the title row of a Traveler's Titles renderer. */
    private static float rowBottom(Object r, int height) throws ReflectiveOperationException {
        float size = textSizeField.getFloat(r);
        int yOffset = yOffsetField.getInt(r);
        return centeredField.getBoolean(r) ? height / 2f + (yOffset + FONT_HEIGHT) * size : (yOffset + FONT_HEIGHT) * size;
    }

    /** Horizontal center (screen px) of the title row of a Traveler's Titles renderer. */
    private static float rowCenterX(Object r, int width) throws ReflectiveOperationException {
        float size = textSizeField.getFloat(r);
        int xOffset = xOffsetField.getInt(r);
        if (centeredField.getBoolean(r)) {
            return width / 2f + xOffset * size;
        }
        Object t = displayedTitleField.get(r);
        int textWidth = t instanceof Component c ? Minecraft.getInstance().font.width(c) : 0;
        return xOffset * size + textWidth * size / 2f;
    }

    /**
     * The subtitle of a Traveler's Titles title lives in a field whose name we do not know for sure: look at the public
     * and the private fields, and log them once so a wrong guess is easy to fix.
     */
    private static Field findSubtitleField(Class<?> renderer) {
        Field found = null;
        StringBuilder all = new StringBuilder();
        for (Field f : renderer.getDeclaredFields()) {
            all.append(f.getName()).append(':').append(f.getType().getSimpleName()).append(' ');
            if (found == null && !java.lang.reflect.Modifier.isStatic(f.getModifiers())
                    && f.getName().toLowerCase(java.util.Locale.ROOT).contains("sub")) {
                try {
                    f.setAccessible(true);
                    found = f;
                } catch (RuntimeException ignored) {
                    // not readable: the dimension fallback below is used instead
                }
            }
        }
        HardcoreRegionDifficulty.LOGGER.info("Traveler's Titles renderer fields: {}| subtitle field: {}", all,
                found == null ? "not found" : found.getName());
        return found;
    }

    private static Object biomeRenderer() throws ReflectiveOperationException {
        Object manager = managerField.get(null);
        return manager == null ? null : biomeRendererField.get(manager);
    }

    private static Object dimensionRenderer() throws ReflectiveOperationException {
        Object manager = managerField.get(null);
        return manager == null ? null : dimensionRendererField.get(manager);
    }

    private static synchronized boolean resolve() {
        if (resolved) {
            return usable;
        }
        resolved = true;
        try {
            ClassLoader loader = TravelersTitlesCompat.class.getClassLoader();
            // initialize = false: only read the structure of the classes, do not run anything of Traveler's Titles
            Class<?> common = Class.forName(PKG + "TravelersTitlesCommon", false, loader);
            Class<?> manager = Class.forName(PKG + "render.TitleRenderManager", false, loader);
            Class<?> renderer = Class.forName(PKG + "render.TitleRenderer", false, loader);
            managerField = common.getField("titleManager");
            biomeRendererField = manager.getField("biomeTitleRenderer");
            dimensionRendererField = manager.getField("dimensionTitleRenderer");
            enabledField = renderer.getField("enabled");
            displayedTitleField = renderer.getField("displayedTitle");
            titleTimerField = renderer.getField("titleTimer");
            subtitleField = findSubtitleField(renderer);
            fadeInField = renderer.getField("titleFadeInTicks");
            displayTimeField = renderer.getField("titleDisplayTime");
            fadeOutField = renderer.getField("titleFadeOutTicks");
            textSizeField = renderer.getField("titleTextSize");
            xOffsetField = renderer.getField("titleXOffset");
            yOffsetField = renderer.getField("titleYOffset");
            centeredField = renderer.getField("isTextCentered");
            defaultColorField = renderer.getField("titleDefaultTextColor");
            displayTitleMethod = renderer.getMethod("displayTitle", Component.class, Component.class);
            setColorMethod = renderer.getMethod("setColor", String.class);
            usable = true;
            HardcoreRegionDifficulty.LOGGER.info("Traveler's Titles found: biome titles are shown by Traveler's Titles, HRD adds the difficulty line and the structure titles through it.");
        } catch (ReflectiveOperationException | LinkageError e) {
            usable = false;
            HardcoreRegionDifficulty.LOGGER.warn("Traveler's Titles found but its classes could not be read ({}). HRD keeps its own title.", e.toString());
        }
        return usable;
    }
}
