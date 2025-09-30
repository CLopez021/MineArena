package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.BaseConfig;
import com.clopez021.mine_arena.spell.SpellEntity;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Config for selecting and parameterizing a spell effect behavior. It may be triggered on cast or
 * on impact. Stores name (registry key), effect radius, and effect damage.
 */
public class SpellEffectBehaviorConfig extends BaseConfig {
  public enum EffectTrigger {
    ON_CAST,
    ON_IMPACT
  }

  private String effectBehaviorName = OnCollisionBehaviors.DEFAULT_KEY;
  private float effectRadius = 2.0f;
  private float effectDamage = 0.0f;
  private boolean despawnOnTrigger = true;
  private String description =
      OnCollisionBehaviors.definitionFor(OnCollisionBehaviors.DEFAULT_KEY).description();
  private Consumer<SpellEntity> effectHandler =
      OnCollisionBehaviors.definitionFor(OnCollisionBehaviors.DEFAULT_KEY).handler();

  // Unified id for either entity or block
  private String effectSpawnId = "";
  private int effectSpawnCount = 0;
  // Whether explosion/effect should also affect the caster/owner
  private boolean effectAffectPlayer = false;
  // Unified effect id: either a single registry id (e.g., "minecraft:regeneration") or a keyword
  // ("ignite","freeze")
  private String effectId = "";
  // Duration to apply effect in seconds (we convert to ticks internally as needed)
  private int effectDurationSeconds = 0;
  // Strength of the effect (0 = level I, 1 = level II, ...)
  private int effectAmplifier = 1;
  // When to trigger the effect
  private EffectTrigger effectTrigger = EffectTrigger.ON_IMPACT;

  public SpellEffectBehaviorConfig() {}

  public SpellEffectBehaviorConfig(
      String effectBehaviorName,
      float effectRadius,
      float effectDamage,
      boolean despawnOnTrigger,
      String effectSpawnId,
      int effectSpawnCount,
      String effectId,
      int effectDurationSeconds,
      int effectAmplifier,
      boolean effectAffectPlayer,
      EffectTrigger effectTrigger) {
    setEffectBehaviorName(effectBehaviorName);
    this.effectRadius = effectRadius;
    this.effectDamage = effectDamage;
    this.despawnOnTrigger = despawnOnTrigger;
    this.effectSpawnId = effectSpawnId == null ? "" : effectSpawnId;
    this.effectSpawnCount = Math.max(0, effectSpawnCount);
    this.effectId = effectId == null ? "" : effectId;
    this.effectDurationSeconds = Math.max(0, effectDurationSeconds);
    this.effectAmplifier = Math.max(1, effectAmplifier);
    this.effectAffectPlayer = effectAffectPlayer;
    this.effectTrigger = effectTrigger == null ? EffectTrigger.ON_IMPACT : effectTrigger;
  }

  public String getEffectBehaviorName() {
    return effectBehaviorName;
  }

  public float getEffectRadius() {
    return effectRadius;
  }

  public float getEffectDamage() {
    return effectDamage;
  }

  public String getDescription() {
    return description;
  }

  public Consumer<SpellEntity> getEffectHandler() {
    return effectHandler;
  }

  public boolean getEffectAffectPlayer() {
    return effectAffectPlayer;
  }

  public void setEffectAffectPlayer(boolean effectAffectPlayer) {
    this.effectAffectPlayer = effectAffectPlayer;
  }

  public EffectTrigger getEffectTrigger() {
    return effectTrigger;
  }

  public void setEffectTrigger(EffectTrigger effectTrigger) {
    this.effectTrigger = effectTrigger == null ? EffectTrigger.ON_IMPACT : effectTrigger;
  }

  public boolean getDespawnOnTrigger() {
    return despawnOnTrigger;
  }

  public void setDespawnOnTrigger(boolean despawnOnTrigger) {
    this.despawnOnTrigger = despawnOnTrigger;
  }

  public void setEffectBehaviorName(String effectBehaviorName) {
    this.effectBehaviorName =
        (effectBehaviorName == null || effectBehaviorName.isEmpty())
            ? OnCollisionBehaviors.DEFAULT_KEY
            : effectBehaviorName;
    updateDerived();
  }

  public void setEffectRadius(float effectRadius) {
    this.effectRadius = effectRadius;
  }

  public void setEffectDamage(float effectDamage) {
    this.effectDamage = effectDamage;
  }

  // Unified accessors
  public String getEffectSpawnId() {
    return effectSpawnId;
  }

  public void setEffectSpawnId(String effectSpawnId) {
    this.effectSpawnId = effectSpawnId == null ? "" : effectSpawnId;
  }

  public int getEffectSpawnCount() {
    return effectSpawnCount;
  }

  public void setEffectSpawnCount(int effectSpawnCount) {
    this.effectSpawnCount = Math.max(0, effectSpawnCount);
  }

  // Effect config
  public String getEffectId() {
    return effectId;
  }

  public void setEffectId(String effectId) {
    this.effectId = effectId == null ? "" : effectId;
  }

  public int getEffectDurationSeconds() {
    return effectDurationSeconds;
  }

  public int getEffectDurationTicks() {
    return Math.max(0, effectDurationSeconds) * 20;
  }

  public void setEffectDurationSeconds(int effectDurationSeconds) {
    this.effectDurationSeconds = Math.max(0, effectDurationSeconds);
  }

  public int getEffectAmplifier() {
    return effectAmplifier;
  }

