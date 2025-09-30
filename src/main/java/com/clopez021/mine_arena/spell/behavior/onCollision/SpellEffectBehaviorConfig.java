package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.BaseConfig;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Config for spell effect behavior. It may be triggered on cast or on impact. Stores effect radius,
 * damage, knockback, and other parameters.
 */
public class SpellEffectBehaviorConfig extends BaseConfig {
  public enum EffectTrigger {
    ON_CAST,
    ON_IMPACT
  }

  private float radius = 2.0f;
  private float damage = 0.0f;
  private boolean despawnOnTrigger = true;

  // Unified id for either entity or block
  private String spawnId = "";
  private int spawnCount = 0;
  // Whether explosion/effect should also affect the caster/owner
  private boolean affectPlayer = false;
  // Unified status effect id: either a single registry id (e.g., "minecraft:regeneration") or a
  // keyword
  // ("ignite","freeze")
  private String statusEffectId = "";
  // Duration to apply status effect in seconds (we convert to ticks internally as needed)
  private int statusDurationSeconds = 0;
  // Strength of the status effect (0 = level I, 1 = level II, ...)
  private int statusAmplifier = 1;
  // When to trigger the effect
  private EffectTrigger trigger = EffectTrigger.ON_IMPACT;
  // Knockback amount to apply to entities (0 = no knockback)
  private float knockbackAmount = 0.0f;
  // Whether to break blocks within the effect radius
  private boolean breakBlocks = false;

  public SpellEffectBehaviorConfig() {}

  public SpellEffectBehaviorConfig(
      float radius,
      float damage,
      boolean despawnOnTrigger,
      String spawnId,
      int spawnCount,
      String statusEffectId,
      int statusDurationSeconds,
      int statusAmplifier,
      boolean affectPlayer,
      EffectTrigger trigger,
      float knockbackAmount,
      boolean breakBlocks) {
    this.radius = radius;
    this.damage = damage;
    this.despawnOnTrigger = despawnOnTrigger;
    this.spawnId = spawnId == null ? "" : spawnId;
    this.spawnCount = Math.max(0, spawnCount);
    this.statusEffectId = statusEffectId == null ? "" : statusEffectId;
    this.statusDurationSeconds = Math.max(0, statusDurationSeconds);
    this.statusAmplifier = Math.max(1, statusAmplifier);
    this.affectPlayer = affectPlayer;
    this.trigger = trigger == null ? EffectTrigger.ON_IMPACT : trigger;
    this.knockbackAmount = Math.max(0.0f, knockbackAmount);
    this.breakBlocks = breakBlocks;
  }

  public float getRadius() {
    return radius;
  }

  public float getDamage() {
    return damage;
  }

  public boolean getAffectPlayer() {
    return affectPlayer;
  }

  public void setAffectPlayer(boolean affectPlayer) {
    this.affectPlayer = affectPlayer;
  }

  public EffectTrigger getTrigger() {
    return trigger;
  }

  public void setTrigger(EffectTrigger trigger) {
    this.trigger = trigger == null ? EffectTrigger.ON_IMPACT : trigger;
  }

  public boolean getDespawnOnTrigger() {
    return despawnOnTrigger;
  }

  public void setDespawnOnTrigger(boolean despawnOnTrigger) {
    this.despawnOnTrigger = despawnOnTrigger;
  }

  public void setRadius(float radius) {
    this.radius = radius;
  }

  public void setDamage(float damage) {
    this.damage = damage;
  }

  // Unified accessors
  public String getSpawnId() {
    return spawnId;
  }

  public void setSpawnId(String spawnId) {
    this.spawnId = spawnId == null ? "" : spawnId;
  }

  public int getSpawnCount() {
    return spawnCount;
  }

  public void setSpawnCount(int spawnCount) {
    this.spawnCount = Math.max(0, spawnCount);
  }

  // Status effect config
  public String getStatusEffectId() {
    return statusEffectId;
  }

