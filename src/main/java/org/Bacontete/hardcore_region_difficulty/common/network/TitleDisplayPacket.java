package org.Bacontete.hardcore_region_difficulty.common.network;

import net.minecraft.network.FriendlyByteBuf;

public class TitleDisplayPacket {
    private final String regionName;
    private final String difficultyTitle;
    private final String structureName;

    private final int difficultyLevel;

    /** Icon of the region ("" = default skull of the level) and title color (0xRRGGBB, -1 = default). */
    private final String icon;
    private final int titleColor;

    public TitleDisplayPacket(String regionName, String difficultyTitle, String structureName, int difficultyLevel) {
        this(regionName, difficultyTitle, structureName, difficultyLevel, "", -1);
    }

    public TitleDisplayPacket(String regionName, String difficultyTitle, String structureName, int difficultyLevel,
                              String icon, int titleColor) {
        this.icon = icon == null ? "" : icon;
        this.titleColor = titleColor;
        this.difficultyLevel = difficultyLevel;
        this.regionName = regionName == null ? "" : regionName;
        this.difficultyTitle = difficultyTitle == null ? "" : difficultyTitle;
        this.structureName = structureName == null ? "" : structureName;
    }

    public TitleDisplayPacket(FriendlyByteBuf buffer) {
        this.regionName = buffer.readUtf();
        this.difficultyTitle = buffer.readUtf();
        this.structureName = buffer.readUtf();
        this.difficultyLevel = buffer.readVarInt();
        this.icon = buffer.readUtf();
        this.titleColor = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(regionName);
        buffer.writeUtf(difficultyTitle);
        buffer.writeUtf(structureName);
        buffer.writeVarInt(difficultyLevel);
        buffer.writeUtf(icon);
        buffer.writeInt(titleColor);
    }

    public String getRegionName() {
        return regionName;
    }

    public String getDifficultyTitle() {
        return difficultyTitle;
    }

    public String getStructureName() {
        return structureName;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public String getIcon() {
        return icon;
    }

    public int getTitleColor() {
        return titleColor;
    }
}
