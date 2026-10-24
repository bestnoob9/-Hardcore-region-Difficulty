package org.Bacontete.hardcore_region_difficulty.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityCategory;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends every quality definition (id, name, color and effects) of the server to a client, so the tooltip colors and
 * the attribute effects on the client always match the server's quality file. Chance weights stay on the server.
 */
public class QualitySyncPacket {

    private final List<QualityDefinition> definitions;

    public QualitySyncPacket(List<QualityDefinition> definitions) {
        this.definitions = definitions;
    }

    public QualitySyncPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<QualityDefinition> list = new ArrayList<>(count);
        QualityCategory[] categories = QualityCategory.values();
        for (int i = 0; i < count; i++) {
            int categoryIndex = buf.readVarInt();
            String id = buf.readUtf();
            String name = buf.readBoolean() ? buf.readUtf() : null;
            int rgb = buf.readInt();
            int tier = buf.readInt();
            double damage = buf.readDouble();
            double speed = buf.readDouble();
            double durability = buf.readDouble();
            double mining = buf.readDouble();
            double reach = buf.readDouble();
            double armorBonus = buf.readDouble();
            double moveSpeed = buf.readDouble();
            double holdMining = buf.readDouble();
            if (categoryIndex < 0 || categoryIndex >= categories.length) {
                continue;
            }
            list.add(new QualityDefinition(categories[categoryIndex], id, name, TextColor.fromRgb(rgb), tier,
                    0, QualityDefinition.WeightGroup.fromTier(tier), 0,
                    damage, speed, durability, mining, reach, armorBonus, moveSpeed, holdMining));
        }
        this.definitions = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(definitions.size());
        for (QualityDefinition d : definitions) {
            buf.writeVarInt(d.getCategory().ordinal());
            buf.writeUtf(d.name());
            String name = d.getCustomName();
            buf.writeBoolean(name != null);
            if (name != null) {
                buf.writeUtf(name);
            }
            buf.writeInt(d.getTextColor().getValue());
            buf.writeInt(d.getTier());
            buf.writeDouble(d.getDamagePercent());
            buf.writeDouble(d.getSpeedPercent());
            buf.writeDouble(d.getDurabilityPercent());
            buf.writeDouble(d.getMiningPercent());
            buf.writeDouble(d.getReach());
            buf.writeDouble(d.getArmorBonus());
            buf.writeDouble(d.getMoveSpeedPercent());
            buf.writeDouble(d.getHoldMiningPercent());
        }
    }

    public List<QualityDefinition> getDefinitions() {
        return definitions;
    }
}