  public void setStatusEffectId(String statusEffectId) {
    this.statusEffectId = statusEffectId == null ? "" : statusEffectId;
  }

  public int getStatusDurationSeconds() {
    return statusDurationSeconds;
  }

  public int getStatusDurationTicks() {
    return Math.max(0, statusDurationSeconds) * 20;
  }

  public void setStatusDurationSeconds(int statusDurationSeconds) {
    this.statusDurationSeconds = Math.max(0, statusDurationSeconds);
  }

  public int getStatusAmplifier() {
    return statusAmplifier;
  }

  public void setStatusAmplifier(int statusAmplifier) {
    this.statusAmplifier = Math.max(0, statusAmplifier);
  }

  public float getKnockbackAmount() {
    return knockbackAmount;
  }

  public void setKnockbackAmount(float knockbackAmount) {
    this.knockbackAmount = Math.max(0.0f, knockbackAmount);
  }

  public boolean getBreakBlocks() {
    return breakBlocks;
  }

  public void setBreakBlocks(boolean breakBlocks) {
    this.breakBlocks = breakBlocks;
  }

  @Override
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putFloat("radius", radius);
    tag.putFloat("damage", damage);
    tag.putBoolean("despawnOnTrigger", despawnOnTrigger);
    tag.putBoolean("affectPlayer", affectPlayer);
    tag.putString("trigger", trigger.name().toLowerCase(Locale.ROOT));
    if (!spawnId.isEmpty()) tag.putString("spawnId", spawnId);
    if (spawnCount > 0) tag.putInt("spawnCount", spawnCount);
    if (!statusEffectId.isEmpty()) tag.putString("statusEffectId", statusEffectId);
    if (statusDurationSeconds > 0) tag.putInt("statusDurationSeconds", statusDurationSeconds);
    if (statusAmplifier > 0) tag.putInt("statusAmplifier", statusAmplifier);
    if (knockbackAmount > 0.0f) tag.putFloat("knockbackAmount", knockbackAmount);
    tag.putBoolean("breakBlocks", breakBlocks);
    return tag;
  }

  public static SpellEffectBehaviorConfig fromNBT(CompoundTag tag) {
    SpellEffectBehaviorConfig c = new SpellEffectBehaviorConfig();
    if (tag == null) return c;

    if (tag.contains("radius", Tag.TAG_FLOAT)) c.setRadius(tag.getFloat("radius"));

    if (tag.contains("damage", Tag.TAG_FLOAT)) c.setDamage(tag.getFloat("damage"));

    if (tag.contains("despawnOnTrigger", Tag.TAG_BYTE))
      c.setDespawnOnTrigger(tag.getBoolean("despawnOnTrigger"));

    if (tag.contains("affectPlayer", Tag.TAG_BYTE))
      c.setAffectPlayer(tag.getBoolean("affectPlayer"));

    if (tag.contains("trigger", Tag.TAG_STRING)) {
      String v = tag.getString("trigger");
      c.setTrigger(parseTrigger(v));
    }

    if (tag.contains("spawnId", Tag.TAG_STRING)) c.setSpawnId(tag.getString("spawnId"));

    if (tag.contains("spawnCount", Tag.TAG_INT)) c.setSpawnCount(tag.getInt("spawnCount"));

    if (tag.contains("statusEffectId", Tag.TAG_STRING))
      c.setStatusEffectId(tag.getString("statusEffectId"));

    if (tag.contains("statusDurationSeconds", Tag.TAG_INT))
      c.setStatusDurationSeconds(tag.getInt("statusDurationSeconds"));

    if (tag.contains("statusAmplifier", Tag.TAG_INT))
      c.setStatusAmplifier(tag.getInt("statusAmplifier"));

    if (tag.contains("knockbackAmount", Tag.TAG_FLOAT))
      c.setKnockbackAmount(tag.getFloat("knockbackAmount"));

    if (tag.contains("breakBlocks", Tag.TAG_BYTE)) c.setBreakBlocks(tag.getBoolean("breakBlocks"));

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
