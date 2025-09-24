package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.client.WandScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class SpellCompletePacket {
  private final String errorMessage; // null or empty means success

  public SpellCompletePacket() {
    this("");
  }

  public SpellCompletePacket(String errorMessage) {
    this.errorMessage = errorMessage != null ? errorMessage : "";
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(errorMessage);
  }

  public static SpellCompletePacket decode(FriendlyByteBuf buf) {
    String err = buf.readUtf();
    return new SpellCompletePacket(err);
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ctx.enqueueWork(
        () ->
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () -> {
                      if (errorMessage != null && !errorMessage.isEmpty()) {
                        WandScreens.onSpellErrorClient(errorMessage);
                      } else {
                        WandScreens.onSpellCompleteClient();
                      }
                    }));
    ctx.setPacketHandled(true);
  }
}
