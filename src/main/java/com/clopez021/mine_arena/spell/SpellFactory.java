package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.api.MeshyExceptions;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.SpellCompletePacket;
import com.clopez021.mine_arena.player.PlayerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class SpellFactory {

  /** Create and spawn the spell entity using LLM-derived config, then notify client when done. */
  public static void createSpell(ServerPlayer player, String spellDescription, String castPhrase) {
    if (player == null || player.server == null) return;

    if (spellDescription.isEmpty()) {
      sendComplete(player, "Please enter a description for your spell.");
      return;
    }

    LLMSpellConfigService.SpellResult result;
    try {
      System.out.println("Generating spell config for: " + spellDescription);
      result = LLMSpellConfigService.generateSpellFromLLM(spellDescription);
    } catch (MeshyExceptions.InvalidApiKeyException e) {
      System.err.println("Meshy invalid API key: " + e.getMessage());
      sendComplete(player, "Meshy API key missing or invalid. Configure it in settings.");
      return;
    } catch (MeshyExceptions.PaymentRequiredException e) {
      System.err.println("Meshy payment required: " + e.getMessage());
      sendComplete(player, "Meshy payment required. Add funds to continue.");
      return;
    } catch (MeshyExceptions.TooManyRequestsException e) {
      System.err.println("Meshy rate limited: " + e.getMessage());
      sendComplete(player, "Meshy rate limit hit. Please try again shortly.");
      return;
    } catch (MeshyExceptions.ServerErrorException e) {
      System.err.println("Meshy server error: " + e.getMessage());
      sendComplete(player, "Meshy server error. Please try again later.");
      return;
    } catch (IllegalArgumentException e) {
      System.err.println("Invalid LLM response: " + e.getMessage());
      sendComplete(player, "LLM returned invalid data. Please try rephrasing your spell.");
      return;
    } catch (RuntimeException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
      System.err.println("LLM/Meshy runtime error: " + msg);
      if (msg.contains("OpenRouter") || msg.contains("HTTP")) {
        sendComplete(player, "LLM request failed. Check OpenRouter API key and connectivity.");
      } else {
        sendComplete(player, "Spell generation failed. Please try again.");
      }
      return;
    } catch (Exception e) {
      System.err.println("Unexpected error generating spell config: " + e.getMessage());
      sendComplete(player, "Unexpected error during spell creation.");
      return;
    }

    // Save a reusable PlayerSpellConfig for this player using LLM-generated name
    String spellName = result.name != null && !result.name.isEmpty() ? result.name : castPhrase;
    PlayerSpellConfig reusable = new PlayerSpellConfig(spellName, castPhrase, result.config);
    PlayerManager.getInstance().addSpell(player, reusable);

    // Notify client that spell creation finished successfully (no error)
    sendComplete(player, "");
  }

  private static void sendComplete(ServerPlayer player, String errorMessage) {
    player.server.execute(
        () -> {
          if (player.connection != null) {
            PacketHandler.INSTANCE.send(
                new SpellCompletePacket(errorMessage), PacketDistributor.PLAYER.with(player));
          }
        });
  }
}
