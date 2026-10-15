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

@Mod.EventBusSubscriber(modid = HardcoreRegionDifficulty.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RegionTitleOverlay {
    private RegionTitleOverlay() {}

    private static final ResourceLocation[] SKULLS = new ResourceLocation[TitleDisplayHandler.SKULL_TIERS];
    static {
        for (int i = 0; i < SKULLS.length; i++) {
            SKULLS[i] = ResourceLocation.fromNamespaceAndPath(HardcoreRegionDifficulty.MODID, "textures/gui/skull_" + (i + 1) + ".png");
        }
    }
    private static final int SKULL_TEXTURE_SIZE = 12;

    private static final int SKULL_SIZE = 11;
    private static final int SKULL_GAP = 3;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("region_title", RegionTitleOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
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
        pose.pushPose();
        pose.translate(centerX, y, 0f);
        pose.scale(subScale, subScale, 1f);

        int textWidth = font.width(active.subtitle());
        int totalWidth = SKULL_SIZE + SKULL_GAP + textWidth;
        int left = -totalWidth / 2;
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(SKULLS[active.skullTier() - 1], left, -1, SKULL_SIZE, SKULL_SIZE,
                0f, 0f, SKULL_TEXTURE_SIZE, SKULL_TEXTURE_SIZE, SKULL_TEXTURE_SIZE, SKULL_TEXTURE_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.drawString(font, active.subtitle(), left + SKULL_SIZE + SKULL_GAP, 1, (a << 24) | 0xBBBBBB, true);
        pose.popPose();

        RenderSystem.disableBlend();
    }
}
