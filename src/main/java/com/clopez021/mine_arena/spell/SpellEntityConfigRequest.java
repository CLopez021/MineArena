package com.clopez021.mine_arena.spell;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * DTO used by an LLM or external client to describe a spell entity to generate. This is
 * intentionally minimal: it only includes the inputs needed to drive 3D model generation and
 * movement. Collision behavior is provided separately.
 */
public class SpellEntityConfigRequest {
  private static final Gson GSON = new Gson();

  @SerializedName("prompt")
  private String prompt;

  @SerializedName("microScale")
  private float microScale;

  @SerializedName("movementDirection")
  private SpellEntityConfig.MovementDirection movementDirection;

  @SerializedName("speed")
  private float movementSpeed;

  public SpellEntityConfigRequest() {}

  public SpellEntityConfigRequest(
      String prompt,
      float microScale,
      SpellEntityConfig.MovementDirection movementDirection,
      float movementSpeed) {
    this.prompt = prompt;
    this.microScale = microScale;
    this.movementDirection = movementDirection;
    this.movementSpeed = movementSpeed;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public float getMicroScale() {
    return microScale;
  }

  public void setMicroScale(float microScale) {
    this.microScale = microScale;
  }

  public SpellEntityConfig.MovementDirection getMovementDirection() {
    return movementDirection;
  }

  public void setMovementDirection(SpellEntityConfig.MovementDirection movementDirection) {
    this.movementDirection = movementDirection;
  }

  public float getMovementSpeed() {
    return movementSpeed;
  }

  public void setMovementSpeed(float movementSpeed) {
    this.movementSpeed = movementSpeed;
  }

  public String toJson() {
    return GSON.toJson(this);
  }

  public static SpellEntityConfigRequest fromJson(String json) {
    return GSON.fromJson(json, SpellEntityConfigRequest.class);
  }
}
