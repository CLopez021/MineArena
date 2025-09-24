package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/** One-shot DTO containing all parameters for collision behavior. */
public class CollisionBehaviorRequest {
  private static final Gson GSON = new Gson();

  @SerializedName("collisionBehaviorName")
  private String collisionBehaviorName;

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

  @SerializedName("effectId")
  private String effectId;

  @SerializedName("effectDuration")
  private int effectDuration;

  @SerializedName("effectAmplifier")
  private int effectAmplifier;

  @SerializedName("affectPlayer")
  private boolean affectPlayer;

  public CollisionBehaviorRequest() {}

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

  public String getEffectId() {
    return effectId;
  }

  public int getEffectDuration() {
    return effectDuration;
  }

  public int getEffectAmplifier() {
    return effectAmplifier;
  }

  public boolean getAffectPlayer() {
    return affectPlayer;
  }

  public String toJson() {
    return GSON.toJson(this);
  }

  public static CollisionBehaviorRequest fromJson(String json) {
    return GSON.fromJson(json, CollisionBehaviorRequest.class);
  }
}
