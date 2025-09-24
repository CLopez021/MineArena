package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.api.Meshy;
import com.clopez021.mine_arena.api.Message;
import com.clopez021.mine_arena.api.openrouter;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    String assistant =
        openrouter.chat(
            java.util.List.of(
                new Message("system", systemPromptCollisionFull()),
                new Message("user", spellIntent)));
    System.out.println("collision behavior: " + assistant);
    JsonObject o = parseObject(assistant);
    System.out.println("collision behavior: " + o);
    String name = firstPresentString(o, "collisionBehaviorName", "name", "");
    float radius = getFloat(o, "radius", 0.0f);
    float damage = getFloat(o, "damage", 0.0f);
    boolean shouldDespawn = getBool(o, "shouldDespawn", false);
    String spawnId = firstPresentString(o, "spawnEntityID", "spawnId", "");
    int spawnCount = getInt(o, "spawnCount", 0);
    boolean affectPlayer = getBool(o, "affectPlayer", false);
    String effectId = getString(o, "effectId", "");
    int effectDuration = getInt(o, "effectDuration", 0);
    int effectAmplifier = getInt(o, "effectAmplifier", 0);
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
        affectPlayer);
  }

  /** Step 1b: Generate model + full SpellEntityConfig using a second LLM call (movement/model). */
  public static SpellEntityConfig generateSpellConfigFromLLM(String spellIntent) throws Exception {
    if (spellIntent == null || spellIntent.isEmpty()) {
      throw new IllegalArgumentException("spellIntent cannot be empty");
    }

    // First, build collision behavior via LLM
    CollisionBehaviorConfig cb = generateCollisionBehaviorFromLLM(spellIntent);

    // Then, build movement/model info via a separate LLM call
    String assistant =
        openrouter.chat(
            java.util.List.of(
                new Message("system", systemPromptSpell()), new Message("user", spellIntent)));
    System.out.println("spell config: " + assistant);
    JsonObject o = parseObject(assistant);
    System.out.println("spell config: " + o);

    String prompt = getString(o, "prompt", null);
    if (prompt == null || prompt.isEmpty())
      throw new IllegalArgumentException("LLM missing 'prompt'");
    float microScale = getFloat(o, "microScale", 1.0f);
    String dirStr = getString(o, "movementDirection", "NONE");
    SpellEntityConfig.MovementDirection dir;
    try {
      dir = SpellEntityConfig.MovementDirection.valueOf(dirStr.toUpperCase());
    } catch (Exception e) {
      dir = SpellEntityConfig.MovementDirection.NONE;
    }
    float speed = getFloat(o, "speed", 0.0f);

    Map<BlockPos, BlockState> blocks = Meshy.buildBlocksFromPrompt(prompt);

    return new SpellEntityConfig(blocks, microScale, cb, dir, speed);
  }

  // ---- System prompts ----

  private static String systemPromptSpell() {
    return "Output ONLY valid JSON for a spell model request with keys: "
        + "{prompt:string, microScale:number, movementDirection:one of [FORWARD,BACKWARD,UP,DOWN,NONE], speed:number}."
        + " No prose.";
  }

  private static String systemPromptCollisionFull() {
    return "Output ONLY valid JSON for collision behavior with keys: "
        + "{collisionBehaviorName:string(one of [explode,shockwave,none]), radius:number, damage:number, shouldDespawn:boolean, "
        + "spawnEntityID:string, spawnCount:number, affectPlayer:boolean, effectId:string, effectDuration:number, effectAmplifier:number}. No prose.";
  }

  // ---- JSON helpers ----

  private static JsonObject parseObject(String json) {
    try {
      return JsonParser.parseString(json).getAsJsonObject();
    } catch (Exception e) {
      throw new IllegalArgumentException("LLM did not return JSON: " + json);
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
