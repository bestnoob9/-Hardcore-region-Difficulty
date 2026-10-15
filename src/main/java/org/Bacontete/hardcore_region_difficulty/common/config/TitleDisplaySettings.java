package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonObject;

public class TitleDisplaySettings {
    public static final String STYLE_COMPACT = "compact";
    public static final String STYLE_VANILLA = "vanilla";

    public static final double DEFAULT_SCALE = 1.8;

    private final boolean enabled;
    private final String style;
    private final double scale;
    private final int yOffset;
    private final int fadeInTicks;
    private final int stayTicks;
    private final int fadeOutTicks;
    private final int repeatCooldownSeconds;

    public TitleDisplaySettings() {
        this(true, STYLE_COMPACT, DEFAULT_SCALE, 30, 10, 40, 20, 60);
    }

    public TitleDisplaySettings(boolean enabled, String style, double scale, int yOffset,
                                int fadeInTicks, int stayTicks, int fadeOutTicks, int repeatCooldownSeconds) {
        this.enabled = enabled;
        this.style = STYLE_VANILLA.equalsIgnoreCase(style) ? STYLE_VANILLA : STYLE_COMPACT;
        this.scale = Math.max(0.5, Math.min(3.0, scale));
        this.yOffset = Math.max(0, Math.min(400, yOffset));
        this.fadeInTicks = Math.max(0, fadeInTicks);
        this.stayTicks = Math.max(0, stayTicks);
        this.fadeOutTicks = Math.max(0, fadeOutTicks);
        this.repeatCooldownSeconds = Math.max(0, repeatCooldownSeconds);
    }

    public static TitleDisplaySettings fromJson(JsonObject obj) {
        TitleDisplaySettings d = new TitleDisplaySettings();
        try {
            return new TitleDisplaySettings(
                    obj.has("enabled") ? obj.get("enabled").getAsBoolean() : d.enabled,
                    obj.has("style") ? obj.get("style").getAsString() : d.style,
                    obj.has("scale") ? obj.get("scale").getAsDouble() : d.scale,
                    obj.has("y_offset") ? obj.get("y_offset").getAsInt() : d.yOffset,
                    obj.has("fade_in_ticks") ? obj.get("fade_in_ticks").getAsInt() : d.fadeInTicks,
                    obj.has("stay_ticks") ? obj.get("stay_ticks").getAsInt() : d.stayTicks,
                    obj.has("fade_out_ticks") ? obj.get("fade_out_ticks").getAsInt() : d.fadeOutTicks,
                    obj.has("repeat_cooldown_seconds") ? obj.get("repeat_cooldown_seconds").getAsInt() : d.repeatCooldownSeconds);
        } catch (RuntimeException e) {
            return d;
        }
    }

    public boolean isEnabled() { return enabled; }
    public boolean isVanillaStyle() { return STYLE_VANILLA.equals(style); }
    public double getScale() { return scale; }
    public int getYOffset() { return yOffset; }
    public int getFadeInTicks() { return fadeInTicks; }
    public int getStayTicks() { return stayTicks; }
    public int getFadeOutTicks() { return fadeOutTicks; }
    public int getRepeatCooldownSeconds() { return repeatCooldownSeconds; }
}