  public void setEffectAmplifier(int effectAmplifier) {
    this.effectAmplifier = Math.max(0, effectAmplifier);
  }

  private void updateDerived() {
    var def = OnCollisionBehaviors.definitionFor(this.effectBehaviorName);
    this.description = def.description();
    this.effectHandler = def.handler();
  }

  @Override
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putString("effectBehaviorName", effectBehaviorName);
    tag.putFloat("effectRadius", effectRadius);
    tag.putFloat("effectDamage", effectDamage);
    tag.putBoolean("despawnOnTrigger", despawnOnTrigger);
    tag.putBoolean("effectAffectPlayer", effectAffectPlayer);
    tag.putString("effectTrigger", effectTrigger.name().toLowerCase(Locale.ROOT));
    if (!effectSpawnId.isEmpty()) tag.putString("effectSpawnId", effectSpawnId);
    if (effectSpawnCount > 0) tag.putInt("effectSpawnCount", effectSpawnCount);
    if (!effectId.isEmpty()) tag.putString("effectId", effectId);
    if (effectDurationSeconds > 0) tag.putInt("effectDurationSeconds", effectDurationSeconds);
    if (effectAmplifier > 0) tag.putInt("effectAmplifier", effectAmplifier);
    return tag;
  }

  public static SpellEffectBehaviorConfig fromNBT(CompoundTag tag) {
    SpellEffectBehaviorConfig c = new SpellEffectBehaviorConfig();
    if (tag == null) return c;

    // New keys
    if (tag.contains("effectBehaviorName", Tag.TAG_STRING))
      c.setEffectBehaviorName(tag.getString("effectBehaviorName"));
    else if (tag.contains("collisionBehaviorName", Tag.TAG_STRING)) // backward compat
    c.setEffectBehaviorName(tag.getString("collisionBehaviorName"));
    else if (tag.contains("name", Tag.TAG_STRING)) // older compat
    c.setEffectBehaviorName(tag.getString("name"));
    else c.updateDerived();

    if (tag.contains("effectRadius", Tag.TAG_FLOAT))
      c.setEffectRadius(tag.getFloat("effectRadius"));
    else if (tag.contains("radius", Tag.TAG_FLOAT)) c.setEffectRadius(tag.getFloat("radius"));

    if (tag.contains("effectDamage", Tag.TAG_FLOAT))
      c.setEffectDamage(tag.getFloat("effectDamage"));
    else if (tag.contains("damage", Tag.TAG_FLOAT)) c.setEffectDamage(tag.getFloat("damage"));

    if (tag.contains("despawnOnTrigger", Tag.TAG_BYTE))
      c.setDespawnOnTrigger(tag.getBoolean("despawnOnTrigger"));
    else if (tag.contains("shouldDespawn", Tag.TAG_BYTE))
      c.setDespawnOnTrigger(tag.getBoolean("shouldDespawn"));

    if (tag.contains("effectAffectPlayer", Tag.TAG_BYTE))
      c.setEffectAffectPlayer(tag.getBoolean("effectAffectPlayer"));
    else if (tag.contains("affectPlayer", Tag.TAG_BYTE))
      c.setEffectAffectPlayer(tag.getBoolean("affectPlayer"));

    if (tag.contains("effectTrigger", Tag.TAG_STRING)) {
      String v = tag.getString("effectTrigger");
      c.setEffectTrigger(parseTrigger(v));
    } else if (tag.contains("triggersInstantly", Tag.TAG_BYTE)) {
      // old boolean maps to ON_CAST when true, otherwise ON_IMPACT
      boolean instant = tag.getBoolean("triggersInstantly");
      c.setEffectTrigger(instant ? EffectTrigger.ON_CAST : EffectTrigger.ON_IMPACT);
    }

    if (tag.contains("effectSpawnId", Tag.TAG_STRING))
      c.setEffectSpawnId(tag.getString("effectSpawnId"));
    else if (tag.contains("spawnId", Tag.TAG_STRING)) c.setEffectSpawnId(tag.getString("spawnId"));

    if (tag.contains("effectSpawnCount", Tag.TAG_INT))
      c.setEffectSpawnCount(tag.getInt("effectSpawnCount"));
    else if (tag.contains("spawnCount", Tag.TAG_INT))
      c.setEffectSpawnCount(tag.getInt("spawnCount"));

    if (tag.contains("effectId", Tag.TAG_STRING)) c.setEffectId(tag.getString("effectId"));

    if (tag.contains("effectDurationSeconds", Tag.TAG_INT))
      c.setEffectDurationSeconds(tag.getInt("effectDurationSeconds"));
    else if (tag.contains("effectDuration", Tag.TAG_INT))
      c.setEffectDurationSeconds(Math.max(0, tag.getInt("effectDuration")) / 20);

    if (tag.contains("effectAmplifier", Tag.TAG_INT))
      c.setEffectAmplifier(tag.getInt("effectAmplifier"));

    return c;
  }

  private static EffectTrigger parseTrigger(String v) {
    if (v == null) return EffectTrigger.ON_IMPACT;
    String s = v.trim().toLowerCase(Locale.ROOT);
    return switch (s) {
      case "oncast", "on_cast", "cast" -> EffectTrigger.ON_CAST;
      case "onimpact", "on_impact", "impact" -> EffectTrigger.ON_IMPACT;
      default -> EffectTrigger.ON_IMPACT;
    };
  }
}
