package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.BaseConfig;
import com.clopez021.mine_arena.spell.SpellEntity;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Config for selecting and parameterizing a collision behavior. Stores name (registry key), radius,
 * and damage.
 */
public class CollisionBehaviorConfig extends BaseConfig {
  private String name = OnCollisionBehaviors.DEFAULT_KEY;
  private float radius = 2.0f;
  private float damage = 0.0f;
  private boolean shouldDespawn = true;
  private String description =
      OnCollisionBehaviors.definitionFor(OnCollisionBehaviors.DEFAULT_KEY).description();
  private Consumer<SpellEntity> handler =
      OnCollisionBehaviors.definitionFor(OnCollisionBehaviors.DEFAULT_KEY).handler();
  // Unified id for either entity or block
  private String spawnId = "";
  private int spawnCount = 0;
  // Whether explosion effects should also affect the caster/owner
  private boolean affectPlayer = false;
  // Unified effect id: either a single registry id (e.g., "minecraft:regeneration") or a keyword
  // ("ignite","freeze")
  private String effectId = "";
  // Duration to apply effect in ticks (20 ticks = 1 second)
  private int effectDuration = 0;
  // Strength of the effect (0 = level I, 1 = level II, ...)
  private int effectAmplifier = 1;

  public CollisionBehaviorConfig() {}

  public CollisionBehaviorConfig(
      String name,
      float radius,
      float damage,
      boolean shouldDespawn,
      String spawnEntityId,
      int spawnCount,
      String effectId,
      int effectDuration,
      int effectAmplifier,
      boolean affectPlayer) {
    setName(name);
    this.radius = radius;
    this.damage = damage;
    this.shouldDespawn = shouldDespawn;
    this.spawnId = spawnEntityId == null ? "" : spawnEntityId; // unified
    this.spawnCount = Math.max(0, spawnCount); // unified
    this.effectId = effectId == null ? "" : effectId;
    this.effectDuration = Math.max(0, effectDuration);
    this.effectAmplifier = Math.max(1, effectAmplifier);
    this.affectPlayer = affectPlayer;
  }

  public String getName() {
    return name;
  }

  public float getRadius() {
    return radius;
  }

  public float getDamage() {
    return damage;
  }

  public String getDescription() {
    return description;
  }

  public Consumer<SpellEntity> getCollisionHandler() {
    return handler;
  }

  public boolean getAffectPlayer() {
    return affectPlayer;
  }

  public void setAffectPlayer(boolean affectPlayer) {
    this.affectPlayer = affectPlayer;
  }

  public void setName(String name) {
    this.name = (name == null || name.isEmpty()) ? OnCollisionBehaviors.DEFAULT_KEY : name;
    updateDerived();
  }

  public void setRadius(float radius) {
    this.radius = radius;
  }

  public void setDamage(float damage) {
    this.damage = damage;
  }

  public void setShouldDespawn(boolean shouldDespawn) {
    this.shouldDespawn = shouldDespawn;
  }

  public boolean getShouldDespawn() {
    return shouldDespawn;
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

  // Effect config
  public String getEffectId() {
    return effectId;
  }

  public void setEffectId(String effectId) {
    this.effectId = effectId == null ? "" : effectId;
  }

  public int getEffectDuration() {
    return effectDuration;
  }

  public void setEffectDuration(int effectDuration) {
    this.effectDuration = Math.max(0, effectDuration);
  }

  public int getEffectAmplifier() {
    return effectAmplifier;
  }

  public void setEffectAmplifier(int effectAmplifier) {
    this.effectAmplifier = Math.max(0, effectAmplifier);
  }

  private void updateDerived() {
    var def = OnCollisionBehaviors.definitionFor(this.name);
    this.description = def.description();
    this.handler = def.handler();
  }

  @Override
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putString("name", name);
    tag.putFloat("radius", radius);
    tag.putFloat("damage", damage);
    tag.putBoolean("shouldDespawn", shouldDespawn);
    tag.putBoolean("affectPlayer", affectPlayer);
    if (!spawnId.isEmpty()) tag.putString("spawnId", spawnId);
    if (spawnCount > 0) tag.putInt("spawnCount", spawnCount);
    if (!effectId.isEmpty()) tag.putString("effectId", effectId);
    if (effectDuration > 0) tag.putInt("effectDuration", effectDuration);
    if (effectAmplifier > 0) tag.putInt("effectAmplifier", effectAmplifier);
    return tag;
  }

  public static CollisionBehaviorConfig fromNBT(CompoundTag tag) {
    CollisionBehaviorConfig c = new CollisionBehaviorConfig();
    if (tag == null) return c;
    if (tag.contains("name", Tag.TAG_STRING)) c.setName(tag.getString("name"));
    else c.updateDerived();
    if (tag.contains("radius", Tag.TAG_FLOAT)) c.setRadius(tag.getFloat("radius"));
    if (tag.contains("damage", Tag.TAG_FLOAT)) c.setDamage(tag.getFloat("damage"));
    if (tag.contains("shouldDespawn", Tag.TAG_BYTE))
      c.setShouldDespawn(tag.getBoolean("shouldDespawn"));
    if (tag.contains("affectPlayer", Tag.TAG_BYTE))
      c.setAffectPlayer(tag.getBoolean("affectPlayer"));
    if (tag.contains("spawnId", Tag.TAG_STRING)) c.setSpawnId(tag.getString("spawnId"));
    if (tag.contains("spawnCount", Tag.TAG_INT)) c.setSpawnCount(tag.getInt("spawnCount"));
    if (tag.contains("effectId", Tag.TAG_STRING)) c.setEffectId(tag.getString("effectId"));
    if (tag.contains("effectDuration", Tag.TAG_INT))
      c.setEffectDuration(tag.getInt("effectDuration"));
    if (tag.contains("effectAmplifier", Tag.TAG_INT))
      c.setEffectAmplifier(tag.getInt("effectAmplifier"));
    return c;
  }
}
