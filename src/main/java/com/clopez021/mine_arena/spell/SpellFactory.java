package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.integration.meshy.MeshyExceptions;
import com.clopez021.mine_arena.network.PacketHandler;
import com.clopez021.mine_arena.network.SpellCompletePacket;
import com.clopez021.mine_arena.player.PlayerManager;
import com.clopez021.mine_arena.spell.config.PlayerSpellConfig;
import java.util.concurrent.CompletableFuture;
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

    System.out.println("Generating spell config for: " + spellDescription);

    CompletableFuture.supplyAsync(
            () -> {
              try {
                return LLMSpellConfigService.generateSpellFromLLM(spellDescription);
              } catch (MeshyExceptions.InvalidApiKeyException e) {
                System.err.println("Meshy invalid API key: " + e.getMessage());
                throw new SpellCreationException(
                    "Meshy API key missing or invalid. Configure it in settings.", e);
              } catch (MeshyExceptions.PaymentRequiredException e) {
                System.err.println("Meshy payment required: " + e.getMessage());
                throw new SpellCreationException(
                    "Meshy payment required. Add funds to continue.", e);
              } catch (MeshyExceptions.TooManyRequestsException e) {
                System.err.println("Meshy rate limited: " + e.getMessage());
                throw new SpellCreationException(
                    "Meshy rate limit hit. Please try again shortly.", e);
              } catch (MeshyExceptions.ServerErrorException e) {
                System.err.println("Meshy server error: " + e.getMessage());
                throw new SpellCreationException("Meshy server error. Please try again later.", e);
              } catch (IllegalArgumentException e) {
                System.err.println("Invalid LLM response: " + e.getMessage());
                throw new SpellCreationException(
                    "LLM returned invalid data. Please try rephrasing your spell.", e);
              } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                System.err.println("LLM/Meshy runtime error: " + msg);
                if (msg.contains("OpenRouter") || msg.contains("HTTP")) {
                  throw new SpellCreationException(
                      "LLM request failed. Check OpenRouter API key and connectivity.", e);
                } else {
                  throw new SpellCreationException("Spell generation failed. Please try again.", e);
                }
              } catch (Exception e) {
                System.err.println("Unexpected error generating spell config: " + e.getMessage());
                throw new SpellCreationException("Unexpected error during spell creation.", e);
              }
            })
        .thenAcceptAsync(
            result -> {
              // Execute back on server thread for Minecraft operations
              if (player.server != null) {
                player.server.execute(
                    () -> {
                      // Save a reusable PlayerSpellConfig for this player using LLM-generated name
                      String spellName =
                          result.name != null && !result.name.isEmpty() ? result.name : castPhrase;
                      PlayerSpellConfig reusable =
                          new PlayerSpellConfig(
                              spellName, castPhrase, result.config, result.cooldownSeconds);
                      PlayerManager.getInstance().addSpell(player, reusable);

                      // Notify client that spell creation finished successfully (no error)
                      sendComplete(player, "");
                    });
              }
            })
        .exceptionally(
            throwable -> {
              // Handle errors back on server thread
              if (player.server != null) {
                player.server.execute(
                    () -> {
                      String errorMessage =
                          throwable.getCause() instanceof SpellCreationException
                              ? throwable.getCause().getMessage()
                              : "Unexpected error during spell creation.";
                      sendComplete(player, errorMessage);
                    });
              }
              return null;
            });
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

  /** Custom exception to carry user-facing error messages from async thread. */
  private static class SpellCreationException extends RuntimeException {
    public SpellCreationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
