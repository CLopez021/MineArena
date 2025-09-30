package com.clopez021.mine_arena.spell;

/** Prompt builder utilities for spell-related LLM prompts. */
public final class SpellPromptTemplates {
  private SpellPromptTemplates() {}

  /** System prompt for effect behavior configuration. */
  public static String collisionBehaviorSystemPrompt() {
    return ("You are configuring a Minecraft spell's EFFECT behavior. "
        + "Use the player's intent to choose appropriate parameters. "
        + "Return a JSON object with these keys:\n"
        + "{\n"
        + "  radius: number (effect radius in blocks; area of influence),\n"
        + "  damage: number (damage to entities within radius; higher means more damage),\n"
        + "  despawnOnTrigger: boolean (whether spell projectile should despawn after effect),\n"
        + "  spawnId: string (optional entity or block identifier to spawn/place; empty for none),\n"
        + "  spawnCount: number (how many to spawn/place if spawnId is provided),\n"
        + "  statusEffectId: string (optional status effect id or keyword; empty for none),\n"
        + "  statusDurationSeconds: number (duration of status effect, in SECONDS),\n"
        + "  statusAmplifier: number (strength/amplifier of the status effect),\n"
        + "  affectPlayer: boolean (whether the spell can affect its owner),\n"
        + "  trigger: string (one of: onCast | onImpact),\n"
        + "  knockbackAmount: number (knockback force to apply to entities; 0 for none),\n"
        + "  breakBlocks: boolean (whether to break blocks within effect radius)\n"
        + "}\n\n"
        + "Be creative and match the intent. Consider realistic game balance.");
  }

  /** System prompt for spell entity model/movement configuration. */
  public static String spellEntitySystemPrompt() {
    return ("You are configuring a Minecraft spell entity's MODEL and MOVEMENT. "
        + "Use the player's intent to choose appropriate parameters.\n\n"
        + "When asked for FINAL OUTPUT, return ONLY a single JSON object with EXACTLY these keys: \n"
        + "{\n"
        + "  prompt: string (short, descriptive prompt for block-based model generation),\n"
        + "  microScale: number (visual scale factor for the model; typical ~0.2 to 1.5),\n"
        + "  shouldMove: boolean (whether the entity moves after being cast),"
        + " this can be set to false if the player wants the spell to be stationary, for example, a spell that creates a stationary spawner.\n"
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
    return "Now output ONLY the final JSON object as specified. "
        + "Do NOT include any prose, markdown, or code fences/backticks. "
        + "Return pure JSON only: the first character MUST be '{' and the last MUST be '}'. "
        + "Example format (keys will differ): {\"exampleKey\": 1}";
  }
}
