package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.api.Message;
import com.clopez021.mine_arena.api.openrouter;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfigBuilder;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorRequest;

/**
 * Orchestrates a one-shot LLM prompt to produce a {@link UnifiedSpellConfigRequest} JSON, then
 * builds a {@link SpellEntityConfig} by generating the model via Meshy and attaching the specified
 * collision behavior.
 */
public final class LLMSpellConfigService {
  private LLMSpellConfigService() {}

  /**
   * Call the LLM with instructions and user intent and build a full SpellEntityConfig.
   *
   * @param systemPrompt High-level instructions for the LLM about the JSON format
   * @param userIntent Natural language description (what to create)
   */
  public static SpellEntityConfig generateFromLLM(String systemPrompt, String userIntent)
      throws Exception {
    if (userIntent == null || userIntent.isEmpty()) {
      throw new IllegalArgumentException("userIntent cannot be empty");
    }

    String assistant =
        openrouter.chat(
            java.util.List.of(
                new Message("system", systemPrompt != null ? systemPrompt : defaultSystemPrompt()),
                new Message("user", userIntent)));

    UnifiedSpellConfigRequest req = UnifiedSpellConfigRequest.fromJson(assistant);

    CollisionBehaviorRequest cReq = req.getCollision();
    CollisionBehaviorConfig behavior = CollisionBehaviorConfigBuilder.fromFull(cReq);

    SpellEntityConfigRequest sReq =
        new SpellEntityConfigRequest(
            req.getPrompt(),
            req.getMicroScale(),
            req.getMovementDirection(),
            req.getMovementSpeed());

    return SpellEntityConfigBuilder.build(sReq, behavior);
  }

  private static String defaultSystemPrompt() {
    return "You are a helpful assistant that outputs ONLY valid JSON matching this schema: "
        + "{\n"
        + "  prompt: string,\n"
        + "  microScale: number,\n"
        + "  movementDirection: one of [FORWARD,BACKWARD,UP,DOWN,NONE],\n"
        + "  speed: number,\n"
        + "  collision: {\n"
        + "    collisionBehaviorName: string (one of [explode,shockwave,none]),\n"
        + "    radius: number,\n"
        + "    damage: number,\n"
        + "    shouldDespawn: boolean,\n"
        + "    spawnId: string,\n"
        + "    spawnCount: number,\n"
        + "    effectId: string,\n"
        + "    effectDuration: number,\n"
        + "    effectAmplifier: number,\n"
        + "    affectPlayer: boolean\n"
        + "  }\n"
        + "}. Do not include any commentary.";
  }
}
