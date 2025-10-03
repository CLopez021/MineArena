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

  // Entity spawning
  private String spawnEntityId = "";
  private int spawnEntityCount = 0;
  // Block placement
  private String placeBlockId = "";
  private int placeBlockCount = 0;
  // Whether explosion/effect should also affect the caster/owner
  private boolean affectOwner = false;
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
  // Block destruction parameters - depth controls how many layers of blocks to break, radius
  // controls the area
  private float blockDestructionRadius = 0.0f;
  private int blockDestructionDepth = 0;

  public SpellEffectBehaviorConfig() {}

  public SpellEffectBehaviorConfig(
      float radius,
      float damage,
      boolean despawnOnTrigger,
      String spawnEntityId,
      int spawnEntityCount,
      String placeBlockId,
      int placeBlockCount,
      String statusEffectId,
      int statusDurationSeconds,
      int statusAmplifier,
      boolean affectOwner,
      EffectTrigger trigger,
      float knockbackAmount,
      float blockDestructionRadius,
      int blockDestructionDepth) {
    this.radius = radius;
    this.damage = damage;
    this.despawnOnTrigger = despawnOnTrigger;
    this.spawnEntityId = spawnEntityId == null ? "" : spawnEntityId;
    this.spawnEntityCount = Math.max(0, spawnEntityCount);
    this.placeBlockId = placeBlockId == null ? "" : placeBlockId;
    this.placeBlockCount = Math.max(0, placeBlockCount);
    this.statusEffectId = statusEffectId == null ? "" : statusEffectId;
    this.statusDurationSeconds = Math.max(0, statusDurationSeconds);
    this.statusAmplifier = Math.max(1, statusAmplifier);
    this.affectOwner = affectOwner;
    this.trigger = trigger == null ? EffectTrigger.ON_IMPACT : trigger;
    this.knockbackAmount = Math.max(0.0f, knockbackAmount);
    this.blockDestructionRadius = Math.max(0.0f, blockDestructionRadius);
    this.blockDestructionDepth = Math.max(0, blockDestructionDepth);
  }

  public float getRadius() {
    return radius;
  }

  public float getDamage() {
    return damage;
  }

  public boolean getAffectOwner() {
    return affectOwner;
  }

  public void setAffectOwner(boolean affectOwner) {
    this.affectOwner = affectOwner;
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

  // Entity spawning accessors
  public String getSpawnEntityId() {
    return spawnEntityId;
  }

  public void setSpawnEntityId(String spawnEntityId) {
    this.spawnEntityId = spawnEntityId == null ? "" : spawnEntityId;
  }

  public int getSpawnEntityCount() {
    return spawnEntityCount;
  }

  public void setSpawnEntityCount(int spawnEntityCount) {
    this.spawnEntityCount = Math.max(0, spawnEntityCount);
  }

  // Block placement accessors
  public String getPlaceBlockId() {
    return placeBlockId;
  }

  public void setPlaceBlockId(String placeBlockId) {
    this.placeBlockId = placeBlockId == null ? "" : placeBlockId;
  }

  public int getPlaceBlockCount() {
    return placeBlockCount;
  }

  public void setPlaceBlockCount(int placeBlockCount) {
    this.placeBlockCount = Math.max(0, placeBlockCount);
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

  public float getBlockDestructionRadius() {
    return blockDestructionRadius;
  }

  public void setBlockDestructionRadius(float blockDestructionRadius) {
    this.blockDestructionRadius = Math.max(0.0f, blockDestructionRadius);
  }

  public int getBlockDestructionDepth() {
    return blockDestructionDepth;
  }

  public void setBlockDestructionDepth(int blockDestructionDepth) {
    this.blockDestructionDepth = Math.max(0, blockDestructionDepth);
  }

  @Override
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putFloat("radius", radius);
    tag.putFloat("damage", damage);
    tag.putBoolean("despawnOnTrigger", despawnOnTrigger);
    tag.putBoolean("affectOwner", affectOwner);
    tag.putString("trigger", trigger.name().toLowerCase(Locale.ROOT));
    if (!spawnEntityId.isEmpty()) tag.putString("spawnEntityId", spawnEntityId);
    if (spawnEntityCount > 0) tag.putInt("spawnEntityCount", spawnEntityCount);
    if (!placeBlockId.isEmpty()) tag.putString("placeBlockId", placeBlockId);
    if (placeBlockCount > 0) tag.putInt("placeBlockCount", placeBlockCount);
    if (!statusEffectId.isEmpty()) tag.putString("statusEffectId", statusEffectId);
    if (statusDurationSeconds > 0) tag.putInt("statusDurationSeconds", statusDurationSeconds);
    if (statusAmplifier > 0) tag.putInt("statusAmplifier", statusAmplifier);
    if (knockbackAmount > 0.0f) tag.putFloat("knockbackAmount", knockbackAmount);
    if (blockDestructionRadius > 0.0f)
      tag.putFloat("blockDestructionRadius", blockDestructionRadius);
    if (blockDestructionDepth > 0) tag.putInt("blockDestructionDepth", blockDestructionDepth);
    return tag;
  }

  public static SpellEffectBehaviorConfig fromNBT(CompoundTag tag) {
    SpellEffectBehaviorConfig c = new SpellEffectBehaviorConfig();
    if (tag == null) return c;

    if (tag.contains("radius", Tag.TAG_FLOAT)) c.setRadius(tag.getFloat("radius"));

    if (tag.contains("damage", Tag.TAG_FLOAT)) c.setDamage(tag.getFloat("damage"));

    if (tag.contains("despawnOnTrigger", Tag.TAG_BYTE))
      c.setDespawnOnTrigger(tag.getBoolean("despawnOnTrigger"));

    if (tag.contains("affectOwner", Tag.TAG_BYTE)) c.setAffectOwner(tag.getBoolean("affectOwner"));

    if (tag.contains("trigger", Tag.TAG_STRING)) {
      String v = tag.getString("trigger");
      c.setTrigger(parseTrigger(v));
    }

    if (tag.contains("spawnEntityId", Tag.TAG_STRING))
      c.setSpawnEntityId(tag.getString("spawnEntityId"));

    if (tag.contains("spawnEntityCount", Tag.TAG_INT))
      c.setSpawnEntityCount(tag.getInt("spawnEntityCount"));

    if (tag.contains("placeBlockId", Tag.TAG_STRING))
      c.setPlaceBlockId(tag.getString("placeBlockId"));

    if (tag.contains("placeBlockCount", Tag.TAG_INT))
      c.setPlaceBlockCount(tag.getInt("placeBlockCount"));

    if (tag.contains("statusEffectId", Tag.TAG_STRING))
      c.setStatusEffectId(tag.getString("statusEffectId"));

    if (tag.contains("statusDurationSeconds", Tag.TAG_INT))
      c.setStatusDurationSeconds(tag.getInt("statusDurationSeconds"));

    if (tag.contains("statusAmplifier", Tag.TAG_INT))
      c.setStatusAmplifier(tag.getInt("statusAmplifier"));

    if (tag.contains("knockbackAmount", Tag.TAG_FLOAT))
      c.setKnockbackAmount(tag.getFloat("knockbackAmount"));

    if (tag.contains("blockDestructionRadius", Tag.TAG_FLOAT))
      c.setBlockDestructionRadius(tag.getFloat("blockDestructionRadius"));

    if (tag.contains("blockDestructionDepth", Tag.TAG_INT))
      c.setBlockDestructionDepth(tag.getInt("blockDestructionDepth"));

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
