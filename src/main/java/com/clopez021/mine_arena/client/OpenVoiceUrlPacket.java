package com.clopez021.mine_arena.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/**
 * Packet to request the client to open the voice recognition URL.
 */
public class OpenVoiceUrlPacket {
    private final String url;

    public OpenVoiceUrlPacket(String url) {
        this.url = url;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
    }

    public static OpenVoiceUrlPacket decode(FriendlyByteBuf buf) {
        return new OpenVoiceUrlPacket(buf.readUtf());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Only execute on client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                VoiceSidecarUi.promptAndOpen(this.url);
            });
        });
        ctx.setPacketHandled(true);
    }
} 