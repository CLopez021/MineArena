package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.spell.behavior.onCollision.OnCollisionBehaviors;
import java.util.stream.Collectors;

/** Prompt builder utilities for spell-related LLM prompts. */
public final class SpellPromptTemplates {
  private SpellPromptTemplates() {}

  /**
   * System prompt for collision behavior configuration. Dynamically lists valid behavior names and
   * their brief descriptions from the registry.
   */
  public static String collisionBehaviorSystemPrompt() {
    String behaviors =
        OnCollisionBehaviors.registry().entrySet().stream()
            .map(e -> e.getKey() + ": " + safeDesc(e.getValue().description()))
            .collect(Collectors.joining("; "));
    return ("You are configuring a Minecraft spell's ON-COLLISION behavior. "
        + "Use the player's intent to choose appropriate parameters. "
        + "Valid collisionBehaviorName values with descriptions: "
        + behaviors
        + ".\n\n"
        + "When asked for FINAL OUTPUT, return ONLY a single JSON object with EXACTLY these keys: \n"
        + "{\n"
        + "  collisionBehaviorName: string (choose one of the names listed above),\n"
        + "  radius: number (effect radius in blocks; area of influence),\n"
        + "  damage: number (damage to entities within radius; higher means more damage),\n"
        + "  shouldDespawn: boolean (whether the spell entity is removed after triggering),\n"
        + "  spawnEntityID: string (optional entity identifier to spawn on collision; empty for none),\n"
        + "  spawnCount: number (how many entities to spawn if spawnEntityID is provided),\n"
        + "  affectPlayer: boolean (whether the spell can affect its owner),\n"
        + "  effectId: string (optional status effect id to apply; empty for none),\n"
        + "  effectDuration: number (duration of effect, in ticks),\n"
        + "  effectAmplifier: number (strength/amplifier of the effect),\n"
        + "  triggersInstantly: boolean (if true, trigger immediately on impact)\n"
        + "}.\n"
        + "Do not include prose, code fences, or extra keys in the FINAL OUTPUT.");
  }

  /** System prompt for spell entity model/movement configuration. */
  public static String spellEntitySystemPrompt() {
    return ("You are configuring a Minecraft spell entity's MODEL and MOVEMENT. "
        + "Use the player's intent to choose appropriate parameters.\n\n"
        + "When asked for FINAL OUTPUT, return ONLY a single JSON object with EXACTLY these keys: \n"
        + "{\n"
        + "  prompt: string (short, descriptive prompt for block-based model generation),\n"
        + "  microScale: number (visual scale factor for the model; typical ~0.2 to 1.5),\n"
        + "  shouldMove: boolean (whether the entity moves after being cast),\n"
        + "  speed: number (movement speed scalar; larger is faster)\n"
        + "}.\n"
        + "Do not include prose, code fences, or extra keys in the FINAL OUTPUT.");
  }

  /** User prompt instructing the model to write detailed notes and reasoning only. */
  public static String reasoningNotepadPrompt(String playerIntent, String scopeLabel) {
    return ("Problem ("
        + scopeLabel
        + "): Based on the player's intent below, design an appropriate configuration.\n"
        + "Player intent: \""
        + playerIntent
        + "\"\n\n"
        + "Write detailed notes and reasoning ONLY. Be explicit about: goals, constraints, chosen values, "
        + "alternatives considered, trade-offs, and assumptions. Do NOT output JSON yet. Use this as your notepad");
  }

  /** Final instruction asking for the JSON only. */
  public static String finalJsonOnlyInstruction() {
    return "Now output ONLY the final JSON object as specified. No prose or code fences.";
  }

  private static String safeDesc(String s) {
    if (s == null) return "";
    return s.replace(';', ',').replace('\n', ' ').trim();
  }
}
