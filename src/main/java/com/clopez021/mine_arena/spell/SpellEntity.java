package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.spell.behavior.onCollision.SpellEffectBehaviorConfig;
import com.clopez021.mine_arena.utils.IdResolver;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SpellEntity extends Entity {
  // ---- Synced key (entire config) ----
  private static final EntityDataAccessor<CompoundTag> DATA_CONFIG =
      SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);

  // Local caches (handy for math/render)
  public final Vector3f minCorner = new Vector3f();

  // Authoritative config (single source of truth)
  private SpellEntityConfig config = SpellEntityConfig.empty();

  // Behavior description/handler live in config.getEffectBehavior()
  private boolean collisionTriggered = false;
  private boolean isColliding = false;
  private boolean entityCollisionDetected = false;
  // Cooldown management to avoid excessive triggering
  private static final int COLLISION_COOLDOWN_TICKS = 20; // 1 second at 20 TPS
  private int ticksSinceLastTrigger = COLLISION_COOLDOWN_TICKS;

  // Dynamic dimensions
  private float spanX = 0.5f, spanY = 0.5f, spanZ = 0.5f;
  public float centerLocalX, centerLocalZ;

  public SpellEntity(EntityType<? extends Entity> type, Level level) {
    super(type, level);
    this.noPhysics = false;
    // Spells are actively steered; disable gravity to avoid desync and sudden drops
    this.setNoGravity(true);
  }

  private UUID ownerPlayerId;

  public void setOwnerPlayerId(UUID id) {
    this.ownerPlayerId = id;
  }

  public UUID getOwnerPlayerId() {
    return this.ownerPlayerId;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    b.define(DATA_CONFIG, new CompoundTag());
  }

  // ---------------- Internal apply helpers (no side checks) ----------------

  private void pushConfigToSyncedData() {
    this.entityData.set(DATA_CONFIG, this.config.toNBT());
  }

  // Apply derived runtime state from current config and refresh size
  private void applyDerivedFromConfig() {
    recalcBoundsFromBlocks();
    refreshDimensions();
  }

  // Expose config-backed runtime state without duplicating storage
  public SpellEntityConfig getConfig() {
    return this.config;
  }

  public Map<BlockPos, BlockState> getBlocks() {
    return this.config.getBlocks();
  }

  public float getMicroScale() {
    return this.config.getMicroScale();
  }

  // ---------------- Server-side setters ----------------

  // Removed granular setters; prefer applying a full config via applyConfigServer

  /** Pack your map -> NBT and set once. Auto-broadcasts to all trackers. */
  // Removed granular setters; prefer applying a full config via applyConfigServer

  // --------------- Client-side: react to updates ---------------

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);

    if (key == DATA_CONFIG) {
      CompoundTag cfgTag = this.entityData.get(DATA_CONFIG);
      this.config = SpellEntityConfig.fromNBT(cfgTag);
      applyDerivedFromConfig();
    }
  }

  // ----------------- Persistence (server only) -----------------
  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    // Delegate to SpellEntityConfig for consistent NBT structure
    tag.merge(this.config.toNBT());
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (level().isClientSide) return; // client doesn't load saves
    this.config = SpellEntityConfig.fromNBT(tag);
    pushConfigToSyncedData();
    applyDerivedFromConfig();
  }

  // ------------ Dynamic dimensions (canonical approach) ------------

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    // Minecraft's AABB is square in XZ; pick the larger span
    float width = Math.max(spanX, spanZ);
    float height = Math.max(0.1f, spanY);
    return EntityDimensions.fixed(Math.max(0.1f, width), height);
  }

  private void recalcBoundsFromBlocks() {
    Map<BlockPos, BlockState> blocks = this.config.getBlocks();
    float microScale = this.config.getMicroScale();
    if (blocks.isEmpty()) {
      spanX = spanY = spanZ = 0.5f;
      centerLocalX = centerLocalZ = 0f;
      return;
    }

    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    for (BlockPos p : blocks.keySet()) {
      minX = Math.min(minX, p.getX());
      minY = Math.min(minY, p.getY());
      minZ = Math.min(minZ, p.getZ());
      maxX = Math.max(maxX, p.getX() + 1);
      maxY = Math.max(maxY, p.getY() + 1);
      maxZ = Math.max(maxZ, p.getZ() + 1);
    }

    spanX = (maxX - minX) * microScale;
    spanY = (maxY - minY) * microScale;
    spanZ = (maxZ - minZ) * microScale;

    // local center for rendering alignment
    centerLocalX = ((minX + maxX) * 0.5f) * microScale;
    centerLocalZ = ((minZ + maxZ) * 0.5f) * microScale;

    // keep minCorner cache synced
    this.minCorner.set(minX, minY, minZ);
  }

  // Public: server-only configure
  public void applyConfigServer(SpellEntityConfig config) {
    if (level().isClientSide) return; // client doesn't load saves
    this.config = config != null ? config : SpellEntityConfig.empty();
    pushConfigToSyncedData();
    applyDerivedFromConfig();
  }

  private java.util.List<LivingEntity> collectAffectedEntities(float radius, boolean affectOwner) {
    Vec3 center = this.position();
    AABB box =
        new AABB(
            center.x - radius,
            center.y - radius,
            center.z - radius,
            center.x + radius,
            center.y + radius,
            center.z + radius);
    List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box);
    targets.removeIf(
        entity ->
            (!affectOwner
                    && this.ownerPlayerId != null
                    && entity.getUUID().equals(this.ownerPlayerId))
                || entity.position().distanceTo(center) > radius);
    return targets;
  }

  private void applyConfiguredEffectArea(java.util.List<LivingEntity> targets) {
    if (this.level().isClientSide) return;
    var behavior = this.config.getEffectBehavior();
    String statusEffectId = behavior.getStatusEffectId();
    int durationTicks = Math.max(0, behavior.getStatusDurationTicks());
    if (statusEffectId == null || statusEffectId.isBlank() || durationTicks <= 0) return;

    for (LivingEntity entity : targets) {
      EffectEngine.applyUnifiedEffect(
          (net.minecraft.server.level.ServerLevel) this.level(),
          entity,
          statusEffectId,
          durationTicks,
          behavior.getStatusAmplifier());
    }
  }

  private void spawnOrPlaceConfiguredOnImpact() {
    if (this.level().isClientSide) return;
    var behavior = this.config.getEffectBehavior();
    String id = behavior.getSpawnId();
    int count = Math.max(0, behavior.getSpawnCount());
    float radius = Math.max(0.0f, behavior.getRadius());
    if (id == null || id.isEmpty() || count <= 0) return;

    var access = this.level().registryAccess();
    // 1) Single id: block
    var blockOpt = IdResolver.resolveBlockStrict(access, id);
    if (blockOpt.isPresent()) {
      System.out.println("blockOpt: " + blockOpt.get());
      Block block = blockOpt.get();
      BlockState state = block.defaultBlockState();
      Level level = this.level();
      for (int i = 0; i < count; i++) {
        BlockPos target = findPlacementSpot(level, radius, 6, state);
        if (target != null) {
          level.setBlock(target, state, 3);
        }
      }
      return;
    }

    // 2) Single id: entity type
    var entityOpt = IdResolver.resolveEntityTypeStrict(access, id);
    if (entityOpt.isPresent()) {
      EntityType<?> entityType = entityOpt.get();
      Level level = this.level();
      for (int i = 0; i < count; i++) {
        BlockPos target = findPlacementSpot(level, radius, 6, null);
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

  /**
   * Find a placement spot by sampling an offset within radius, then scanning upward first, then
   * downward. If a BlockState is provided, also require that it can survive at the target.
   */
  private BlockPos findPlacementSpot(
      Level level, float radius, int attempts, @javax.annotation.Nullable BlockState state) {
    double angle = this.random.nextDouble() * Math.PI * 2.0;
    double r = radius * Math.sqrt(this.random.nextDouble());
    int baseX = (int) Math.floor(this.getX() + Math.cos(angle) * r);
    int baseZ = (int) Math.floor(this.getZ() + Math.sin(angle) * r);
    int baseY = (int) Math.floor(this.getY());
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

  private void applyKnockbackToEntities(
      java.util.List<LivingEntity> entities, float knockbackAmount) {
    Vec3 center = this.position();
    for (LivingEntity entity : entities) {
      double dist = entity.position().distanceTo(center);
      if (dist <= 1e-6) continue; // Avoid division by zero

      float falloff = 1.0f - (float) (dist / this.config.getEffectBehavior().getRadius());
      float kb = knockbackAmount * Math.max(0f, falloff);

      double dirX = entity.getX() - center.x;
      double dirZ = entity.getZ() - center.z;
      entity.knockback(kb, dirX, dirZ);
      entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.05 * kb, 0.0));
    }
  }

  private void breakBlocksInRadius(float radius, int depth) {
    if (this.level().isClientSide || radius <= 0.0f || depth <= 0) return;

    Vec3 center = this.position();

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
              BlockState state = this.level().getBlockState(pos);
              if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
                this.level().destroyBlock(pos, true);
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

  @Override
  public void tick() {
    super.tick();

    if (!level().isClientSide) {
      // Increment cooldown timer
      if (ticksSinceLastTrigger < COLLISION_COOLDOWN_TICKS) ticksSinceLastTrigger++;

      // Compute desired motion each tick from config-provided direction (per-player)
      float speed = Math.max(0f, this.config.getMovementSpeed());
      Vec3 v = this.config.getDirection(ownerPlayerId);
      boolean shouldMove = this.config.getShouldMove();
      Vec3 motion =
          (shouldMove && speed > 0f && v.lengthSqr() > 1e-6)
              ? v.normalize().scale(speed)
              : Vec3.ZERO;

      this.setDeltaMovement(motion);
      if (!motion.equals(Vec3.ZERO)) this.hasImpulse = true;

      // Move using vanilla pipeline for proper networking/interpolation
      this.move(MoverType.SELF, this.getDeltaMovement());

      // Trigger on-cast behavior (no collision required)
      if (this.config.getEffectBehavior().getTrigger()
          == SpellEffectBehaviorConfig.EffectTrigger.ON_CAST) {
        SpellEffectBehaviorConfig behavior = this.config.getEffectBehavior();
        float radius = Math.max(0.1f, behavior.getRadius());
        boolean affectOwner = behavior.getAffectPlayer();
        java.util.List<LivingEntity> affectedEntities =
            collectAffectedEntities(radius, affectOwner);
        triggerCollision(affectedEntities);
      }

      // Enhanced collision detection: blocks OR entities
      boolean blockCollision =
          this.onGround() || this.horizontalCollision || this.verticalCollision;
      boolean entityCollision = this.entityCollisionDetected;

      // Additional entity collision check using AABB overlap
      if (!entityCollision && !blockCollision) {
        List<Entity> nearbyEntities =
            this.level()
                .getEntities(
                    this,
                    this.getBoundingBox(),
                    entity ->
                        entity != this
                            && (this.ownerPlayerId == null
                                || !entity.getUUID().equals(this.ownerPlayerId)));
        entityCollision = !nearbyEntities.isEmpty();
      }

      boolean collidingNow = blockCollision || entityCollision;

      if (collidingNow) {
        // Pre-compute affected entities once for this tick
        var behavior = this.config.getEffectBehavior();
        float radius = Math.max(0.1f, behavior.getRadius());
        boolean affectOwner = behavior.getAffectPlayer();
        java.util.List<LivingEntity> affectedEntities =
            collectAffectedEntities(radius, affectOwner);

        triggerCollision(affectedEntities);
        isColliding = true;
      } else if (isColliding) {
        // Collision ended this tick
        isColliding = false;
      }

      // Reset entity collision flag for next tick
      this.entityCollisionDetected = false;
    }
  }

  /** Initialize from SpellEntityConfig (server-side only). */
  public void initializeServer(SpellEntityConfig data) {
    if (level().isClientSide) {
      return;
    }
    applyConfigServer(data);
  }

  public void triggerCollision(java.util.List<LivingEntity> affectedEntities) {
    if (!level().isClientSide && ticksSinceLastTrigger >= COLLISION_COOLDOWN_TICKS) {
      var behavior = this.config.getEffectBehavior();

      // Apply damage to entities
      float damage = Math.max(0f, behavior.getDamage());
      if (damage > 0f) {
        for (LivingEntity entity : affectedEntities) {
          entity.hurt(this.damageSources().magic(), damage);
        }
      }

      // Apply knockback to entities
      float knockback = Math.max(0f, behavior.getKnockbackAmount());
      if (knockback > 0f) {
        applyKnockbackToEntities(affectedEntities, knockback);
      }

      // Break blocks if configured
      if (behavior.getBlockDestructionRadius() > 0.0f && behavior.getBlockDestructionDepth() > 0) {
        breakBlocksInRadius(
            behavior.getBlockDestructionRadius(), behavior.getBlockDestructionDepth());
      }

      // Apply status effects
      applyConfiguredEffectArea(affectedEntities);

      // Spawn entities/blocks
      spawnOrPlaceConfiguredOnImpact();

      // Despawn if configured
      if (behavior.getDespawnOnTrigger()) {
        this.discard();
      }

      ticksSinceLastTrigger = 0;
    }
  }
}
