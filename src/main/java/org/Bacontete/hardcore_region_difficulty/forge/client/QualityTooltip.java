package org.Bacontete.hardcore_region_difficulty.forge.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.quality.ItemQuality;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityConfig;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityType;

@Mod.EventBusSubscriber(modid = HardcoreRegionDifficulty.MODID, value = Dist.CLIENT)
public final class QualityTooltip {

    private QualityTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!QualityConfig.isEnabled()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        QualityType quality = ItemQuality.find(stack);
        if (quality == null) {
            return;
        }
        event.getToolTip().add(1, Component.translatable("hardcore_region_difficulty.quality.prefix")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "))
                .append(quality.getDisplayName()));
        int line = 2;
        if (ItemQuality.isRanged(stack)) {
            String key = stack.getItem() instanceof CrossbowItem ? "load_speed" : "draw_speed";
            line = addEffect(event, line, key, quality.getSpeedPercent());
            line = addEffect(event, line, "arrow_damage", quality.getDamagePercent());
        } else {
            if (ItemQuality.isTool(stack)) {
                line = addEffect(event, line, "mining_speed", quality.getMiningPercent());
            }
            if (ItemQuality.isWeapon(stack)) {
                line = addEffect(event, line, "damage", quality.getDamagePercent());
                line = addEffect(event, line, "attack_speed", quality.getSpeedPercent());
            }
        }
        if (ItemQuality.isWeapon(stack) || ItemQuality.isTool(stack)) {
            line = addFlat(event, line, "reach", quality.getReach());
        }
        if (ItemQuality.isArmor(stack)) {
            line = addEffect(event, line, "armor", quality.getDamagePercent());
            line = addEffect(event, line, "toughness", quality.getDamagePercent());
            line = addFlat(event, line, "armor_bonus", quality.getArmorBonus());
        }
        if (ItemQuality.isShield(stack)) {
            line = addFlat(event, line, "armor_bonus", quality.getArmorBonus());
            line = addEffect(event, line, "move_speed", quality.getMoveSpeedPercent());
            line = addEffect(event, line, "hold_mining", quality.getHoldMiningPercent());
        }
        if (stack.isDamageableItem()) {
            addEffect(event, line, "durability", quality.getDurabilityPercent());
        }
    }

    private static int addEffect(ItemTooltipEvent event, int line, String key, double fraction) {
        long percent = Math.round(fraction * 100.0D);
        if (percent == 0) {
            return line;
        }
        event.getToolTip().add(line, Component.translatable("hardcore_region_difficulty.quality.effect." + key,
                (percent > 0 ? "+" : "") + percent + "%").withStyle(percent > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        return line + 1;
    }

    private static int addFlat(ItemTooltipEvent event, int line, String key, double value) {
        long tenths = Math.round(value * 10.0D);
        if (tenths == 0) {
            return line;
        }
        event.getToolTip().add(line, Component.translatable("hardcore_region_difficulty.quality.effect." + key,
                (tenths > 0 ? "+" : "") + (tenths / 10.0D)).withStyle(tenths > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        return line + 1;
    }
}
