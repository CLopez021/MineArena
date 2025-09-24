package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * First-step DTO for describing collision behavior without effects. Effects can be applied in a
 * follow-up step.
 */
public class CollisionBehaviorBasicRequest {
  private static final Gson GSON = new Gson();

  @SerializedName("collisionBehaviorName")
  private String collisionBehaviorName; // maps to CollisionBehaviorConfig.name

  @SerializedName("radius")
  private float radius;

  @SerializedName("damage")
  private float damage;

  @SerializedName("shouldDespawn")
  private boolean shouldDespawn;

  @SerializedName("spawnId")
  private String spawnId;

  @SerializedName("spawnCount")
  private int spawnCount;

  @SerializedName("affectPlayer")
  private boolean affectPlayer;

  public CollisionBehaviorBasicRequest() {}

  public CollisionBehaviorBasicRequest(
      String collisionBehaviorName,
      float radius,
      float damage,
      boolean shouldDespawn,
      String spawnId,
      int spawnCount,
      boolean affectPlayer) {
    this.collisionBehaviorName = collisionBehaviorName;
    this.radius = radius;
    this.damage = damage;
    this.shouldDespawn = shouldDespawn;
    this.spawnId = spawnId;
    this.spawnCount = spawnCount;
    this.affectPlayer = affectPlayer;
  }

  public String getCollisionBehaviorName() {
    return collisionBehaviorName;
  }

  public float getRadius() {
    return radius;
  }

  public float getDamage() {
    return damage;
  }

  public boolean getShouldDespawn() {
    return shouldDespawn;
  }

  public String getSpawnId() {
    return spawnId;
  }

  public int getSpawnCount() {
    return spawnCount;
  }

  public boolean getAffectPlayer() {
    return affectPlayer;
  }

  public String toJson() {
    return GSON.toJson(this);
  }

  public static CollisionBehaviorBasicRequest fromJson(String json) {
    return GSON.fromJson(json, CollisionBehaviorBasicRequest.class);
  }
}
