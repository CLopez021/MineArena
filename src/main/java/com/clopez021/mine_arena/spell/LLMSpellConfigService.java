package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.api.Meshy;
import com.clopez021.mine_arena.api.Message;
import com.clopez021.mine_arena.api.openrouter;
import com.clopez021.mine_arena.spell.behavior.onCollision.SpellEffectBehaviorConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Service for generating spell configs from an LLM using a 2-step process with shared context. */
public final class LLMSpellConfigService {
  private LLMSpellConfigService() {}

  /** Result containing both the spell name and config. */
  public static class SpellResult {
    public final String name;
    public final SpellEntityConfig config;

    public SpellResult(String name, SpellEntityConfig config) {
      this.name = name;
      this.config = config;
    }
  }

  /**
   * Main entry point: Generate a complete SpellEntityConfig using a 2-step process. All steps share
   * the same conversation context so the LLM can build on previous reasoning. Step 1: Generate
   * behavior config only Step 2: Generate entity visual/movement config + name (with Step 1
   * context) Then merge in code and validate
   */
  public static SpellResult generateSpellFromLLM(String spellIntent) throws Exception {
    if (spellIntent == null || spellIntent.isEmpty()) {
      throw new IllegalArgumentException("spellIntent cannot be empty");
    }

    // Shared conversation context for all steps
    List<Message> conversationHistory = new ArrayList<>();

    // Step 1: Generate behavior config
    JsonObject step1Json = executeStep1Behavior(spellIntent, conversationHistory);

    // Step 2: Generate entity config (conversation already has Step 1 context)
    JsonObject step2Json = executeStep2Entity(spellIntent, conversationHistory);

    // Merge the two JSON objects in code
    JsonObject finalConfigJson = mergeConfigs(step1Json, step2Json);

    // Validate the merged config
    validateFinalConfig(finalConfigJson);

    System.out.println("Final config: " + finalConfigJson.toString());
    // Parse the final merged config and create the SpellEntityConfig
    String spellName = getString(finalConfigJson, "name", spellIntent);
    SpellEntityConfig config = parseAndBuildFinalConfig(finalConfigJson);
    return new SpellResult(spellName, config);
  }

  // ---- STEP 1: Behavior Only ----

  private static JsonObject executeStep1Behavior(
      String spellIntent, List<Message> conversationHistory) {
    // Start with Step 1 system prompt
    conversationHistory.add(
        new Message("system", SpellPromptTemplates.step1BehaviorSystemPrompt()));
    conversationHistory.add(
        new Message("user", SpellPromptTemplates.step1BehaviorNotepadPrompt(spellIntent)));

    String reasoning;
    try {
      reasoning = openrouter.chat(conversationHistory);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Step 1 behavior reasoning: " + e);
    }

    // Add reasoning to shared context
    conversationHistory.add(new Message("assistant", reasoning));
    conversationHistory.add(new Message("user", SpellPromptTemplates.finalJsonOnlyInstruction()));

    String jsonResult;
    try {
      jsonResult = openrouter.chat(conversationHistory);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Step 1 behavior JSON: " + e);
    }

    // Add Step 1 JSON result to shared context
    conversationHistory.add(new Message("assistant", jsonResult));

    try {
      return parseObject(jsonResult);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Step 1 behavior JSON: " + e);
    }
  }

  // ---- STEP 2: Entity Visual/Movement ----

  private static JsonObject executeStep2Entity(
      String spellIntent, List<Message> conversationHistory) {
    // Now transition to Step 2 - conversation already contains all of Step 1
    conversationHistory.add(
        new Message(
            "user", "Now moving to STEP 2.\n\n" + SpellPromptTemplates.step2EntitySystemPrompt()));
    conversationHistory.add(
        new Message("user", SpellPromptTemplates.step2EntityNotepadPrompt(spellIntent)));

    String reasoning;
    try {
      reasoning = openrouter.chat(conversationHistory);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Step 2 entity reasoning: " + e);
    }

    // Add Step 2 reasoning to shared context
    conversationHistory.add(new Message("assistant", reasoning));
    conversationHistory.add(new Message("user", SpellPromptTemplates.finalJsonOnlyInstruction()));

    String jsonResult;
    try {
      jsonResult = openrouter.chat(conversationHistory);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Step 2 entity JSON: " + e);
    }

    // Add Step 2 JSON result to shared context
    conversationHistory.add(new Message("assistant", jsonResult));

    try {
      return parseObject(jsonResult);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Step 2 entity JSON: " + e);
    }
  }

  // ---- Merge Configs in Code ----

  private static JsonObject mergeConfigs(JsonObject step1Behavior, JsonObject step2Entity) {
    // Simply add the behavior object to the entity config
    step2Entity.add("behavior", step1Behavior);
    return step2Entity;
  }

  // ---- Validation ----

