package com.clopez021.mine_arena.spell;

/** Prompt builder utilities for spell-related LLM prompts. */
public final class SpellPromptTemplates {
  private SpellPromptTemplates() {}

  // ---- NEW 3-STEP APPROACH ----

  /** STEP 1: System prompt for behavior-only configuration. */
  public static String step1BehaviorSystemPrompt() {
    return ("You are configuring STEP 1: SpellEffectBehaviorConfig (behavior only).\n\n"
        + "Return ONLY a JSON object with EXACTLY these keys (no extras, no prose):\n"
        + "{\n"
        + "  radius: number (>0, area in blocks),\n"
        + "  damage: number (>=0),\n"
        + "  despawnOnTrigger: boolean,\n"
        + "  spawnId: string (\"\" for none),\n"
        + "  spawnCount: number (>=0; ignored if spawnId==\"\"),\n"
        + "  statusEffectId: string (\"\" for none),\n"
        + "  statusDurationSeconds: number (>=0),\n"
        + "  statusAmplifier: number (>=0),\n"
        + "  affectOwner: boolean,\n"
        + "  trigger: \"onCast\" | \"onImpact\",\n"
        + "  knockbackAmount: number (>=0),\n"
        + "  blockDestructionRadius: number (>=0),\n"
        + "  blockDestructionDepth: number (>=0)\n"
        + "}\n\n"
        + "Rules:\n"
        + "- trigger=onCast means effects occur immediately at the caster (no impact needed).\n"
        + "- trigger=onImpact means effects occur when the entity collides (requires travel later).\n"
        + "- Empty spawnId or statusEffectId means \"none\" for that effect.");
  }

  /** STEP 1: Notepad prompt for behavior reasoning. */
  public static String step1BehaviorNotepadPrompt(String playerIntent) {
    return ("STEP 1 Notepad (do NOT output JSON):\n"
        + "- Player intent: \"" + playerIntent + "\"\n"
        + "- Pick trigger (onCast vs onImpact) and justify.\n"
        + "- Choose radius, damage, status/spawn usage, knockback, and block destruction.\n"
        + "- Decide whether the owner can be affected (affectOwner).");
  }

  /** STEP 2: System prompt for entity presentation & motion (no behavior). */
  public static String step2EntitySystemPrompt() {
    return ("You are configuring STEP 2: SpellEntity (visual + movement), WITHOUT behavior.\n"
        + "Assume STEP 1 is already decided and available in context (especially trigger).\n\n"
        + "Return ONLY a JSON object with EXACTLY these keys (no extras, no prose):\n"
        + "{\n"
        + "  prompt: string (short description of the model look),\n"
        + "  microScale: number (~0.2 to 1.5),\n"
        + "  shouldMove: boolean,\n"
        + "  speed: number (>=0)\n"
        + "}\n\n"
        + "Rules:\n"
        + "- If STEP 1 chose trigger=\"onCast\": the effect is instant; set shouldMove=false by default (speed may be 0).\n"
        + "- If STEP 1 chose trigger=\"onImpact\": entity must be able to collide; set shouldMove=true and choose speed>0.");
  }

  /** STEP 2: Notepad prompt for entity reasoning. */
  public static String step2EntityNotepadPrompt(String playerIntent) {
    return ("STEP 2 Notepad (do NOT output JSON):\n"
        + "- Player intent: \"" + playerIntent + "\"\n"
        + "- Choose a concise visual prompt and scale.\n"
        + "- Set shouldMove/speed consistent with STEP 1 trigger.");
  }

  /** STEP 3: System prompt for final merge. */
  public static String step3MergeSystemPrompt() {
    return ("You are configuring STEP 3: Final merge.\n\n"
        + "Using the STEP 1 behavior JSON and the STEP 2 entity JSON already in context, return ONLY the final SpellEntityConfig object with EXACTLY this shape (no extras, no prose):\n\n"
        + "{\n"
        + "  \"prompt\": string,\n"
        + "  \"microScale\": number,\n"
        + "  \"behavior\": {\n"
        + "    \"radius\": number,\n"
        + "    \"damage\": number,\n"
        + "    \"despawnOnTrigger\": boolean,\n"
        + "    \"spawnId\": string,\n"
        + "    \"spawnCount\": number,\n"
        + "    \"statusEffectId\": string,\n"
        + "    \"statusDurationSeconds\": number,\n"
        + "    \"statusAmplifier\": number,\n"
        + "    \"affectOwner\": boolean,\n"
        + "    \"trigger\": \"onCast\" | \"onImpact\",\n"
        + "    \"knockbackAmount\": number,\n"
        + "    \"blockDestructionRadius\": number,\n"
        + "    \"blockDestructionDepth\": number\n"
        + "  },\n"
        + "  \"shouldMove\": boolean,\n"
        + "  \"speed\": number\n"
        + "}\n\n"
        + "Validation rules:\n"
        + "- If behavior.trigger=\"onImpact\": shouldMove MUST be true and speed > 0.\n"
        + "- If behavior.trigger=\"onCast\": shouldMove SHOULD be false (speed can be 0).\n"
        + "- Keep all numbers >= 0 where specified. Use empty strings \"\" for optional IDs when unused.");
  }

  /** Final instruction for JSON-only output (used in all steps). */
  public static String finalJsonOnlyInstruction() {
    return "Now output ONLY the final JSON object as specified. "
        + "Do NOT include any prose, markdown, or code fences/backticks. "
        + "Return pure JSON only: the first character MUST be '{' and the last MUST be '}'. "
        + "Example format (keys will differ): {\"exampleKey\": 1}";
  }

  // ---- LEGACY METHODS (kept for backward compatibility) ----

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
        + "  affectOwner: boolean (whether the spell can affect its owner),\n"
        + "  trigger: string (one of: onCast | onImpact),\n"
        + "  knockbackAmount: number (knockback force to apply to entities; 0 for none),\n"
        + "  blockDestructionRadius: number (radius of block destruction; 0 for none),\n"
        + "  blockDestructionDepth: number (layers of blocks to destroy; 0 for none)\n"
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
}
