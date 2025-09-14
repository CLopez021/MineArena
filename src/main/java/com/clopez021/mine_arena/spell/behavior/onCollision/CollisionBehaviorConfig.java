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

  public CollisionBehaviorConfig() {}

  public CollisionBehaviorConfig(
      String name,
      float radius,
      float damage,
      boolean shouldDespawn,
      String spawnEntityId,
      int spawnCount) {
    setName(name);
    this.radius = radius;
    this.damage = damage;
    this.shouldDespawn = shouldDespawn;
    this.spawnId = spawnEntityId == null ? "" : spawnEntityId; // unified
    this.spawnCount = Math.max(0, spawnCount); // unified
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

  public Consumer<SpellEntity> getHandler() {
    return handler;
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
    if (!spawnId.isEmpty()) tag.putString("spawnId", spawnId);
    if (spawnCount > 0) tag.putInt("spawnCount", spawnCount);
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
    if (tag.contains("spawnId", Tag.TAG_STRING)) c.setSpawnId(tag.getString("spawnId"));
    if (tag.contains("spawnCount", Tag.TAG_INT)) c.setSpawnCount(tag.getInt("spawnCount"));
    return c;
  }
}
