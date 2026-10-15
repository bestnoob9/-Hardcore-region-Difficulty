package org.Bacontete.hardcore_region_difficulty.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyConfig;
import org.Bacontete.hardcore_region_difficulty.common.config.TitleDisplaySettings;
import org.Bacontete.hardcore_region_difficulty.forge.ClientConfig;

import java.util.HashMap;
import java.util.Map;

public class TitleDisplayHandler {

    public record Active(String name, String subtitle, int skullTier, long startMillis,
                         int fadeInTicks, int stayTicks, int fadeOutTicks,
                         double scale, int yOffset) {}

    public static final int SKULL_TIERS = 7;

    public static int skullTier(int difficultyLevel) {
        return Math.max(1, Math.min(SKULL_TIERS, difficultyLevel));
    }

    private static ChatFormatting skullColor(int tier) {
        return switch (tier) {
            case 1 -> ChatFormatting.GRAY;
            case 2 -> ChatFormatting.YELLOW;
            case 3 -> ChatFormatting.GOLD;
            case 4 -> ChatFormatting.RED;
            case 5 -> ChatFormatting.LIGHT_PURPLE;
            case 6 -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.DARK_RED;
        };
    }

    private static volatile Active current;
    private static final Map<String, Long> LAST_SHOWN = new HashMap<>();

    /** Cong trinh cua packet title truoc do (ke ca packet bi cooldown chan), de biet khi nao vua ra khoi cong trinh. */
    private static String lastPacketStructure = "";

    public static Active current() {
        return current;
    }

    public static void clear() {
        current = null;
    }

    public static void displayBossTitle(String entityName, String bossTypeKey, int bossLevel) {
        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null && config.hasFileChanged()) {
            config.loadConfig();
        }
        TitleDisplaySettings settings = config != null ? config.getTitleDisplaySettings() : new TitleDisplaySettings();
        if (settings == null || !settings.isEnabled() || !ClientConfig.SHOW_BOSS_TITLE.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entityName == null || entityName.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        int tier = skullTier(bossLevel);

        if (settings.isVanillaStyle()) {
            minecraft.gui.setTimes(settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks());
            minecraft.gui.setSubtitle(Component.literal("\u2620 ").withStyle(skullColor(tier))
                    .append(Component.translatable(bossTypeKey).withStyle(ChatFormatting.GRAY)));
            minecraft.gui.setTitle(Component.literal(entityName));
            return;
        }

        current = new Active(entityName, Component.translatable(bossTypeKey).getString(), tier, now,
                settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks(),
                settings.getScale(), settings.getYOffset());
    }

    public static void displayTitle(String regionName, String difficultyTitle, String structureName, int difficultyLevel) {
        String incomingStructure = structureName == null ? "" : structureName;
        boolean leftStructure = !lastPacketStructure.isEmpty() && incomingStructure.isEmpty();
        lastPacketStructure = incomingStructure;

        DifficultyConfig config = DifficultyConfig.getInstance();
        if (config != null && config.hasFileChanged()) {
            config.loadConfig(); 
        }
        TitleDisplaySettings settings = config != null ? config.getTitleDisplaySettings() : new TitleDisplaySettings();
        if (settings == null || !settings.isEnabled() || !ClientConfig.SHOW_REGION_TITLE.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || regionName == null || regionName.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        String key = regionName + "|" + structureName;
        int cooldown = settings.getRepeatCooldownSeconds();
        Long last = LAST_SHOWN.get(key);
        if (!leftStructure && cooldown > 0 && last != null && now - last < cooldown * 1000L) {
            return;
        }
        if (LAST_SHOWN.size() > 64) {
            LAST_SHOWN.clear();
        }
        LAST_SHOWN.put(key, now);

        int tier = skullTier(difficultyLevel);
        String subtitleText = difficultyTitle == null ? "" : difficultyTitle;

        if (settings.isVanillaStyle()) {
            minecraft.gui.setTimes(settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks());

            minecraft.gui.setSubtitle(Component.literal("\u2620 ").withStyle(skullColor(tier))
                    .append(Component.literal(subtitleText).withStyle(ChatFormatting.GRAY)));
            minecraft.gui.setTitle(Component.literal(regionName));
            return;
        }

        current = new Active(regionName, subtitleText, tier, now,
                settings.getFadeInTicks(), settings.getStayTicks(), settings.getFadeOutTicks(),
                settings.getScale(), settings.getYOffset());
    }
}
