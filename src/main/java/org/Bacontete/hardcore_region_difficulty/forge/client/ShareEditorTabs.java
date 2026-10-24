package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.config.ConfigShare;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;

import java.io.File;

/**
 * "Share" tab: the setup lives in HrdSetting.json in the config folder (written by the mod, or by /hrd export).
 * To use another setup, put its file in the config folder, type its name here and save.
 */
public final class ShareEditorTabs {

    private static final String K = "hardcore_region_difficulty.config.";

    private ShareEditorTabs() {}

    private static Component t(String key) {
        return Component.translatable(K + key);
    }

    /** @return the runnable to call at the end of the saving runnable (it may replace the config files). */
    public static Runnable add(ConfigEntryBuilder e, ConfigCategory tab) {
        Minecraft mc = Minecraft.getInstance();
        DifficultyConfig cfg = DifficultyConfig.getInstance();
        if (cfg == null) return () -> {};
        boolean editable = mc.level == null || mc.getSingleplayerServer() != null;

        ConfigShare.ensureFile(cfg);
        File own = new File(cfg.getConfigDirectory(), ConfigShare.FILE_NAME);
        tab.addEntry(e.startTextDescription(t("share.note")).build());
        tab.addEntry(e.startTextDescription(Component.literal(own.getPath()).withStyle(ChatFormatting.AQUA)).build());

        if (!editable) {
            tab.addEntry(e.startTextDescription(t("region_editor.remote").copy().withStyle(ChatFormatting.RED)).build());
            return () -> {};
        }
        String[] fileName = {""};
        tab.addEntry(e.startStrField(t("share.import"), "")
                .setDefaultValue("")
                .setTooltip(TooltipWrap.wrap(t("share.import.tooltip")))
                .setSaveConsumer(v -> fileName[0] = v == null ? "" : v.trim())
                .build());
        tab.addEntry(e.startTextDescription(t("share.import.warning").copy().withStyle(ChatFormatting.RED)).build());

        return () -> {
            if (fileName[0].isEmpty()) return;
            // only a file of the config folder, never a path
            File file = new File(cfg.getConfigDirectory(), new File(fileName[0]).getName());
            JsonObject pack = ConfigShare.readFile(file);
            if (pack == null) {
                HardcoreRegionDifficulty.LOGGER.error("Share import failed: cannot read {}", file);
                return;
            }
            String error = ConfigShare.apply(cfg, pack);
            if (error != null) {
                HardcoreRegionDifficulty.LOGGER.error("Share import failed: {}", error);
            } else if (mc.getSingleplayerServer() == null) {
                cfg.loadConfig();
            }
        };
    }
}
