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
    String effectId = behavior.getEffectId();
    int durationTicks = Math.max(0, behavior.getEffectDurationTicks());
    if (effectId == null || effectId.isBlank() || durationTicks <= 0) return;

    for (LivingEntity entity : targets) {
      EffectEngine.applyUnifiedEffect(
          (net.minecraft.server.level.ServerLevel) this.level(),
          entity,
          effectId,
          durationTicks,
          behavior.getEffectAmplifier());
    }
  }

  private void spawnOrPlaceConfiguredOnImpact() {
    if (this.level().isClientSide) return;
    var behavior = this.config.getEffectBehavior();
    String id = behavior.getEffectSpawnId();
    int count = Math.max(0, behavior.getEffectSpawnCount());
    float radius = Math.max(0.0f, behavior.getEffectRadius());
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
      if (this.config.getEffectBehavior().getEffectTrigger()
          == SpellEffectBehaviorConfig.EffectTrigger.ON_CAST) {
        var behavior = this.config.getEffectBehavior();
        float radius = Math.max(0.1f, behavior.getEffectRadius());
        boolean affectOwner = behavior.getEffectAffectPlayer();
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
        float radius = Math.max(0.1f, behavior.getEffectRadius());
        boolean affectOwner = behavior.getEffectAffectPlayer();
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
    if (!level().isClientSide
        && this.config.getEffectBehavior().getEffectHandler() != null
        && ticksSinceLastTrigger >= COLLISION_COOLDOWN_TICKS) {
      this.config.getEffectBehavior().getEffectHandler().accept(this);
      applyConfiguredEffectArea(affectedEntities);
      spawnOrPlaceConfiguredOnImpact();
      ticksSinceLastTrigger = 0;
    }
  }
}
