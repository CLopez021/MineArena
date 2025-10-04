package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.utils.IdResolver;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Utility class for handling spell collision effects and interactions. */
public class SpellCollisionHandler {

  /**
   * Collects all living entities within the specified radius of the spell, optionally excluding the
   * owner.
   */
  public static List<LivingEntity> collectAffectedEntities(
      SpellEntity spell, float radius, boolean affectOwner) {
    Vec3 center = spell.position();
    AABB box =
        new AABB(
            center.x - radius,
            center.y - radius,
            center.z - radius,
            center.x + radius,
            center.y + radius,
            center.z + radius);
    List<LivingEntity> targets = spell.level().getEntitiesOfClass(LivingEntity.class, box);
    targets.removeIf(
        entity ->
            (!affectOwner
                    && spell.getOwnerPlayerId() != null
                    && entity.getUUID().equals(spell.getOwnerPlayerId()))
                || entity.position().distanceTo(center) > radius);
    return targets;
  }

  /** Applies configured status effects to the target entities. */
  public static void applyConfiguredEffectArea(SpellEntity spell, List<LivingEntity> targets) {
    if (spell.level().isClientSide) return;
    var behavior = spell.getConfig().getEffectBehavior();
    String statusEffectId = behavior.getStatusEffectId();
    int durationTicks = Math.max(0, behavior.getStatusDurationTicks());
    if (statusEffectId == null || statusEffectId.isBlank() || durationTicks <= 0) return;

    for (LivingEntity entity : targets) {
      EffectEngine.applyUnifiedEffect(
          (net.minecraft.server.level.ServerLevel) spell.level(),
          entity,
          statusEffectId,
          durationTicks,
          behavior.getStatusAmplifier());
    }
  }

  /** Spawns entities and places blocks on impact based on the spell's configuration. */
  public static void spawnOrPlaceConfiguredOnImpact(SpellEntity spell) {
    if (spell.level().isClientSide) return;
    var behavior = spell.getConfig().getEffectBehavior();
    float radius = Math.max(0.0f, behavior.getRadius());
    var access = spell.level().registryAccess();
    Level level = spell.level();

    // Handle block placement
    String blockId = behavior.getPlaceBlockId();
    int blockCount = Math.max(0, behavior.getPlaceBlockCount());
    if (blockId != null && !blockId.isEmpty() && blockCount > 0) {
      var blockOpt = IdResolver.resolveBlockStrict(access, blockId);
      if (blockOpt.isPresent()) {
        Block block = blockOpt.get();
        BlockState state = block.defaultBlockState();
        for (int i = 0; i < blockCount; i++) {
          BlockPos target = findPlacementSpot(spell, level, radius, 6, state);
          if (target != null) {
            level.setBlock(target, state, 3);
          }
        }
      }
    }

    // Handle entity spawning
    String entityId = behavior.getSpawnEntityId();
    int entityCount = Math.max(0, behavior.getSpawnEntityCount());
    if (entityId != null && !entityId.isEmpty() && entityCount > 0) {
      var entityOpt = IdResolver.resolveEntityTypeStrict(access, entityId);
      if (entityOpt.isPresent()) {
        var entityType = entityOpt.get();
        for (int i = 0; i < entityCount; i++) {
          BlockPos target = findPlacementSpot(spell, level, radius, 6, null);
          if (target != null) {
            Entity spawned = entityType.create(level);
            if (spawned != null) {
              spawned.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
              level.addFreshEntity(spawned);
            }
          }
        }
      }
    }
  }

  /**
   * Find a placement spot by sampling an offset within radius, then scanning upward first, then
   * downward. If a BlockState is provided, also require that it can survive at the target.
   */
  private static BlockPos findPlacementSpot(
      SpellEntity spell,
      Level level,
      float radius,
      int attempts,
      @javax.annotation.Nullable BlockState state) {
    RandomSource random = spell.getRandomSource();
    double angle = random.nextDouble() * Math.PI * 2.0;
    double r = radius * Math.sqrt(random.nextDouble());
    int baseX = (int) Math.floor(spell.getX() + Math.cos(angle) * r);
    int baseZ = (int) Math.floor(spell.getZ() + Math.sin(angle) * r);
    int baseY = (int) Math.floor(spell.getY());
    BlockPos base = new BlockPos(baseX, baseY, baseZ);
    for (int dy = 0; dy < attempts; dy++) {
      BlockPos target = base.above(dy);
      if (level.isEmptyBlock(target) && (state == null || state.canSurvive(level, target))) {
        return target;
      }
    }
    for (int dy = 1; dy <= attempts; dy++) {
      BlockPos target = base.below(dy);
      if (level.isEmptyBlock(target) && (state == null || state.canSurvive(level, target))) {
        return target;
      }
    }
    return null;
  }