  private static void validateFinalConfig(JsonObject config) {
    // Validate structure exists
    if (!config.has("behavior")) {
      throw new IllegalArgumentException("Final config missing 'behavior' object");
    }

    JsonObject behavior = config.getAsJsonObject("behavior");

    // Validate non-negative numbers
    float radius = getFloat(behavior, "radius", 0.0f);
    if (radius <= 0.0f) {
      throw new IllegalArgumentException("radius must be > 0");
    }
  }

  // ---- Parse Final Config ----
  private static SpellEntityConfig parseAndBuildFinalConfig(JsonObject finalConfig)
      throws Exception {
    // Extract top-level fields (name is extracted separately in the main method)
    String prompt = getString(finalConfig, "prompt", null);
    if (prompt == null || prompt.isEmpty()) {
      throw new IllegalArgumentException("Final config missing 'prompt'");
    }

    float microScale = getFloat(finalConfig, "microScale", 1.0f);
    boolean shouldMove = getBool(finalConfig, "shouldMove", true);
    float speed = getFloat(finalConfig, "speed", 0.0f);

    // Extract behavior object and build SpellEffectBehaviorConfig
    JsonObject behaviorJson = finalConfig.getAsJsonObject("behavior");
    SpellEffectBehaviorConfig behavior = parseBehaviorFromJson(behaviorJson);

    // Generate blocks from prompt
    Map<BlockPos, BlockState> blocks = Meshy.buildBlocksFromPrompt(prompt);

    return new SpellEntityConfig(blocks, microScale, behavior, shouldMove, speed);
  }

  // ---- Parse Behavior From JSON ----
  private static SpellEffectBehaviorConfig parseBehaviorFromJson(JsonObject behaviorJson) {
    float radius = getFloat(behaviorJson, "radius", 2.0f);
    float damage = getFloat(behaviorJson, "damage", 0.0f);
    boolean despawnOnTrigger = getBool(behaviorJson, "despawnOnTrigger", true);
    String spawnEntityId = getString(behaviorJson, "spawnEntityId", "");
    int spawnEntityCount = getInt(behaviorJson, "spawnEntityCount", 0);
    String placeBlockId = getString(behaviorJson, "placeBlockId", "");
    int placeBlockCount = getInt(behaviorJson, "placeBlockCount", 0);
    boolean affectOwner = getBool(behaviorJson, "affectOwner", false);
    String statusEffectId = getString(behaviorJson, "statusEffectId", "");
    int statusDurationSeconds = getInt(behaviorJson, "statusDurationSeconds", 0);
    int statusAmplifier = getInt(behaviorJson, "statusAmplifier", 1);
    SpellEffectBehaviorConfig.EffectTrigger trigger =
        parseTrigger(getString(behaviorJson, "trigger", "onImpact"));
    float knockbackAmount = getFloat(behaviorJson, "knockbackAmount", 0.0f);
    float blockDestructionRadius = getFloat(behaviorJson, "blockDestructionRadius", 0.0f);
    int blockDestructionDepth = getInt(behaviorJson, "blockDestructionDepth", 0);

    return new SpellEffectBehaviorConfig(
        radius,
        damage,
        despawnOnTrigger,
        spawnEntityId,
        spawnEntityCount,
        placeBlockId,
        placeBlockCount,
        statusEffectId,
        statusDurationSeconds,
        statusAmplifier,
        affectOwner,
        trigger,
        knockbackAmount,
        blockDestructionRadius,
        blockDestructionDepth);
  }

  // ---- JSON helpers ----

  private static JsonObject parseObject(String json) {
    try {
      return JsonParser.parseString(json).getAsJsonObject();
    } catch (Exception e) {
      throw new IllegalArgumentException("LLM did not return JSON: " + json + " error:" + e);
    }
  }

  private static String getString(JsonObject o, String key, String defaultValue) {
    JsonElement e = o.get(key);
    return e != null && !e.isJsonNull() ? e.getAsString() : defaultValue;
  }

  private static float getFloat(JsonObject o, String key, float def) {
    JsonElement e = o.get(key);
    try {
      return e != null && !e.isJsonNull() ? e.getAsFloat() : def;
    } catch (Exception ex) {
      return def;
    }
  }

  private static int getInt(JsonObject o, String key, int def) {
    JsonElement e = o.get(key);
    try {
      return e != null && !e.isJsonNull() ? e.getAsInt() : def;
    } catch (Exception ex) {
      return def;
    }
  }

  private static boolean getBool(JsonObject o, String key, boolean def) {
    JsonElement e = o.get(key);
    try {
      return e != null && !e.isJsonNull() ? e.getAsBoolean() : def;
    } catch (Exception ex) {
      return def;
    }
  }

  private static SpellEffectBehaviorConfig.EffectTrigger parseTrigger(String v) {
    if (v == null) return SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT;
    String s = v.trim().toLowerCase(java.util.Locale.ROOT);
    return switch (s) {
      case "oncast", "on_cast", "cast" -> SpellEffectBehaviorConfig.EffectTrigger.ON_CAST;
      case "onimpact", "on_impact", "impact" -> SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT;
      default -> SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT;
    };
  }
}
