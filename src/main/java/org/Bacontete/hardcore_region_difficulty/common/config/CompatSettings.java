package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.JsonObject;

/**
 * Optional cross-mod integration switches ("mod_compat_settings" in the config file).
 * Every switch defaults to false, so old config files keep working unchanged.
 */
public class CompatSettings {
    public static final CompatSettings DEFAULT = new CompatSettings(false);

    private final boolean deeperDarkerCataclysm;

    public CompatSettings(boolean deeperDarkerCataclysm) {
        this.deeperDarkerCataclysm = deeperDarkerCataclysm;
    }

    public static CompatSettings fromJson(JsonObject obj) {
        try {
            return new CompatSettings(
                    obj.has("deeperdarker_cataclysm") && obj.get("deeperdarker_cataclysm").getAsBoolean());
        } catch (RuntimeException e) {
            return DEFAULT;
        }
    }

    /**
     * When true, Cataclysm gear is upgraded from Deeper and Darker Warden gear instead of
     * netherite gear. Requires both the "deeperdarker" and "cataclysm" mods to be installed.
     */
    public boolean isDeeperDarkerCataclysm() { return deeperDarkerCataclysm; }
}
