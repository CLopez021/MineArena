package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorRequest;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/** Unified one-shot request that includes spell generation fields and full collision behavior. */
public class UnifiedSpellConfigRequest {
  private static final Gson GSON = new Gson();

  @SerializedName("prompt")
  private String prompt;

  @SerializedName("microScale")
  private float microScale;

  @SerializedName("movementDirection")
  private SpellEntityConfig.MovementDirection movementDirection;

  @SerializedName("speed")
  private float movementSpeed;

  @SerializedName("collision")
  private CollisionBehaviorRequest collision;

  public UnifiedSpellConfigRequest() {}

  public String getPrompt() {
    return prompt;
  }

  public float getMicroScale() {
    return microScale;
  }

  public SpellEntityConfig.MovementDirection getMovementDirection() {
    return movementDirection;
  }

  public float getMovementSpeed() {
    return movementSpeed;
  }

  public CollisionBehaviorRequest getCollision() {
    return collision;
  }

  public String toJson() {
    return GSON.toJson(this);
  }

  public static UnifiedSpellConfigRequest fromJson(String json) {
    return GSON.fromJson(json, UnifiedSpellConfigRequest.class);
  }
}
