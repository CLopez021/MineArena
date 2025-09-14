package com.clopez021.mine_arena.spell.behavior.onCollision;

import com.clopez021.mine_arena.spell.SpellEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;

public final class OnCollisionBehaviors {
  public static final String DEFAULT_KEY = "explode";

  // Behavior tuple: description + function(SpellEntity)
  public record BehaviorDef(String description, Consumer<SpellEntity> handler) {}

  private static final Map<String, BehaviorDef> REGISTRY;

  // Separate static function for readability
  public static void explode(SpellEntity e) {
    if (e == null || e.level() == null || e.level().isClientSide) return;
    Level level = e.level();
    float radius = Math.max(0.1f, e.getConfig().getBehavior().getRadius());
    level.explode(e, e.getX(), e.getY(), e.getZ(), radius, Level.ExplosionInteraction.BLOCK);
    if (e.getConfig().getBehavior().getShouldDespawn()) {
      e.discard();
    }
  }

  static {
    Map<String, BehaviorDef> map = new HashMap<>();

    map.put(
        DEFAULT_KEY,
        new BehaviorDef(
            "Create an explosion and remove the entity.", OnCollisionBehaviors::explode));

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
