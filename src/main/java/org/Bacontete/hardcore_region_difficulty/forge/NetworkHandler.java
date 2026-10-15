package org.Bacontete.hardcore_region_difficulty.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.client.TitleDisplayHandler;
import org.Bacontete.hardcore_region_difficulty.common.network.BossTitlePacket;
import org.Bacontete.hardcore_region_difficulty.common.network.TitleDisplayPacket;

import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "3"; 
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(HardcoreRegionDifficulty.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.messageBuilder(TitleDisplayPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TitleDisplayPacket::encode)
                .decoder(TitleDisplayPacket::new)
                .consumerMainThread(NetworkHandler::handleTitleDisplay)
                .add();
        INSTANCE.messageBuilder(BossTitlePacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BossTitlePacket::encode)
                .decoder(BossTitlePacket::new)
                .consumerMainThread(NetworkHandler::handleBossTitle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, TitleDisplayPacket packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, BossTitlePacket packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static void handleTitleDisplay(TitleDisplayPacket packet, Supplier<NetworkEvent.Context> context) {

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                TitleDisplayHandler.displayTitle(packet.getRegionName(), packet.getDifficultyTitle(),
                        packet.getStructureName(), packet.getDifficultyLevel()));
        context.get().setPacketHandled(true);
    }

    private static void handleBossTitle(BossTitlePacket packet, Supplier<NetworkEvent.Context> context) {

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                TitleDisplayHandler.displayBossTitle(packet.getEntityName(), packet.getBossTypeKey(), packet.getBossLevel()));
        context.get().setPacketHandled(true);
    }
}
