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

/** Minimal service for generating spell configs from an LLM in two steps, parsing raw JSON. */
public final class LLMSpellConfigService {
  private LLMSpellConfigService() {}

  // Step 1a: Get effect behavior from LLM as JSON and build the config
  public static SpellEffectBehaviorConfig generateEffectBehaviorFromLLM(String spellIntent) {
    if (spellIntent == null || spellIntent.isEmpty()) {
      throw new IllegalArgumentException("spellIntent cannot be empty");
    }

    // Two-step: reasoning then final JSON
    String system = SpellPromptTemplates.collisionBehaviorSystemPrompt();
    List<Message> messages = new ArrayList<>();
    messages.add(new Message("system", system));
    messages.add(
        new Message(
            "user", SpellPromptTemplates.reasoningNotepadPrompt(spellIntent, "Effect Behavior")));

    String llm_reasoning;
    try {
      llm_reasoning = openrouter.chat(messages);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get effect behavior reasoning: " + e);
    }

    messages.add(new Message("assistant", llm_reasoning));
    messages.add(new Message("user", SpellPromptTemplates.finalJsonOnlyInstruction()));

    String llm_config_result;
    try {
      llm_config_result = openrouter.chat(messages);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get effect behavior assistant: " + e);
    }

    JsonObject json_config;
    try {
      json_config = parseObject(llm_config_result);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse effect behavior: " + e);
    }

    float radius;
    float damage;
    boolean despawnOnTrigger;
    String spawnId;
    int spawnCount;
    boolean affectPlayer;
    String statusEffectId;
    int statusDurationSeconds;
    int statusAmplifier;
    SpellEffectBehaviorConfig.EffectTrigger trigger;
    float knockbackAmount;
    float blockDestructionRadius;
    int blockDestructionDepth;
    try {
      radius = getFloat(json_config, "radius", 0.0f);
      damage = getFloat(json_config, "damage", 0.0f);
      despawnOnTrigger = getBool(json_config, "despawnOnTrigger", false);
      spawnId = getString(json_config, "spawnId", "");
      spawnCount = getInt(json_config, "spawnCount", 0);
      affectPlayer = getBool(json_config, "affectPlayer", false);
      statusEffectId = getString(json_config, "statusEffectId", "");
      statusDurationSeconds = getInt(json_config, "statusDurationSeconds", 0);
      statusAmplifier = getInt(json_config, "statusAmplifier", 0);
      trigger = parseTrigger(getString(json_config, "trigger", "onImpact"));
      knockbackAmount = getFloat(json_config, "knockbackAmount", 0.0f);
      blockDestructionRadius = getFloat(json_config, "blockDestructionRadius", 0.0f);
      blockDestructionDepth = getInt(json_config, "blockDestructionDepth", 0);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse effect behavior: " + e);
    }

    return new SpellEffectBehaviorConfig(
        radius,
        damage,
        despawnOnTrigger,
        spawnId,
        spawnCount,
        statusEffectId,
        statusDurationSeconds,
        statusAmplifier,
        affectPlayer,
        trigger,
        knockbackAmount,
        blockDestructionRadius,
        blockDestructionDepth);
  }

  /** Step 1b: Generate model + full SpellEntityConfig using a second LLM call (movement/model). */
  public static SpellEntityConfig generateSpellConfigFromLLM(String spellIntent) throws Exception {
    if (spellIntent == null || spellIntent.isEmpty()) {
      throw new IllegalArgumentException("spellIntent cannot be empty");
    }

    // First, build effect behavior via LLM
    SpellEffectBehaviorConfig cb = generateEffectBehaviorFromLLM(spellIntent);

    // Then, build movement/model info via a separate two-step LLM flow
    String system = SpellPromptTemplates.spellEntitySystemPrompt();
    List<Message> messages = new ArrayList<>();
    messages.add(new Message("system", system));
    messages.add(
        new Message(
            "user",
            SpellPromptTemplates.reasoningNotepadPrompt(spellIntent, "Model and Movement")));
    String llm_reasoning;
    try {
      llm_reasoning = openrouter.chat(messages);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get spell entity reasoning: " + e);
    }

    messages.add(new Message("assistant", llm_reasoning));
    messages.add(new Message("user", SpellPromptTemplates.finalJsonOnlyInstruction()));

    String llm_config_result;
    try {
      llm_config_result = openrouter.chat(messages);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get spell entity assistant: " + e);
    }

    JsonObject json_config;
    try {
      json_config = parseObject(llm_config_result);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse spell entity config: " + e);
    }

    String prompt = getString(json_config, "prompt", null);
    if (prompt == null || prompt.isEmpty())
      throw new IllegalArgumentException("LLM missing 'prompt'");
    float microScale = getFloat(json_config, "microScale", 1.0f);
    boolean shouldMove = getBool(json_config, "shouldMove", true);
    float speed = getFloat(json_config, "speed", 0.0f);

    Map<BlockPos, BlockState> blocks = Meshy.buildBlocksFromPrompt(prompt);

    return new SpellEntityConfig(blocks, microScale, cb, shouldMove, speed);
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