  /** Applies knockback to entities with distance-based falloff. */
  public static void applyKnockbackToEntities(
      SpellEntity spell, List<LivingEntity> entities, float knockbackAmount) {
    Vec3 center = spell.position();
    for (LivingEntity entity : entities) {
      double dist = entity.position().distanceTo(center);
      if (dist <= 1e-6) continue; // Avoid division by zero

      float falloff = 1.0f - (float) (dist / spell.getConfig().getEffectBehavior().getRadius());
      float kb = knockbackAmount * Math.max(0f, falloff);

      double dirX = entity.getX() - center.x;
      double dirZ = entity.getZ() - center.z;
      entity.knockback(kb, dirX, dirZ);
      entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.05 * kb, 0.0));
    }
  }

  /** Breaks blocks in a spherical radius with configurable depth/layers. */
  public static void breakBlocksInRadius(SpellEntity spell, float radius, int depth) {
    if (spell.level().isClientSide || radius <= 0.0f || depth <= 0) return;

    Vec3 center = spell.position();

    // Break blocks in layers from surface inward
    for (int layer = 0; layer < depth; layer++) {
      int minX = (int) Math.floor(center.x - radius);
      int maxX = (int) Math.ceil(center.x + radius);
      int minY = (int) Math.floor(center.y - radius);
      int maxY = (int) Math.ceil(center.y + radius);
      int minZ = (int) Math.floor(center.z - radius);
      int maxZ = (int) Math.ceil(center.z + radius);

      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          for (int z = minZ; z <= maxZ; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            double dist = center.distanceTo(Vec3.atCenterOf(pos));
            if (dist <= radius) {
              BlockState state = spell.level().getBlockState(pos);
              if (!state.isAir() && state.getDestroySpeed(spell.level(), pos) >= 0) {
                spell.level().destroyBlock(pos, false);
              }
            }
          }
        }
      }

      // Reduce radius for next layer to create depth effect
      radius = Math.max(0.0f, radius - 1.0f);
      if (radius <= 0.0f) break;
    }
  }

  /**
   * Main effect trigger that applies all configured effects: damage, knockback, block destruction,
   * status effects, and spawning.
   */
  public static void triggerEffect(
      SpellEntity spell, List<LivingEntity> affectedEntities, int ticksSinceLastTrigger) {
    if (!spell.level().isClientSide && ticksSinceLastTrigger >= 20) { // EFFECT_COOLDOWN_TICKS
      var behavior = spell.getConfig().getEffectBehavior();

      // Apply damage to entities
      float damage = Math.max(0f, behavior.getDamage());
      if (damage > 0f) {
        for (LivingEntity entity : affectedEntities) {
          entity.hurt(spell.damageSources().magic(), damage);
        }
      }

      // Apply knockback to entities
      float knockback = Math.max(0f, behavior.getKnockbackAmount());
      if (knockback > 0f) {
        applyKnockbackToEntities(spell, affectedEntities, knockback);
      }

      // Break blocks if configured
      if (behavior.getBlockDestructionRadius() > 0.0f && behavior.getBlockDestructionDepth() > 0) {
        breakBlocksInRadius(
            spell, behavior.getBlockDestructionRadius(), behavior.getBlockDestructionDepth());
      }

      // Apply status effects
      applyConfiguredEffectArea(spell, affectedEntities);

      // Spawn entities/blocks
      spawnOrPlaceConfiguredOnImpact(spell);

      // Despawn if configured
      if (behavior.getDespawnOnTrigger()) {
        spell.discard();
      }
    }
  }
}
