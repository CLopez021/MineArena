package com.clopez021.mine_arena.packets;


import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
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

        INSTANCE.messageBuilder(VoiceSidecarConfigPacket.class, ++index, NetworkDirection.PLAY_TO_CLIENT).encoder(VoiceSidecarConfigPacket::encode).decoder(VoiceSidecarConfigPacket::decode).consumerMainThread(VoiceSidecarConfigPacket::handle).add();
        INSTANCE.messageBuilder(RecognizedSpeechPacket.class, ++index, NetworkDirection.PLAY_TO_SERVER).encoder(RecognizedSpeechPacket::encode).decoder(RecognizedSpeechPacket::decode).consumerMainThread(RecognizedSpeechPacket::handle).add();
    }
}