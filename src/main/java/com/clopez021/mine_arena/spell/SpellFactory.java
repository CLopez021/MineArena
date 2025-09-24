package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.SpellCompletePacket;
import com.clopez021.mine_arena.player.PlayerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class SpellFactory {

  /** Create and spawn the spell entity using LLM-derived config, then notify client when done. */
  public static void createSpell(ServerPlayer player, String spellDescription, String castPhrase) {
    if (player == null || player.server == null) return;
    if (spellDescription == null || spellDescription.isEmpty()) return;

    SpellEntityConfig cfg;
    try {
      System.out.println("Generating spell config for: " + spellDescription);
      cfg = LLMSpellConfigService.generateSpellConfigFromLLM(spellDescription);
    } catch (Exception e) {
      System.err.println("Failed to generate spell config: " + e.getMessage());
      return;
    }

    // Save a reusable PlayerSpellConfig for this player using provided description/phrase
    PlayerSpellConfig reusable =
        new PlayerSpellConfig(castPhrase, castPhrase != null ? castPhrase : spellDescription, cfg);
    PlayerManager.getInstance().addSpell(player, reusable);

    // Notify client that spell creation finished
    player.server.execute(
        () -> {
          if (player.connection != null) {
            PacketHandler.INSTANCE.send(
                new SpellCompletePacket(), PacketDistributor.PLAYER.with(player));
          }
        });
  }
}
