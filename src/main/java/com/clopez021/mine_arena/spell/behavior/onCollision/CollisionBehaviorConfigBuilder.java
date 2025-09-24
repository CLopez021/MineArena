package com.clopez021.mine_arena.spell.behavior.onCollision;

/** Builder helpers for {@link CollisionBehaviorConfig}. */
public final class CollisionBehaviorConfigBuilder {
  private CollisionBehaviorConfigBuilder() {}

  /** Build a {@link CollisionBehaviorConfig} from the basic request without effects. */
  public static CollisionBehaviorConfig fromBasic(CollisionBehaviorBasicRequest req) {
    if (req == null) throw new IllegalArgumentException("request cannot be null");
    return new CollisionBehaviorConfig(
        req.getCollisionBehaviorName(),
        req.getRadius(),
        req.getDamage(),
        req.getShouldDespawn(),
        req.getSpawnId(),
        req.getSpawnCount(),
        null, // effectId
        0, // effectDuration
        1, // effectAmplifier
        req.getAffectPlayer());
  }

  /** Apply effects in a separate step. */
  public static void applyEffects(
      CollisionBehaviorConfig cfg, String effectId, int effectDuration, int effectAmplifier) {
    if (cfg == null) throw new IllegalArgumentException("config cannot be null");
    cfg.setEffectId(effectId);
    cfg.setEffectDuration(effectDuration);
    cfg.setEffectAmplifier(effectAmplifier);
  }

  /** Build a {@link CollisionBehaviorConfig} from the one-shot full request. */
  public static CollisionBehaviorConfig fromFull(CollisionBehaviorRequest req) {
    if (req == null) throw new IllegalArgumentException("request cannot be null");
    return new CollisionBehaviorConfig(
        req.getCollisionBehaviorName(),
        req.getRadius(),
        req.getDamage(),
        req.getShouldDespawn(),
        req.getSpawnId(),
        req.getSpawnCount(),
        req.getEffectId(),
        req.getEffectDuration(),
        req.getEffectAmplifier(),
        req.getAffectPlayer());
  }
}
