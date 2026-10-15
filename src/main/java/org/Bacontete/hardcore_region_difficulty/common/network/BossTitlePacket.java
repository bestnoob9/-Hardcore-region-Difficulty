package org.Bacontete.hardcore_region_difficulty.common.network;

import net.minecraft.network.FriendlyByteBuf;

public class BossTitlePacket {
    private final String entityName;
    private final String bossTypeKey;

    private final int bossLevel;

    public BossTitlePacket(String entityName, String bossTypeKey, int bossLevel) {
        this.entityName = entityName == null ? "" : entityName;
        this.bossTypeKey = bossTypeKey == null ? "" : bossTypeKey;
        this.bossLevel = bossLevel;
    }

    public BossTitlePacket(FriendlyByteBuf buffer) {
        this.entityName = buffer.readUtf();
        this.bossTypeKey = buffer.readUtf();
        this.bossLevel = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(entityName);
        buffer.writeUtf(bossTypeKey);
        buffer.writeVarInt(bossLevel);
    }

    public String getEntityName() {
        return entityName;
    }

    public String getBossTypeKey() {
        return bossTypeKey;
    }

    public int getBossLevel() {
        return bossLevel;
    }
}
