package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.player.PlayerManager;
import com.clopez021.mine_arena.speech_recognition.SpeechCommand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client -> Server: Carry recognized speech result (spell name). */
public class RecognizedSpeechPacket {
  private final String spellName;

  public RecognizedSpeechPacket(String spellName) {
    this.spellName = spellName;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(spellName != null ? spellName : "");
  }

  public static RecognizedSpeechPacket decode(FriendlyByteBuf buf) {
    String spellName = buf.readUtf();
    return new RecognizedSpeechPacket(spellName);
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ServerPlayer player = ctx.getSender();
    if (player == null) return;
    ctx.enqueueWork(
        () -> {
          // Create simplified SpeechCommand and delegate to PlayerManager
          SpeechCommand command = new SpeechCommand(player.getUUID(), spellName);
          PlayerManager.getInstance().handleSpeechCommand(player, command);
        });
    ctx.setPacketHandled(true);
  }
}
