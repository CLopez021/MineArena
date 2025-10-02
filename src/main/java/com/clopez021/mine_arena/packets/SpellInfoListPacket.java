package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.client.WandScreens;
import com.clopez021.mine_arena.spell.PlayerSpellConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SpellInfoListPacket {
  // Simple data class for client display - no need to send full SpellEntityConfig
  public static class SpellInfo {
    public final String name;
    public final String phrase;

    public SpellInfo(String name, String phrase) {
      this.name = name;
      this.phrase = phrase;
    }
  }

  private final List<SpellInfo> spells;

  public SpellInfoListPacket(Collection<PlayerSpellConfig> spells) {
    this.spells = new ArrayList<>();
    for (PlayerSpellConfig spell : spells) {
      this.spells.add(new SpellInfo(spell.name(), spell.phrase()));
    }
  }

  private SpellInfoListPacket(List<SpellInfo> spells) {
    this.spells = spells;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeInt(spells.size());
    for (SpellInfo spell : spells) {
      buf.writeUtf(spell.name);
      buf.writeUtf(spell.phrase);
    }
  }

  public static SpellInfoListPacket decode(FriendlyByteBuf buf) {
    int size = buf.readInt();
    List<SpellInfo> spells = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      String name = buf.readUtf();
      String phrase = buf.readUtf();
      spells.add(new SpellInfo(name, phrase));
    }
    return new SpellInfoListPacket(spells);
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ctx.enqueueWork(
        () -> {
          WandScreens.setSpellList(spells);
        });
    ctx.setPacketHandled(true);
  }
}
