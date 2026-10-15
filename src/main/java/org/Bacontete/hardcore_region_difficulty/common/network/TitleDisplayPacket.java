package org.Bacontete.hardcore_region_difficulty.common.network;

import net.minecraft.network.FriendlyByteBuf;

public class TitleDisplayPacket {
    private final String regionName;
    private final String difficultyTitle;
    private final String structureName;

    private final int difficultyLevel;

    public TitleDisplayPacket(String regionName, String difficultyTitle, String structureName, int difficultyLevel) {
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
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(regionName);
        buffer.writeUtf(difficultyTitle);
        buffer.writeUtf(structureName);
        buffer.writeVarInt(difficultyLevel);
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
}
