package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.api.Meshy;
import com.clopez021.mine_arena.api.Message;
import com.clopez021.mine_arena.api.openrouter;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
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

  // Step 1a: Get collision behavior from LLM as JSON and build the config
  public static CollisionBehaviorConfig generateCollisionBehaviorFromLLM(String spellIntent) {
    if (spellIntent == null || spellIntent.isEmpty()) {
      throw new IllegalArgumentException("spellIntent cannot be empty");
    }

    // Two-step: reasoning then final JSON
    String system = SpellPromptTemplates.collisionBehaviorSystemPrompt();
    List<Message> messages = new ArrayList<>();
    messages.add(new Message("system", system));
    messages.add(
        new Message(
            "user",
            SpellPromptTemplates.reasoningNotepadPrompt(spellIntent, "Collision Behavior")));

    String llm_reasoning;
    try {
      llm_reasoning = openrouter.chat(messages);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get collision behavior reasoning: " + e);
    }

    messages.add(new Message("assistant", llm_reasoning));
    messages.add(new Message("user", SpellPromptTemplates.finalJsonOnlyInstruction()));

    String llm_config_result;
    try {
      llm_config_result = openrouter.chat(messages);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get collision behavior assistant: " + e);
    }

    JsonObject json_config;
    try {
      json_config = parseObject(llm_config_result);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse collision behavior: " + e);
    }

    String name;
    float radius;
    float damage;
    boolean shouldDespawn;
    String spawnId;
    int spawnCount;
    boolean affectPlayer;
    String effectId;
    int effectDuration;
    int effectAmplifier;
    boolean triggersInstantly;
    try {
      name = firstPresentString(json_config, "collisionBehaviorName", "name", "");
      radius = getFloat(json_config, "radius", 0.0f);
      damage = getFloat(json_config, "damage", 0.0f);
      shouldDespawn = getBool(json_config, "shouldDespawn", false);
      spawnId = firstPresentString(json_config, "spawnEntityID", "spawnId", "");
      spawnCount = getInt(json_config, "spawnCount", 0);
      affectPlayer = getBool(json_config, "affectPlayer", false);
      effectId = getString(json_config, "effectId", "");
      effectDuration = getInt(json_config, "effectDuration", 0);
      effectAmplifier = getInt(json_config, "effectAmplifier", 0);
      triggersInstantly = getBool(json_config, "triggersInstantly", false);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse collision behavior: " + e);
    }

    return new CollisionBehaviorConfig(
        name,
        radius,
        damage,
        shouldDespawn,
        spawnId,
        spawnCount,
        effectId,
        effectDuration,
        effectAmplifier,
        affectPlayer,
        triggersInstantly);
  }

  /** Step 1b: Generate model + full SpellEntityConfig using a second LLM call (movement/model). */
  public static SpellEntityConfig generateSpellConfigFromLLM(String spellIntent) throws Exception {
    if (spellIntent == null || spellIntent.isEmpty()) {
      throw new IllegalArgumentException("spellIntent cannot be empty");
    }

    // First, build collision behavior via LLM
    CollisionBehaviorConfig cb = generateCollisionBehaviorFromLLM(spellIntent);

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

  private static String firstPresentString(JsonObject o, String k1, String k2, String def) {
    String v1 = getString(o, k1, null);
    if (v1 != null && !v1.isEmpty()) return v1;
    String v2 = getString(o, k2, null);
    if (v2 != null && !v2.isEmpty()) return v2;
    return def;
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
}
