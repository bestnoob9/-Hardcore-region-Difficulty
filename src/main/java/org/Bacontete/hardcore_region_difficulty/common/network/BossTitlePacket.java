package org.Bacontete.hardcore_region_difficulty.common.network;

import net.minecraft.network.FriendlyByteBuf;
import org.Bacontete.hardcore_region_difficulty.common.entity.BossLevel;

public class BossTitlePacket {
    private final String entityName;

    private final BossLevel bossLevel;

    /** Icon of the boss ("" = default skull of the boss class) and title color (0xRRGGBB, -1 = default). */
    private final String icon;
    private final int titleColor;

    public BossTitlePacket(String entityName, BossLevel bossLevel, String icon, int titleColor) {
        this.icon = icon == null ? "" : icon;
        this.titleColor = titleColor;
        this.entityName = entityName == null ? "" : entityName;
        this.bossLevel = bossLevel;
    }

    public BossTitlePacket(FriendlyByteBuf buffer) {
        this.entityName = buffer.readUtf();
        this.bossLevel = buffer.readEnum(BossLevel.class);
        this.icon = buffer.readUtf();
        this.titleColor = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(entityName);
        buffer.writeEnum(bossLevel);
        buffer.writeUtf(icon);
        buffer.writeInt(titleColor);
    }

    public String getEntityName() {
        return entityName;
    }

    public BossLevel getBossLevel() {
        return bossLevel;
    }

    public String getIcon() {
        return icon;
    }

    public int getTitleColor() {
        return titleColor;
    }
}
