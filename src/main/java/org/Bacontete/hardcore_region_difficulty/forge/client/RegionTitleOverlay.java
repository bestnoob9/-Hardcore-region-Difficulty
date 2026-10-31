package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.client.TitleDisplayHandler;
import org.Bacontete.hardcore_region_difficulty.common.config.DifficultyIcons;
import org.Bacontete.hardcore_region_difficulty.forge.compat.TravelersTitlesCompat;

@Mod.EventBusSubscriber(modid = HardcoreRegionDifficulty.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RegionTitleOverlay {
    private RegionTitleOverlay() {}

    private static final int SKULL_SIZE = 16;
    private static final int SKULL_GAP = 3;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("region_title", RegionTitleOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        // Traveler's Titles installed: its title is shown, HRD only adds the difficulty line under it.
        // Boss titles are not region titles: they are still drawn by HRD below (TitleDisplayHandler.current()).
        if (TravelersTitlesCompat.isIntegrated()) {
            TravelersTitlesCompat.render(graphics, partialTick, width, height);
        }
        TitleDisplayHandler.Active active = TitleDisplayHandler.current();
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || minecraft.options.hideGui) {
            return;
        }

        float fadeIn = Math.max(1, active.fadeInTicks()) * 50f;
        float stay = active.stayTicks() * 50f;
        float fadeOut = Math.max(1, active.fadeOutTicks()) * 50f;
        float elapsed = System.currentTimeMillis() - active.startMillis();
        if (elapsed >= fadeIn + stay + fadeOut) {
            TitleDisplayHandler.clear();
            return;
        }
        float alpha;
        if (elapsed < fadeIn) {
            alpha = elapsed / fadeIn;
        } else if (elapsed < fadeIn + stay) {
            alpha = 1f;
        } else {
            alpha = 1f - (elapsed - fadeIn - stay) / fadeOut;
        }
        int a = (int) (alpha * 255f);
        if (a < 8) {
            return; 
        }

        Font font = minecraft.font;
        PoseStack pose = graphics.pose();
        float scale = (float) active.scale();
        float nameScale = scale * 1.25f;
        float subScale = scale * 0.9f;
        float centerX = width / 2f;
        float y = active.yOffset();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        pose.pushPose();
        pose.translate(centerX, y, 0f);
        pose.scale(nameScale, nameScale, 1f);
        graphics.drawString(font, active.name(), -font.width(active.name()) / 2, 0, (a << 24) | 0xFFFFFF, true);
        pose.popPose();

        y += font.lineHeight * nameScale + 2f;
        drawDifficultyLine(graphics, font, active.subtitle(), active.icon(), active.skullIcon(), active.color(),
                centerX, y, subScale, alpha);

        RenderSystem.disableBlend();
    }

    /**
     * Draws "[skull] difficulty title" centered on centerX, top at y. Shared by the normal HRD title and by the
     * Traveler's Titles integration.
     */
    public static void drawDifficultyLine(GuiGraphics graphics, Font font, String subtitle, String icon, String skullIcon,
                                          int color, float centerX, float y, float scale, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        PoseStack pose = graphics.pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        pose.pushPose();
        pose.translate(centerX, y, 0f);
        pose.scale(scale, scale, 1f);

        int textWidth = font.width(subtitle);
        int totalWidth = SKULL_SIZE + SKULL_GAP + textWidth;
        int left = -totalWidth / 2;
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ResourceLocation custom = DifficultyIcons.texture(icon);
        ResourceLocation iconTexture = custom != null && IconFolder.ensure(custom)
                ? custom : DifficultyIcons.texture(skullIcon);
        // UV size 1/1 of a 1x1 "texture" = the whole image, so any square size works (12x12 built-in, 32x32, 64x64 ...)
        graphics.blit(iconTexture, left, -3, SKULL_SIZE, SKULL_SIZE,
                0f, 0f, 1, 1, 1, 1);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.drawString(font, subtitle, left + SKULL_SIZE + SKULL_GAP, 1,
                (a << 24) | (color >= 0 ? color : 0xBBBBBB), true);
        pose.popPose();
        RenderSystem.disableBlend();
    }
}
