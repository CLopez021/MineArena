package com.knkevin.ai_builder.packets;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.SimpleChannel;

/**
 * Register and build the different types of packets.
 */
public class PacketHandler {
    private static final int PROTOCOL_VERSION = 1;
    public static final SimpleChannel INSTANCE = ChannelBuilder.named(
            ResourceLocation.fromNamespaceAndPath("model_tools", "main")
    ).networkProtocolVersion(PROTOCOL_VERSION).simpleChannel();

    public static void init() {
        int index = -1;
        INSTANCE.messageBuilder(PlaceModelPacket.class, ++index, NetworkDirection.PLAY_TO_SERVER).encoder(PlaceModelPacket::encode).decoder(PlaceModelPacket::decode).consumerMainThread(PlaceModelPacket::handle).add();
        INSTANCE.messageBuilder(UndoModelPacket.class, ++index, NetworkDirection.PLAY_TO_SERVER).encoder(UndoModelPacket::encode).decoder(UndoModelPacket::decode).consumerMainThread(UndoModelPacket::handle).add();
    }
}