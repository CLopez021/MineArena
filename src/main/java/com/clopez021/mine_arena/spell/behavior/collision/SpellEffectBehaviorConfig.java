package com.clopez021.mine_arena.spell.behavior.collision;

import com.clopez021.mine_arena.spell.config.BaseConfig;
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

  // Getters only - immutable config
  public float getRadius() {
    return radius;
  }

  public float getDamage() {
    return damage;
  }

  public boolean getAffectOwner() {
    return affectOwner;
  }

  public EffectTrigger getTrigger() {
    return trigger;
  }

  public boolean getDespawnOnTrigger() {
    return despawnOnTrigger;
  }

  public String getSpawnEntityId() {
    return spawnEntityId;
  }

  public int getSpawnEntityCount() {
    return spawnEntityCount;
  }

  public String getPlaceBlockId() {
    return placeBlockId;
  }

  public int getPlaceBlockCount() {
    return placeBlockCount;
  }

  public String getStatusEffectId() {
    return statusEffectId;
  }

  public int getStatusDurationSeconds() {
    return statusDurationSeconds;
  }

  public int getStatusDurationTicks() {
    return Math.max(0, statusDurationSeconds) * 20;
  }

  public int getStatusAmplifier() {
    return statusAmplifier;
  }

  public float getKnockbackAmount() {
    return knockbackAmount;
  }

  public float getBlockDestructionRadius() {
    return blockDestructionRadius;
  }

  public int getBlockDestructionDepth() {
    return blockDestructionDepth;
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
    if (tag == null) return new SpellEffectBehaviorConfig();

    float radius = tag.contains("radius", Tag.TAG_FLOAT) ? tag.getFloat("radius") : 2.0f;
    float damage = tag.contains("damage", Tag.TAG_FLOAT) ? tag.getFloat("damage") : 0.0f;
    boolean despawnOnTrigger =
        tag.contains("despawnOnTrigger", Tag.TAG_BYTE) ? tag.getBoolean("despawnOnTrigger") : true;
    String spawnEntityId =
        tag.contains("spawnEntityId", Tag.TAG_STRING) ? tag.getString("spawnEntityId") : "";
    int spawnEntityCount =
        tag.contains("spawnEntityCount", Tag.TAG_INT) ? tag.getInt("spawnEntityCount") : 0;
    String placeBlockId =
        tag.contains("placeBlockId", Tag.TAG_STRING) ? tag.getString("placeBlockId") : "";
    int placeBlockCount =
        tag.contains("placeBlockCount", Tag.TAG_INT) ? tag.getInt("placeBlockCount") : 0;
    String statusEffectId =
        tag.contains("statusEffectId", Tag.TAG_STRING) ? tag.getString("statusEffectId") : "";
    int statusDurationSeconds =
        tag.contains("statusDurationSeconds", Tag.TAG_INT)
            ? tag.getInt("statusDurationSeconds")
            : 0;
    int statusAmplifier =
        tag.contains("statusAmplifier", Tag.TAG_INT) ? tag.getInt("statusAmplifier") : 1;
    boolean affectOwner =
        tag.contains("affectOwner", Tag.TAG_BYTE) ? tag.getBoolean("affectOwner") : false;
    EffectTrigger trigger =
        tag.contains("trigger", Tag.TAG_STRING)
            ? parseTrigger(tag.getString("trigger"))
            : EffectTrigger.ON_IMPACT;
    float knockbackAmount =
        tag.contains("knockbackAmount", Tag.TAG_FLOAT) ? tag.getFloat("knockbackAmount") : 0.0f;
    float blockDestructionRadius =
        tag.contains("blockDestructionRadius", Tag.TAG_FLOAT)
            ? tag.getFloat("blockDestructionRadius")
            : 0.0f;
    int blockDestructionDepth =
        tag.contains("blockDestructionDepth", Tag.TAG_INT)
            ? tag.getInt("blockDestructionDepth")
            : 0;

    // Create config with constructor
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
