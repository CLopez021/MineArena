package com.clopez021.mine_arena.network;

import com.clopez021.mine_arena.spell.SpellFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class WandSubmitPacket {
  private final String spellDescription;
  private final String castPhrase;

  public WandSubmitPacket(String spellDescription, String castPhrase) {
    this.spellDescription = spellDescription != null ? spellDescription : "";
    this.castPhrase = castPhrase != null ? castPhrase : "";
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(spellDescription);
    buf.writeUtf(castPhrase);
  }

  public static WandSubmitPacket decode(FriendlyByteBuf buf) {
    String description = buf.readUtf();
    String phrase = buf.readUtf();
    return new WandSubmitPacket(description, phrase);
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ServerPlayer player = ctx.getSender();
    if (player == null) return;
    ctx.enqueueWork(
        () -> {
          SpellFactory.createSpell(player, spellDescription, castPhrase);
        });
    ctx.setPacketHandled(true);
  }
}
