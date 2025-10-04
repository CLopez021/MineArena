package com.clopez021.mine_arena.network;

import com.clopez021.mine_arena.voice.recognition.client.VoiceSidecar;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/** Server -> Client: Signal the client to stop the voice sidecar (and close tab). */
public class VoiceSidecarStopPacket {
  public VoiceSidecarStopPacket() {}

  public void encode(FriendlyByteBuf buf) {}

  public static VoiceSidecarStopPacket decode(FriendlyByteBuf buf) {
    return new VoiceSidecarStopPacket();
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ctx.enqueueWork(
        () ->
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () -> {
                      var player = Minecraft.getInstance().player;
                      if (player == null) return;
                      try {
                        VoiceSidecar.getInstance().stop();
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    }));
    ctx.setPacketHandled(true);
  }
}
