package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.SpellEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OnCollisionBehaviors {
  public static final String DEFAULT_KEY = "explode";

  // Behavior tuple: description + function(SpellEntity)
  public record BehaviorDef(String description, Consumer<SpellEntity> handler) {}

  private static final Map<String, BehaviorDef> REGISTRY;

  // Separate static function for readability
  public static void explode(SpellEntity e) {
    if (e == null || e.level() == null || e.level().isClientSide) return;
    Level level = e.level();
    float radius = Math.max(0.1f, e.getConfig().getCollisionBehavior().getRadius());
    float damage = Math.max(0f, e.getConfig().getCollisionBehavior().getDamage());
    boolean affectOwner = e.getConfig().getCollisionBehavior().getAffectPlayer();
    ExplosionHelper.explode(level, e.position(), radius, damage, e.getOwnerPlayerId(), affectOwner);
    if (e.getConfig().getCollisionBehavior().getShouldDespawn()) {
      e.discard();
    }
  }

  // Push entities away without damaging or breaking blocks
  public static void shockwave(SpellEntity e) {
    if (e == null || e.level() == null || e.level().isClientSide) return;
    if (!(e.level() instanceof ServerLevel serverLevel)) return;

    float r = Math.max(0.1f, e.getConfig().getCollisionBehavior().getRadius());
    boolean affectOwner = e.getConfig().getCollisionBehavior().getAffectPlayer();
    Vec3 center = e.position();

    AABB box =
        new AABB(
            center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);
    for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
      if (!affectOwner
          && e.getOwnerPlayerId() != null
          && entity.getUUID().equals(e.getOwnerPlayerId())) continue;
      double dist = entity.position().distanceTo(center);
      if (dist > r || dist <= 1e-6) continue;
      float falloff = 1.0f - (float) (dist / r);
      float kb = Math.max(0.4f, r * 0.25f) * Math.max(0f, falloff);
      double dirX = entity.getX() - center.x;
      double dirZ = entity.getZ() - center.z;
      entity.knockback(kb, dirX, dirZ);
      entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.05 * kb, 0.0));
    }

    serverLevel.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.GENERIC_EXPLODE,
        SoundSource.BLOCKS,
        1.0f,
        1.2f);

    if (e.getConfig().getCollisionBehavior().getShouldDespawn()) {
      e.discard();
    }
  }

  // No-op behavior (do nothing besides optional despawn)
  public static void none(SpellEntity e) {
    if (e == null || e.level() == null || e.level().isClientSide) return;
    if (e.getConfig().getCollisionBehavior().getShouldDespawn()) {
      e.discard();
    }
  }

  static {
    Map<String, BehaviorDef> map = new HashMap<>();

    map.put(
        DEFAULT_KEY,
        new BehaviorDef(
            "Create an explosion and remove the entity.", OnCollisionBehaviors::explode));

    map.put(
        "shockwave",
        new BehaviorDef(
            "Push nearby entities without damage or block destruction.",
            OnCollisionBehaviors::shockwave));

    map.put("none", new BehaviorDef("Perform no collision effect.", OnCollisionBehaviors::none));

    REGISTRY = Collections.unmodifiableMap(map);
  }

  private OnCollisionBehaviors() {}

  public static BehaviorDef definitionFor(String key) {
    if (key == null || key.isEmpty()) key = DEFAULT_KEY;
    return REGISTRY.getOrDefault(key, REGISTRY.get(DEFAULT_KEY));
  }

  public static Map<String, BehaviorDef> registry() {
    return REGISTRY;
  }
}
