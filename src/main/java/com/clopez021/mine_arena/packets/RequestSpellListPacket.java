package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.player.Player;
import com.clopez021.mine_arena.player.PlayerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class RequestSpellListPacket {
  public RequestSpellListPacket() {}

  public void encode(FriendlyByteBuf buf) {}

  public static RequestSpellListPacket decode(FriendlyByteBuf buf) {
    return new RequestSpellListPacket();
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ServerPlayer player = ctx.getSender();
    if (player == null) return;
    ctx.enqueueWork(
        () -> {
          Player p = PlayerManager.getInstance().getPlayer(player);
          if (p == null) {
            p = PlayerManager.getInstance().createPlayer(player);
          }
          PacketHandler.INSTANCE.send(
              new SpellInfoListPacket(p.getSpells()),
              net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
        });
    ctx.setPacketHandled(true);
  }
}
