package org.Bacontete.hardcore_region_difficulty.forge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.forge.quality.QualityForgeMenu;

public class QualityForgeScreen extends AbstractContainerScreen<QualityForgeMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HardcoreRegionDifficulty.MODID, "textures/gui/quality_forge.png");

    public QualityForgeScreen(QualityForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 44;      
        this.titleLabelY = 15;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {

        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xF0E6D2, true);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB8B8C6, true);
        int xpCost = menu.getXpCost();
        if (xpCost > 0 && menu.getSlot(2).hasItem() && minecraft != null && minecraft.player != null) {
            boolean enough = minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= xpCost;
            graphics.drawString(font, Component.translatable("hardcore_region_difficulty.forge.xp_cost", xpCost),
                    120, 53, enough ? 0x80FF20 : 0xFF6060, true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
